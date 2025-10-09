package com.example.speak

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class OfflineSpeechRecognizer(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var recognitionCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("OfflineSpeech", "Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("OfflineSpeech", "Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Puedes usar esto para mostrar la intensidad del audio
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // No es necesario implementar esto
                    }

                    override fun onEndOfSpeech() {
                        Log.d("OfflineSpeech", "End of speech")
                    }

                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera de voz agotado"
                            else -> "Error desconocido"
                        }
                        errorCallback?.invoke(errorMessage)
                        isListening = false
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val bestMatch = matches[0]
                            recognitionCallback?.invoke(bestMatch)
                        }
                        isListening = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // No es necesario implementar esto
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // No es necesario implementar esto
                    }
                })
            }
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening) return

        recognitionCallback = onResult
        errorCallback = onError
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            // Configurar para modo offline
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        if (!isListening) return
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
} 