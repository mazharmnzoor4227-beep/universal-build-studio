package com.mazhar.nexora

import org.json.JSONObject
import java.util.UUID

enum class WorkspaceMode(val label: String, val icon: String) {
    GENERAL("General", "✦"),
    CODE("Code", "⌘"),
    WEBSITE("Website", "▣"),
    ANDROID("Android app", "▤"),
    DOCUMENT("Document", "≡")
}

enum class ApiProtocol { OPENAI_CHAT, GEMINI, ANTHROPIC, PUTER_WEB }

data class ProviderPreset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val protocol: ApiProtocol,
    val description: String,
    val docsUrl: String,
    val freeHint: Boolean = false
)

data class ProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val providerId: String,
    val model: String,
    val baseUrl: String,
    val apiKey: String,
    val protocol: ApiProtocol,
    val customHeaders: String = ""
) {
    fun maskedKey(): String = if (apiKey.length < 8) "••••••" else "••••${apiKey.takeLast(4)}"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("providerId", providerId)
        put("model", model)
        put("baseUrl", baseUrl)
        put("apiKey", apiKey)
        put("protocol", protocol.name)
        put("customHeaders", customHeaders)
    }

    companion object {
        fun fromJson(json: JSONObject): ProviderProfile? = runCatching {
            ProviderProfile(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = json.optString("name").ifBlank { "Provider" },
                providerId = json.optString("providerId", "custom"),
                model = json.optString("model").ifBlank { "openrouter/free" },
                baseUrl = json.optString("baseUrl"),
                apiKey = json.optString("apiKey"),
                protocol = ApiProtocol.valueOf(json.optString("protocol", ApiProtocol.OPENAI_CHAT.name)),
                customHeaders = json.optString("customHeaders")
            )
        }.getOrNull()
    }
}

object ProviderCatalog {
    val presets = listOf(
        ProviderPreset(
            "openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openrouter/free",
            ApiProtocol.OPENAI_CHAT, "Free router plus paid models through one OpenAI-compatible API.",
            "https://openrouter.ai/openrouter/free", true
        ),
        ProviderPreset(
            "gemini", "Google Gemini", "https://generativelanguage.googleapis.com", "gemini-2.5-flash",
            ApiProtocol.GEMINI, "Google AI Studio key with a free tier subject to current limits.",
            "https://aistudio.google.com/api-keys", true
        ),
        ProviderPreset(
            "groq", "Groq", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant",
            ApiProtocol.OPENAI_CHAT, "Very fast OpenAI-compatible inference with a developer free tier.",
            "https://console.groq.com/", true
        ),
        ProviderPreset(
            "deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat",
            ApiProtocol.OPENAI_CHAT, "OpenAI-compatible DeepSeek endpoint; availability and pricing can change.",
            "https://platform.deepseek.com/", false
        ),
        ProviderPreset(
            "anthropic", "Anthropic", "https://api.anthropic.com", "claude-3-5-haiku-latest",
            ApiProtocol.ANTHROPIC, "Native Messages API adapter.",
            "https://console.anthropic.com/", false
        ),
        ProviderPreset(
            "mistral", "Mistral", "https://api.mistral.ai/v1", "mistral-small-latest",
            ApiProtocol.OPENAI_CHAT, "OpenAI-compatible chat endpoint.",
            "https://console.mistral.ai/", false
        ),
        ProviderPreset(
            "xai", "xAI", "https://api.x.ai/v1", "grok-3-mini",
            ApiProtocol.OPENAI_CHAT, "OpenAI-compatible chat endpoint.",
            "https://console.x.ai/", false
        ),
        ProviderPreset(
            "puter", "Puter.js free connector", "", "puter-free",
            ApiProtocol.PUTER_WEB, "Optional web connector; no API key, uses Puter.js hosted sign-in and its terms.",
            "https://developer.puter.com/tutorials/free-unlimited-openrouter-api/", true
        ),
        ProviderPreset(
            "custom", "Custom OpenAI-compatible", "https://example.com/v1", "your-model",
            ApiProtocol.OPENAI_CHAT, "Use any HTTPS endpoint that follows the OpenAI chat-completions shape.",
            "", false
        )
    )

    fun preset(id: String): ProviderPreset = presets.firstOrNull { it.id == id } ?: presets.last()
}

data class ChatLine(val role: String, val content: String)

data class AppSettings(
    val autoSendVoice: Boolean = false,
    val reducedMotion: Boolean = false,
    val speakReplies: Boolean = true
)
