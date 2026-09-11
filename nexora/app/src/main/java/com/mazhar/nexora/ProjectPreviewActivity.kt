package com.mazhar.nexora

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

class ProjectPreviewActivity : Activity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("project_name").orEmpty().ifBlank { "Preview" }
        val html = intent.getStringExtra("project_html").orEmpty()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF080A0D.toInt()) }
        val bar = TextView(this).apply {
            text = "  $title  ·  Preview"
            textSize = 15f
            setTextColor(0xFFEAF2EE.toInt())
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFF11161B.toInt())
        }
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        val safe = if (html.isBlank()) "<html><body><h2>No previewable HTML file</h2></body></html>" else html
        web.loadDataWithBaseURL("https://nexora.local/", safe, "text/html", "UTF-8", null)
    }
}
