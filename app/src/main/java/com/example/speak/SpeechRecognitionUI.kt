package com.example.speak

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SpeechRecognitionUI @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var startButton: Button
    private var stopButton: Button
    private var resultTextView: TextView
    private var speechRecognizer: OfflineSpeechRecognizer? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.speech_recognition_ui, this, true)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resultTextView = findViewById(R.id.resultTextView)

        setupButtons()
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            if (context is Activity && SpeechPermissionHelper.checkAndRequestPermissions(context as Activity)) {
                startListening()
            }
        }

        stopButton.setOnClickListener {
            stopListening()
        }
    }

    private fun startListening() {
        speechRecognizer = OfflineSpeechRecognizer(context)
        speechRecognizer?.startListening(
            onResult = { result ->
                resultTextView.text = result
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (SpeechPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            startListening()
        }
    }
} 