package com.example.speak.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

/**
 * Clase utilitaria para manejar la seguridad temporal de la aplicación
 * Esta clase controla el acceso a la aplicación antes de su lanzamiento oficial
 */
public class SecurityManager {
    
    private static final String TAG = "SecurityManager";
    private static final String PREF_SECURITY = "security_prefs";
    private static final String KEY_DEVICE_VERIFIED = "device_verified_";
    private static final String KEY_VERIFICATION_DATE = "verification_date_";
    private static final String KEY_VERIFICATION_COUNT = "verification_count_";
    
    // Contraseña temporal - CAMBIAR ANTES DE PUBLICAR EN PLAY STORE
    private static final String TEMP_PASSWORD = "Parra1075219244!";
    
    // Configuración de seguridad
    private static final long MAX_VERIFICATION_AGE_DAYS = 30; // Días máximos para la verificación
    private static final int MAX_VERIFICATION_ATTEMPTS = 5; // Intentos máximos por sesión
    
    private Context context;
    private String deviceId;
    
    public SecurityManager(Context context) {
        this.context = context;
        this.deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    
    /**
     * Verifica si el dispositivo actual tiene acceso autorizado
     */
    public boolean isDeviceAuthorized() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String deviceKey = KEY_DEVICE_VERIFIED + deviceId;
        
        boolean isVerified = prefs.getBoolean(deviceKey, false);
        
        if (isVerified) {
            // Verificar que la verificación no sea muy antigua
            if (isVerificationExpired()) {
                // Marcar como no verificado si ha expirado
                markDeviceAsUnauthorized();
                return false;
            }
        }
        
        return isVerified;
    }
    
    /**
     * Verifica si la contraseña ingresada es correcta
     */
    public boolean verifyPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        boolean isValid = TEMP_PASSWORD.equals(password.trim());
        
        if (isValid) {
            markDeviceAsAuthorized();
            Log.d(TAG, "Dispositivo autorizado: " + deviceId);
        } else {
            incrementVerificationAttempts();
            Log.w(TAG, "Intento de verificación fallido para dispositivo: " + deviceId);
        }
        
        return isValid;
    }
    
    /**
     * Marca el dispositivo como autorizado
     */
    private void markDeviceAsAuthorized() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        String deviceKey = KEY_DEVICE_VERIFIED + deviceId;
        String dateKey = KEY_VERIFICATION_DATE + deviceId;
        String countKey = KEY_VERIFICATION_COUNT + deviceId;
        
        editor.putBoolean(deviceKey, true);
        editor.putLong(dateKey, System.currentTimeMillis());
        editor.putInt(countKey, 0); // Resetear contador de intentos
        editor.apply();
    }
    
    /**
     * Marca el dispositivo como no autorizado
     */
    private void markDeviceAsUnauthorized() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        String deviceKey = KEY_DEVICE_VERIFIED + deviceId;
        editor.putBoolean(deviceKey, false);
        editor.apply();
        
        Log.i(TAG, "Verificación expirada para dispositivo: " + deviceId);
    }
    
    /**
     * Verifica si la verificación ha expirado
     */
    private boolean isVerificationExpired() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String dateKey = KEY_VERIFICATION_DATE + deviceId;
        
        long verificationDate = prefs.getLong(dateKey, 0);
        long currentTime = System.currentTimeMillis();
        long daysSinceVerification = (currentTime - verificationDate) / (1000 * 60 * 60 * 24);
        
        return daysSinceVerification > MAX_VERIFICATION_AGE_DAYS;
    }
    
    /**
     * Incrementa el contador de intentos de verificación
     */
    private void incrementVerificationAttempts() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String countKey = KEY_VERIFICATION_COUNT + deviceId;
        
        int currentAttempts = prefs.getInt(countKey, 0);
        int newAttempts = currentAttempts + 1;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(countKey, newAttempts);
        editor.apply();
        
        Log.w(TAG, "Intento de verificación #" + newAttempts + " para dispositivo: " + deviceId);
    }
    
    /**
     * Obtiene el número de intentos de verificación actuales
     */
    public int getVerificationAttempts() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String countKey = KEY_VERIFICATION_COUNT + deviceId;
        return prefs.getInt(countKey, 0);
    }
    
    /**
     * Verifica si se han excedido los intentos máximos de verificación
     */
    public boolean hasExceededMaxAttempts() {
        return getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS;
    }
    
    /**
     * Obtiene el ID del dispositivo actual
     */
    public String getDeviceId() {
        return deviceId;
    }
    
    /**
     * Obtiene la contraseña temporal (solo para debugging)
     */
    public String getTempPassword() {
        return TEMP_PASSWORD;
    }
    
    /**
     * Limpia todos los datos de seguridad (útil para testing)
     */
    public void clearSecurityData() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        String deviceKey = KEY_DEVICE_VERIFIED + deviceId;
        String dateKey = KEY_VERIFICATION_DATE + deviceId;
        String countKey = KEY_VERIFICATION_COUNT + deviceId;
        
        editor.remove(deviceKey);
        editor.remove(dateKey);
        editor.remove(countKey);
        editor.apply();
        
        Log.i(TAG, "Datos de seguridad limpiados para dispositivo: " + deviceId);
    }
    
    /**
     * Obtiene información de debug sobre el estado de seguridad
     */
    public String getSecurityDebugInfo() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURITY, Context.MODE_PRIVATE);
        String deviceKey = KEY_DEVICE_VERIFIED + deviceId;
        String dateKey = KEY_VERIFICATION_DATE + deviceId;
        String countKey = KEY_VERIFICATION_COUNT + deviceId;
        
        boolean isVerified = prefs.getBoolean(deviceKey, false);
        long verificationDate = prefs.getLong(dateKey, 0);
        int attempts = prefs.getInt(countKey, 0);
        
        return String.format(
            "Device ID: %s\n" +
            "Authorized: %s\n" +
            "Verification Date: %s\n" +
            "Attempts: %d\n" +
            "Expired: %s",
            deviceId,
            isVerified,
            verificationDate > 0 ? new java.util.Date(verificationDate).toString() : "Never",
            attempts,
            isVerificationExpired()
        );
    }
}
