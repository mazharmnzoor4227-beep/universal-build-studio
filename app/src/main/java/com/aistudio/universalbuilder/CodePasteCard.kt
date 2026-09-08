package com.aistudio.universalbuilder

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CodePasteCard(
    htmlCode: String,
    onCodeChange: (String) -> Unit,
    onUseCode: () -> Unit
) {

    var showPreview by remember {
        mutableStateOf(false)
    }

    BuilderCard(
        title = "PASTE HTML CODE"
    ) {

        Text(
            text = "Paste complete HTML / CSS / JavaScript code here.",
            color = Color(0xFF8E98A8)
        )

        Spacer(
            Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = htmlCode,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = {
                Text(
                    "<!DOCTYPE html>..."
                )
            }
        )

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = {
                showPreview = !showPreview
            },
            enabled = htmlCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (showPreview) {
                    "HIDE PREVIEW"
                } else {
                    "PREVIEW CODE"
                },
                fontWeight = FontWeight.Bold
            )
        }

        if (showPreview) {

            Spacer(
                Modifier.height(12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),

                    factory = { context ->

                        WebView(context).apply {

                            webViewClient =
                                WebViewClient()

                            settings.javaScriptEnabled =
                                true

                            settings.domStorageEnabled =
                                true

                            settings.databaseEnabled =
                                true

                            settings.allowFileAccess =
                                true

                            settings.allowContentAccess =
                                true

                            settings.mediaPlaybackRequiresUserGesture =
                                false
                        }
                    },

                    update = { webView ->

                        webView.loadDataWithBaseURL(
                            null,
                            htmlCode,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Button(
            onClick = onUseCode,
            enabled = htmlCode.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {

            Text(
                "USE THIS CODE FOR APK",
                fontWeight = FontWeight.Black
            )
        }
    }
}
