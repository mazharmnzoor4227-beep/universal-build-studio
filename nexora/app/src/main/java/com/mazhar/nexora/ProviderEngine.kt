package com.mazhar.nexora

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ProviderEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        profile: ProviderProfile,
        messages: List<ChatLine>,
        system: String,
        puterActivity: Activity? = null,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        when (profile.protocol) {
            ApiProtocol.OPENAI_CHAT -> openAi(profile, messages, system, onDelta)
            ApiProtocol.GEMINI -> gemini(profile, messages, system, onDelta)
            ApiProtocol.ANTHROPIC -> anthropic(profile, messages, system, onDelta)
            ApiProtocol.PUTER_WEB -> {
                val activity = puterActivity ?: throw IOException("Puter connector needs an active app screen")
                PuterBridge(activity).generate(messages.lastOrNull()?.content.orEmpty(), onDelta)
            }
        }
    }

    private fun openAi(
        profile: ProviderProfile,
        messages: List<ChatLine>,
        system: String,
        onDelta: (String) -> Unit
    ): String {
        val endpoint = openAiEndpoint(profile.baseUrl)
        val payloadMessages = JSONArray()
        payloadMessages.put(JSONObject().put("role", "system").put("content", system))
        messages.forEach { line ->
            payloadMessages.put(JSONObject().put("role", line.role).put("content", line.content))
        }
        val body = JSONObject()
            .put("model", profile.model)
            .put("messages", payloadMessages)
            .put("temperature", 0.35)
            .put("stream", true)
            .toString()
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${profile.apiKey}")
            .header("Accept", "text/event-stream")
            .applyCustomHeaders(profile.customHeaders)
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw providerError(response.code, raw)
            return parseStreamingBody(raw, ApiProtocol.OPENAI_CHAT, onDelta)
        }
    }

    private fun gemini(
        profile: ProviderProfile,
        messages: List<ChatLine>,
        system: String,
        onDelta: (String) -> Unit
    ): String {
        require(profile.apiKey.isNotBlank()) { "Gemini API key is empty" }
        require(profile.baseUrl.startsWith("https://")) { "Provider endpoint must use HTTPS" }
        val contents = JSONArray()
        messages.forEach { line ->
            contents.put(
                JSONObject()
                    .put("role", if (line.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", line.content)))
            )
        }
        val payload = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.35))
        val endpoint = profile.baseUrl.trimEnd('/') + "/v1beta/models/" + profile.model + ":streamGenerateContent?alt=sse&key=" + profile.apiKey
        val request = Request.Builder()
            .url(endpoint)
            .applyCustomHeaders(profile.customHeaders)
            .post(payload.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw providerError(response.code, raw)
            return parseStreamingBody(raw, ApiProtocol.GEMINI, onDelta)
        }
    }

    private fun anthropic(
        profile: ProviderProfile,
        messages: List<ChatLine>,
        system: String,
        onDelta: (String) -> Unit
    ): String {
        val endpoint = profile.baseUrl.trimEnd('/') + if (profile.baseUrl.endsWith("/v1")) "/messages" else "/v1/messages"
        val payloadMessages = JSONArray()
        messages.filter { it.role != "system" }.forEach { line ->
            payloadMessages.put(JSONObject().put("role", if (line.role == "assistant") "assistant" else "user").put("content", line.content))
        }
        val body = JSONObject()
            .put("model", profile.model)
            .put("max_tokens", 4096)
            .put("system", system)
            .put("messages", payloadMessages)
            .put("stream", true)
            .toString()
        val request = Request.Builder()
            .url(endpoint)
            .header("x-api-key", profile.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Accept", "text/event-stream")
            .applyCustomHeaders(profile.customHeaders)
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw providerError(response.code, raw)
            return parseStreamingBody(raw, ApiProtocol.ANTHROPIC, onDelta)
        }
    }

    fun test(profile: ProviderProfile): String {
        return kotlinx.coroutines.runBlocking {
            generate(profile, listOf(ChatLine("user", "Reply with exactly OK.")), "You are testing a connection.", null) { }
        }
    }

    private fun parseStreamingBody(raw: String, protocol: ApiProtocol, onDelta: (String) -> Unit): String {
        if (raw.isBlank()) throw IOException("Provider returned an empty response")
        val output = StringBuilder()
        val lines = raw.lineSequence().toList()
        val hasDataLines = lines.any { it.trimStart().startsWith("data:") }
        if (!hasDataLines) {
            val json = runCatching { JSONObject(raw) }.getOrNull()
                ?: throw IOException("Provider returned malformed JSON")
            val text = extractFullText(json, protocol)
            if (text.isBlank()) throw IOException("Provider response did not contain text")
            onDelta(text)
            return text
        }
        lines.forEach { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) return@forEach
            val data = trimmed.removePrefix("data:").trim()
            if (data.isBlank() || data == "[DONE]") return@forEach
            val json = runCatching { JSONObject(data) }.getOrNull() ?: return@forEach
            val text = extractDelta(json, protocol)
            if (text.isNotEmpty()) {
                output.append(text)
                onDelta(text)
            }
        }
        if (output.isEmpty()) throw IOException("Provider response contained no user-facing text")
        return output.toString()
    }

    private fun extractDelta(json: JSONObject, protocol: ApiProtocol): String = when (protocol) {
        ApiProtocol.OPENAI_CHAT -> {
            val choices = json.optJSONArray("choices") ?: return ""
            val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return ""
            contentText(delta.opt("content"))
        }
        ApiProtocol.GEMINI -> {
            val candidates = json.optJSONArray("candidates") ?: return ""
            val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return ""
            contentText(parts.optJSONObject(0)?.opt("text"))
        }
        ApiProtocol.ANTHROPIC -> {
            val delta = json.optJSONObject("delta") ?: return ""
            contentText(delta.opt("text"))
        }
        ApiProtocol.PUTER_WEB -> ""
    }

    private fun extractFullText(json: JSONObject, protocol: ApiProtocol): String = when (protocol) {
        ApiProtocol.OPENAI_CHAT -> contentText(
            json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
        )
        ApiProtocol.GEMINI -> contentText(
            json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.opt("text")
        )
        ApiProtocol.ANTHROPIC -> {
            val content = json.optJSONArray("content")?.optJSONObject(0)
            contentText(content?.opt("text"))
        }
        ApiProtocol.PUTER_WEB -> ""
    }

    private fun contentText(value: Any?): String = when (value) {
        is String -> value
        is JSONArray -> buildString {
            for (i in 0 until value.length()) {
                val item = value.optJSONObject(i)
                append(item?.optString("text").orEmpty())
            }
        }
        else -> ""
    }

    private fun Request.Builder.applyCustomHeaders(raw: String): Request.Builder {
        if (raw.isBlank()) return this
        runCatching {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.equals("Authorization", ignoreCase = true) || key.contains("api-key", true)) continue
                header(key, json.optString(key))
            }
        }
        return this
    }

    private fun openAiEndpoint(base: String): String {
        require(base.startsWith("https://")) { "Provider endpoint must use HTTPS" }
        val clean = base.trimEnd('/')
        return when {
            clean.endsWith("/chat/completions") -> clean
            clean.endsWith("/v1") -> "$clean/chat/completions"
            else -> "$clean/v1/chat/completions"
        }
    }

    private fun providerError(code: Int, body: String): IOException {
        val message = runCatching { JSONObject(body).optString("error").ifBlank { JSONObject(body).optString("message") } }.getOrNull()
            .orEmpty()
        val suffix = message.ifBlank { body.take(260).replace("\n", " ") }
        return IOException("Provider HTTP $code: $suffix")
    }

    companion object { private val JSON = "application/json; charset=utf-8".toMediaType() }
}
