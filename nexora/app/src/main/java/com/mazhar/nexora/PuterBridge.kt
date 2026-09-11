package com.mazhar.nexora

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Puter is not a normal API-key provider. This isolated bridge runs only when the user
 * explicitly selects the Puter profile and never exposes Android interfaces to the page.
 */
class PuterBridge(private val activity: Activity) {
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun generate(prompt: String, onDelta: (String) -> Unit): String = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            var finished = false
            val webView = WebView(activity)
            val callback = object {
                @JavascriptInterface
                fun onResult(payload: String) {
                    if (finished) return
                    finished = true
                    val json = runCatching { JSONObject(payload) }.getOrNull()
                    val error = json?.optString("error").orEmpty()
                    val text = json?.optString("text").orEmpty()
                    webView.post {
                        webView.stopLoading()
                        webView.destroy()
                        if (error.isNotBlank()) continuation.resumeWithException(java.io.IOException(error))
                        else if (text.isBlank()) continuation.resumeWithException(java.io.IOException("Puter returned no text"))
                        else {
                            onDelta(text)
                            continuation.resume(text)
                        }
                    }
                }
            }
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            webView.addJavascriptInterface(callback, "NexoraPuter")
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript("window.nexoraRun(${JSONObject.quote(prompt)});", null)
                }
            }
            webView.loadDataWithBaseURL("https://puter.com/", BRIDGE_HTML, "text/html", "UTF-8", null)
            continuation.invokeOnCancellation {
                finished = true
                webView.post { webView.stopLoading(); webView.destroy() }
            }
        }
    }

    companion object {
        private const val BRIDGE_HTML = """
<!doctype html><html><head><meta name='viewport' content='width=device-width'></head><body>
<script src='https://js.puter.com/v2/'></script>
<script>
window.nexoraRun = async function(prompt) {
  try {
    let tries = 0;
    while (!window.puter && tries++ < 120) await new Promise(r => setTimeout(r, 100));
    if (!window.puter) throw new Error('Puter.js did not load. Check internet access.');
    const result = await puter.ai.chat(prompt);
    let text = typeof result === 'string' ? result : (result?.message?.content || result?.text || result?.content || JSON.stringify(result));
    window.NexoraPuter.onResult(JSON.stringify({ok:true,text:String(text)}));
  } catch (e) {
    window.NexoraPuter.onResult(JSON.stringify({ok:false,error:String(e)}));
  }
};
</script></body></html>
"""
    }
}
