package com.aistudio.universalbuilder

import android.net.Uri
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = "App preview",
            color = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        if (previewUrl != null) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
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

                            settings.mediaPlaybackRequiresUserGesture =
                                false

                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = false

                            overScrollMode =
                                WebView.OVER_SCROLL_ALWAYS

                            setOnTouchListener { view, event ->

                                when (event.actionMasked) {

                                    MotionEvent.ACTION_DOWN,
                                    MotionEvent.ACTION_MOVE -> {

                                        view.parent
                                            ?.requestDisallowInterceptTouchEvent(
                                                true
                                            )
                                    }

                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> {

                                        view.parent
                                            ?.requestDisallowInterceptTouchEvent(
                                                false
                                            )
                                    }
                                }

                                false
                            }
                        }
                    },

                    update = { webView ->

                        if (webView.url != previewUrl) {

                            webView.loadUrl(
                                previewUrl
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxSize()
                )
            }

        } else {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment =
                    Alignment.Center
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

                                it.setImageURI(
                                    iconUri
                                )
                            },

                            modifier =
                                Modifier.size(72.dp)
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )
                    }

                    Text(
                        text = appName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(3.dp)
                    )

                    Text(
                        text = projectType,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
