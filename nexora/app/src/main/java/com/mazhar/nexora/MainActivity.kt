package com.mazhar.nexora

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: NexoraViewModel by viewModels()
    private var pendingMicAction: (() -> Unit)? = null
    private lateinit var voiceController: VoiceController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceController = VoiceController(
            context = this,
            onState = { state -> viewModel.setVoiceStatus(state) },
            onPartial = { partial -> viewModel.setVoiceStatus(viewModel.voiceStatus.value, partial) },
            onOneShotText = { text -> NexoraUiEvents.onVoiceText(text) },
            onCallText = { text ->
                viewModel.setVoiceStatus("Thinking", text)
                viewModel.sendMessage(text, this) { reply ->
                    if (viewModel.settings.value.speakReplies) voiceController.speak(reply)
                }
            }
        )
        setContent {
            NexoraTheme {
                Surface(color = NexoraColors.background) {
                    NexoraApp(
                        viewModel = viewModel,
                        onRequestVoiceTyping = { requestMicrophone { voiceController.startOneShot() } },
                        onToggleVoiceCall = {
                            if (viewModel.voiceCallActive.value) {
                                voiceController.stopCall()
                                viewModel.setVoiceCallActive(false)
                                viewModel.setVoiceStatus("Idle")
                            } else {
                                requestMicrophone {
                                    viewModel.setVoiceCallActive(true)
                                    voiceController.startCall()
                                }
                            }
                        },
                        onReadAloud = { voiceController.speak(it) },
                        onPreviewProject = { project ->
                            startActivity(Intent(this, ProjectPreviewActivity::class.java).apply {
                                putExtra("project_name", project.name)
                                putExtra("project_html", project.previewHtml)
                            })
                        },
                        onVoiceTextDraft = { NexoraUiEvents.deliverVoiceText(it) }
                    )
                }
            }
        }
    }

    private fun requestMicrophone(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingMicAction = action
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC) {
            val action = pendingMicAction
            pendingMicAction = null
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) action?.invoke()
            else viewModel.setVoiceStatus("Microphone permission denied")
        }
    }

    override fun onDestroy() {
        voiceController.shutdown()
        super.onDestroy()
    }

    companion object { private const val REQUEST_MIC = 7001 }
}

object NexoraUiEvents {
    private var voiceTextConsumer: ((String) -> Unit)? = null
    fun registerVoiceTextConsumer(consumer: (String) -> Unit) { voiceTextConsumer = consumer }
    fun onVoiceText(text: String) { voiceTextConsumer?.invoke(text) }
    fun deliverVoiceText(text: String) { onVoiceText(text) }
}

object NexoraColors {
    val background = Color(0xFF080A0D)
    val surface = Color(0xFF11161B)
    val surfaceHigh = Color(0xFF172027)
    val line = Color(0xFF26323A)
    val text = Color(0xFFEAF2EE)
    val muted = Color(0xFF98A8A2)
    val accent = Color(0xFFB9F6E2)
    val accentDeep = Color(0xFF5DD8B2)
    val warning = Color(0xFFF3C77B)
}

@androidx.compose.runtime.Composable
fun NexoraTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = NexoraColors.accent,
        onPrimary = Color(0xFF07100D),
        secondary = NexoraColors.accentDeep,
        background = NexoraColors.background,
        surface = NexoraColors.surface,
        surfaceVariant = NexoraColors.surfaceHigh,
        onBackground = NexoraColors.text,
        onSurface = NexoraColors.text,
        onSurfaceVariant = NexoraColors.muted
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
