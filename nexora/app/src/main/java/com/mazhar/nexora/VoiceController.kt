package com.mazhar.nexora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class VoiceController(
    context: Context,
    private val onState: (String) -> Unit,
    private val onPartial: (String) -> Unit,
    private val onOneShotText: (String) -> Unit,
    private val onCallText: (String) -> Unit
) : RecognitionListener {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var callMode = false
    private var speaking = false
    private var retryCount = 0

    init {
        tts = TextToSpeech(appContext) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (ttsReady) {
                val urdu = tts?.setLanguage(Locale.forLanguageTag("ur-PK")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (urdu < 0) tts?.setLanguage(Locale.US)
                tts?.setSpeechRate(0.96f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onState("Speaking") }
                    override fun onDone(utteranceId: String?) {
                        main.post {
                            speaking = false
                            if (callMode) startListening() else onState("Ready")
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        main.post {
                            speaking = false
                            if (callMode) startListening() else onState("Voice playback unavailable")
                        }
                    }
                })
            }
        }
    }

    fun startOneShot() {
        callMode = false
        retryCount = 0
        startListening()
    }

    fun startCall() {
        callMode = true
        retryCount = 0
        onState("Listening")
        startListening()
    }

    fun stopCall() {
        callMode = false
        speaking = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        onState("Idle")
        onPartial("")
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        if (!ttsReady) {
            onState("Text-to-speech is not ready")
            return
        }
        speaking = true
        val clean = text.replace(Regex("```[\\s\\S]*?```"), "Code is available in the project workspace.")
            .replace(Regex("[#*_`>]"), "")
            .take(3800)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "nexora-${UUID.randomUUID()}")
    }

    fun shutdown() {
        stopCall()
        tts?.shutdown()
        tts = null
    }

    private fun startListening() {
        main.post {
            if (speaking) return@post
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                onState("Speech Services are unavailable")
                return@post
            }
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            onState(if (callMode) "Listening" else "Transcribing")
            recognizer?.startListening(intent)
        }
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
        onPartial("")
        if (text.isBlank()) {
            if (callMode && retryCount++ < 4) main.postDelayed({ startListening() }, 300)
            else onState("No speech detected")
            return
        }
        retryCount = 0
        if (callMode) {
            onState("Thinking")
            onCallText(text)
        } else {
            onState("Ready")
            onOneShotText(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        onPartial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
    }

    override fun onError(error: Int) {
        if (callMode && error in setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && retryCount++ < 5) {
            main.postDelayed({ startListening() }, 350)
        } else {
            onState(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Microphone permission denied" else "Voice paused")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) { onState(if (callMode) "Listening" else "Transcribing") }
    override fun onBeginningOfSpeech() { onState("Listening · speak now") }
    override fun onEndOfSpeech() { if (callMode) onState("Processing voice") }
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
