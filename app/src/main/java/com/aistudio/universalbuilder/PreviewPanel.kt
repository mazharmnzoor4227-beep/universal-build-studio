package com.aistudio.universalbuilder

import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PreviewPanel(
    appName: String,
    projectType: String,
    previewStatus: String,
    previewMessage: String,
    previewUrl: String?,
    iconUri: Uri?
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF11151D),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = "APP PREVIEW",
            color = Color(0xFFA58BFF),
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = previewStatus,
            color = when {
                previewStatus.contains("✓") ->
                    Color(0xFF51DFA8)

                previewStatus.contains("⚠") ->
                    Color(0xFFFFC857)

                previewStatus.contains("✕") ->
                    Color(0xFFFF6B6B)

                else ->
                    Color.White
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = previewMessage,
            color = Color(0xFF949EAE),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        if (previewUrl != null) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(20.dp)
                    )
            ) {

                AndroidView(
                    factory = { context ->

                        WebView(context).apply {

                            webViewClient =
                                WebViewClient()

                            webChromeClient =
                                WebChromeClient()

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.mediaPlaybackRequiresUserGesture = false
                        }
                    },
                    update = { webView ->

                        if (webView.url != previewUrl) {
                            webView.loadUrl(previewUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

        } else {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Color(0xFF090C12),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    if (iconUri != null) {

                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    scaleType =
                                        ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = {
                                it.setImageURI(iconUri)
                            },
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                    }

                    Text(
                        text = appName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = projectType,
                        color = Color(0xFF8D97A7),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
