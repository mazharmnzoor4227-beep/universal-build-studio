package com.mazhar.agentstarter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " AgentStarter/1.0");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NativeBridge(this, webView), "NativeBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    static class NativeBridge {
        private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        private final SharedPreferences prefs;
        private final WebView webView;
        private final OkHttpClient http;

        NativeBridge(Context context, WebView webView) {
            prefs = context.getSharedPreferences("agent_prefs", Context.MODE_PRIVATE);
            this.webView = webView;
            http = new OkHttpClient.Builder()
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }

        @JavascriptInterface
        public String getStoredKey() {
            return prefs.getString("api_key", "");
        }

        @JavascriptInterface
        public void saveKey(String key) {
            prefs.edit().putString("api_key", key == null ? "" : key.trim()).apply();
        }

        @JavascriptInterface
        public void clearKey() {
            prefs.edit().remove("api_key").apply();
        }

        @JavascriptInterface
        public String detectProvider(String key) {
            return providerFor(key);
        }

        @JavascriptInterface
        public void sendMessage(String requestId, String key, String messagesJson) {
            String trimmed = key == null ? "" : key.trim();
            if (trimmed.isEmpty()) {
                callback(requestId, false, "API key is missing.", "unknown");
                return;
            }

            try {
                JSONArray messages = new JSONArray(messagesJson);
                String provider = providerFor(trimmed);
                Request request = buildRequest(provider, trimmed, messages);
                http.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback(requestId, false, "Network error: " + safe(e.getMessage()), provider);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String raw = response.body() == null ? "" : response.body().string();
                        if (!response.isSuccessful()) {
                            callback(requestId, false, parseApiError(raw, response.code()), provider);
                            return;
                        }
                        try {
                            String text = parseText(provider, raw);
                            if (text == null || text.trim().isEmpty()) text = "The API returned no text.";
                            callback(requestId, true, text, provider);
                        } catch (Exception e) {
                            callback(requestId, false, "Could not parse API response: " + safe(e.getMessage()), provider);
                        }
                    }
                });
            } catch (Exception e) {
                callback(requestId, false, "Invalid request: " + safe(e.getMessage()), "unknown");
            }
        }

        private String providerFor(String key) {
            if (key == null) return "unknown";
            String k = key.trim();
            if (k.startsWith("sk-ant-")) return "anthropic";
            if (k.startsWith("sk-or-")) return "openrouter";
            if (k.startsWith("AIza")) return "gemini";
            if (k.startsWith("sk-proj-") || k.startsWith("sk-svcacct-")) return "openai";
            if (k.startsWith("sk-")) return "deepseek";
            return "unknown";
        }

        private Request buildRequest(String provider, String key, JSONArray messages) throws Exception {
            switch (provider) {
                case "openai": return openAiRequest(key, messages);
                case "deepseek": return deepSeekRequest(key, messages);
                case "anthropic": return anthropicRequest(key, messages);
                case "gemini": return geminiRequest(key, messages);
                case "openrouter": return openRouterRequest(key, messages);
                default:
                    throw new IllegalArgumentException("Unsupported API key format.");
            }
        }

        private Request openAiRequest(String key, JSONArray messages) throws Exception {
            JSONObject body = new JSONObject();
            body.put("model", "gpt-5.6-luna");
            body.put("input", toResponsesInput(messages));
            body.put("tools", new JSONArray().put(new JSONObject().put("type", "web_search")));
            body.put("max_output_tokens", 2500);
            return new Request.Builder()
                    .url("https://api.openai.com/v1/responses")
                    .addHeader("Authorization", "Bearer " + key)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
        }

        private Request deepSeekRequest(String key, JSONArray messages) throws Exception {
            JSONObject body = new JSONObject();
            body.put("model", "deepseek-v4-flash");
            body.put("input", toResponsesInput(messages));
            body.put("tools", new JSONArray().put(new JSONObject().put("type", "web_search")));
            body.put("tool_choice", "auto");
            body.put("max_output_tokens", 2500);
            return new Request.Builder()
                    .url("https://api.deepseek.com/responses")
                    .addHeader("Authorization", "Bearer " + key)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
        }

        private Request anthropicRequest(String key, JSONArray messages) throws Exception {
            JSONObject body = new JSONObject();
            body.put("model", "claude-sonnet-5");
            body.put("max_tokens", 2500);
            body.put("messages", normalizeChatMessages(messages, false));
            return new Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", key)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
        }

        private Request openRouterRequest(String key, JSONArray messages) throws Exception {
            JSONObject body = new JSONObject();
            body.put("model", "openrouter/auto");
            body.put("messages", normalizeChatMessages(messages, true));
            body.put("max_tokens", 2500);
            return new Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + key)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-OpenRouter-Title", "Agent Starter")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
        }

        private Request geminiRequest(String key, JSONArray messages) throws Exception {
            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject m = messages.getJSONObject(i);
                String role = m.optString("role", "user");
                if ("system".equals(role) || "developer".equals(role)) role = "user";
                if ("assistant".equals(role)) role = "model";
                JSONObject item = new JSONObject();
                item.put("role", role);
                item.put("parts", new JSONArray().put(new JSONObject().put("text", m.optString("content", ""))));
                contents.put(item);
            }
            body.put("contents", contents);
            body.put("generationConfig", new JSONObject().put("maxOutputTokens", 2500));
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + key;
            return new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
        }

        private JSONArray toResponsesInput(JSONArray messages) throws Exception {
            JSONArray input = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject m = messages.getJSONObject(i);
                String role = m.optString("role", "user");
                if (!role.equals("user") && !role.equals("assistant") && !role.equals("system") && !role.equals("developer")) role = "user";
                input.put(new JSONObject()
                        .put("role", role)
                        .put("content", m.optString("content", "")));
            }
            return input;
        }

        private JSONArray normalizeChatMessages(JSONArray messages, boolean allowSystem) throws Exception {
            JSONArray out = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject m = messages.getJSONObject(i);
                String role = m.optString("role", "user");
                if (role.equals("developer")) role = allowSystem ? "system" : "user";
                if (!allowSystem && role.equals("system")) role = "user";
                out.put(new JSONObject()
                        .put("role", role)
                        .put("content", m.optString("content", "")));
            }
            return out;
        }

        private String parseText(String provider, String raw) throws Exception {
            JSONObject json = new JSONObject(raw);

            if (provider.equals("openai") || provider.equals("deepseek")) {
                String top = json.optString("output_text", "");
                if (!top.isEmpty()) return top;
                JSONArray output = json.optJSONArray("output");
                if (output != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < output.length(); i++) {
                        JSONObject item = output.optJSONObject(i);
                        if (item == null || !"message".equals(item.optString("type"))) continue;
                        JSONArray content = item.optJSONArray("content");
                        if (content == null) continue;
                        for (int j = 0; j < content.length(); j++) {
                            JSONObject part = content.optJSONObject(j);
                            if (part != null && "output_text".equals(part.optString("type"))) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(part.optString("text", ""));
                            }
                        }
                    }
                    return sb.toString();
                }
            }

            if (provider.equals("anthropic")) {
                JSONArray content = json.optJSONArray("content");
                if (content != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < content.length(); i++) {
                        JSONObject part = content.optJSONObject(i);
                        if (part != null && "text".equals(part.optString("type"))) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(part.optString("text", ""));
                        }
                    }
                    return sb.toString();
                }
            }

            if (provider.equals("gemini")) {
                JSONArray candidates = json.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                    JSONArray parts = content == null ? null : content.optJSONArray("parts");
                    if (parts != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.optJSONObject(i);
                            if (part != null && part.has("text")) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(part.optString("text", ""));
                            }
                        }
                        return sb.toString();
                    }
                }
            }

            if (provider.equals("openrouter")) {
                JSONArray choices = json.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                    if (message != null) return message.optString("content", "");
                }
            }
            return "";
        }

        private String parseApiError(String raw, int status) {
            try {
                JSONObject j = new JSONObject(raw);
                JSONObject error = j.optJSONObject("error");
                if (error != null) {
                    String msg = error.optString("message", "");
                    if (!msg.isEmpty()) return "API error " + status + ": " + msg;
                }
                String msg = j.optString("message", j.optString("msg", ""));
                if (!msg.isEmpty()) return "API error " + status + ": " + msg;
            } catch (Exception ignored) {}
            String compact = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
            if (compact.length() > 300) compact = compact.substring(0, 300) + "…";
            return "API error " + status + (compact.isEmpty() ? "" : ": " + compact);
        }

        private void callback(String requestId, boolean ok, String text, String provider) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", requestId);
                payload.put("ok", ok);
                payload.put("text", text == null ? "" : text);
                payload.put("provider", provider == null ? "unknown" : provider);
                String js = "window.AgentNative && window.AgentNative.onResult(" + JSONObject.quote(payload.toString()) + ");";
                webView.post(() -> webView.evaluateJavascript(js, null));
            } catch (Exception ignored) {}
        }

        private String safe(String value) {
            return value == null ? "Unknown error" : value;
        }
    }
}
