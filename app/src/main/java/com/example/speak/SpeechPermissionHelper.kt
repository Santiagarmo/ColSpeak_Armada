package com.example.speak

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object SpeechPermissionHelper {
    private const val RECORD_AUDIO_PERMISSION_CODE = 1001

    fun checkAndRequestPermissions(activity: Activity): Boolean {
        val permissions = mutableListOf<String>()
        
        // Verificar permiso de grabación de audio
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return false
        }
        return true
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        return false
    }
} 