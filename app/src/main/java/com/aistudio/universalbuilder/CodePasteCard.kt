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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CodePasteCard(
    htmlCode: String,
    onCodeChange: (String) -> Unit,
    onUseCode: () -> Unit
) {

    var previous by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var toolsVisible by remember { mutableStateOf(false) }
    var showPreview by remember {
        mutableStateOf(false)
    }

    BuilderCard(
        title = "HTML editor"
    ) {

        Text(
            text = "Paste complete HTML / CSS / JavaScript code here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Row {
            TextButton(onClick={ toolsVisible=!toolsVisible }) { Text("Find / replace") }
            TextButton(onClick={ val current=htmlCode; onCodeChange(previous); previous=current },enabled=previous!=htmlCode) { Text("Undo / redo") }
        }
        if(toolsVisible) {
            OutlinedTextField(value=search,onValueChange={search=it},label={Text("Find")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=replacement,onValueChange={replacement=it},label={Text("Replace with")},modifier=Modifier.fillMaxWidth())
            TextButton(onClick={previous=htmlCode;onCodeChange(htmlCode.replace(search,replacement))},enabled=search.isNotEmpty()) { Text("Replace all") }
        }
        Text("${htmlCode.lines().size} lines • ${htmlCode.length} characters",style=MaterialTheme.typography.labelSmall)
        OutlinedTextField(
            value = htmlCode,
            textStyle=MaterialTheme.typography.bodySmall.copy(fontFamily=FontFamily.Monospace),
            onValueChange = { previous=htmlCode; onCodeChange(it) },
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
                    "Hide preview"
                } else {
                    "Preview code"
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
                "Use this code",
                fontWeight = FontWeight.Black
            )
        }
    }
}
