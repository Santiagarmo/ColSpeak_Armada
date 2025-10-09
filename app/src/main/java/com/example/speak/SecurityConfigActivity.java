package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.speak.helpers.SecurityManager;

public class SecurityConfigActivity extends AppCompatActivity {

    private static final String TAG = "SecurityConfigActivity";
    
    // Contraseña de administrador - CAMBIAR ANTES DE PUBLICAR
    private static final String ADMIN_PASSWORD = "ParraAdmin1075219244!";
    
    private EditText adminPasswordInput;
    private EditText newTempPasswordInput;
    private Button adminLoginButton;
    private Button changePasswordButton;
    private Button clearSecurityButton;
    private Button backButton;
    
    private TextView securityStatusText;
    private TextView deviceInfoText;
    
    private SecurityManager securityManager;
    private boolean isAdminLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_config);
        
        securityManager = new SecurityManager(this);
        initializeViews();
        setupClickListeners();
        updateSecurityInfo();
    }

    private void initializeViews() {
        adminPasswordInput = findViewById(R.id.admin_password_input);
        newTempPasswordInput = findViewById(R.id.new_temp_password_input);
        adminLoginButton = findViewById(R.id.admin_login_button);
        changePasswordButton = findViewById(R.id.change_password_button);
        clearSecurityButton = findViewById(R.id.clear_security_button);
        backButton = findViewById(R.id.back_button);
        
        securityStatusText = findViewById(R.id.security_status_text);
        deviceInfoText = findViewById(R.id.device_info_text);
        
        // Inicialmente ocultar controles de administrador
        setAdminControlsVisibility(false);
    }

    private void setupClickListeners() {
        adminLoginButton.setOnClickListener(v -> verifyAdminPassword());
        
        changePasswordButton.setOnClickListener(v -> changeTempPassword());
        
        clearSecurityButton.setOnClickListener(v -> clearSecurityData());
        
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SecurityConfigActivity.this, SecurityActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void verifyAdminPassword() {
        String enteredPassword = adminPasswordInput.getText().toString().trim();
        
        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la contraseña de administrador", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ADMIN_PASSWORD.equals(enteredPassword)) {
            isAdminLoggedIn = true;
            setAdminControlsVisibility(true);
            adminPasswordInput.setText("");
            Toast.makeText(this, "Acceso de administrador concedido", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Contraseña de administrador incorrecta", Toast.LENGTH_SHORT).show();
            adminPasswordInput.setText("");
        }
    }

    private void changeTempPassword() {
        if (!isAdminLoggedIn) {
            Toast.makeText(this, "Debe iniciar sesión como administrador", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String newPassword = newTempPasswordInput.getText().toString().trim();
        
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la nueva contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (newPassword.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Aquí se cambiaría la contraseña en SecurityManager
        // Por ahora solo mostramos un mensaje
        Toast.makeText(this, "Funcionalidad de cambio de contraseña en desarrollo", Toast.LENGTH_LONG).show();
        newTempPasswordInput.setText("");
    }

    private void clearSecurityData() {
        if (!isAdminLoggedIn) {
            Toast.makeText(this, "Debe iniciar sesión como administrador", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar diálogo de confirmación
        new android.app.AlertDialog.Builder(this)
            .setTitle("Confirmar acción")
            .setMessage("¿Está seguro de que desea limpiar todos los datos de seguridad?\n\n" +
                       "Esto requerirá que todos los dispositivos se verifiquen nuevamente.")
            .setPositiveButton("Sí, limpiar", (dialog, which) -> {
                securityManager.clearSecurityData();
                Toast.makeText(this, "Datos de seguridad limpiados", Toast.LENGTH_SHORT).show();
                updateSecurityInfo();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void setAdminControlsVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        
        newTempPasswordInput.setVisibility(visibility);
        changePasswordButton.setVisibility(visibility);
        clearSecurityButton.setVisibility(visibility);
    }

    private void updateSecurityInfo() {
        // Información del estado de seguridad
        String statusInfo = String.format(
            "Estado de Seguridad:\n" +
            "• Dispositivo autorizado: %s\n" +
            "• Intentos de verificación: %d\n",
            //"• Contraseña temporal: %s",
            securityManager.isDeviceAuthorized() ? "SÍ" : "NO",
            securityManager.getVerificationAttempts()
            //securityManager.getTempPassword()
        );
        securityStatusText.setText(statusInfo);
        
        // Información del dispositivo
        String deviceInfo = String.format(
            "Información del Dispositivo:\n" +
            "• ID del dispositivo: %s\n" +
            "• Información de debug:\n%s",
            securityManager.getDeviceId(),
            securityManager.getSecurityDebugInfo()
        );
        deviceInfoText.setText(deviceInfo);
    }

    @Override
    public void onBackPressed() {
        // Permitir volver atrás
        super.onBackPressed();
    }
}
