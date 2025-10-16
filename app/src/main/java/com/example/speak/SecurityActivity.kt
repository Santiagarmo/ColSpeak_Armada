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

public class SecurityActivity extends AppCompatActivity {

    private static final String TAG = "SecurityActivity";
    
    private EditText passwordInput;
    private Button verifyButton;
    private Button configButton;
    private TextView titleText;
    private TextView descriptionText;
    private TextView attemptsText;
    
    private SecurityManager securityManager;
    private boolean isDeviceVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);
        
        // Inicializar SecurityManager
        securityManager = new SecurityManager(this);
        
        // Verificar si el dispositivo ya fue verificado
        checkDeviceVerification();
        
        if (isDeviceVerified) {
            // Dispositivo ya verificado, continuar a la aplicación
            proceedToApp();
        } else {
            // Configurar la interfaz de verificación
            setupSecurityInterface();
        }
    }

    private void checkDeviceVerification() {
        isDeviceVerified = securityManager.isDeviceAuthorized();
    }

    private void setupSecurityInterface() {
        passwordInput = findViewById(R.id.password_input);
        verifyButton = findViewById(R.id.verify_button);
        configButton = findViewById(R.id.config_button);
        titleText = findViewById(R.id.security_title);
        descriptionText = findViewById(R.id.security_description);
        attemptsText = findViewById(R.id.attempts_text);
        
        // Configurar texto explicativo
        titleText.setText("Verificación de Seguridad");
        descriptionText.setText("Esta aplicación requiere verificación temporal antes de su lanzamiento oficial.\n\n" +
                "Ingrese la contraseña de acceso temporal para continuar.\n\n" +
                "Esta verificación solo se requiere una vez por dispositivo.");
        
        // Actualizar información de intentos
        updateAttemptsInfo();
        
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });
        
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSecurityConfig();
            }
        });
        
        // Permitir verificación al presionar Enter
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            verifyPassword();
            return true;
        });
    }

    private void verifyPassword() {
        // Verificar si se han excedido los intentos máximos
        if (securityManager.hasExceededMaxAttempts()) {
            Toast.makeText(this, "Demasiados intentos fallidos. Intente más tarde.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String enteredPassword = passwordInput.getText().toString().trim();
        
        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (securityManager.verifyPassword(enteredPassword)) {
            // Contraseña correcta
            Toast.makeText(this, "Verificación exitosa", Toast.LENGTH_SHORT).show();
            proceedToApp();
        } else {
            // Contraseña incorrecta
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
            passwordInput.requestFocus();
            
            // Actualizar información de intentos
            updateAttemptsInfo();
        }
    }

    private void updateAttemptsInfo() {
        int attempts = securityManager.getVerificationAttempts();
        int maxAttempts = 5; // Debe coincidir con MAX_VERIFICATION_ATTEMPTS en SecurityManager
        
        if (attempts > 0) {
            attemptsText.setText(String.format("Intentos: %d/%d", attempts, maxAttempts));
            attemptsText.setVisibility(View.VISIBLE);
        } else {
            attemptsText.setVisibility(View.GONE);
        }
    }

    private void proceedToApp() {
        // Ir al splash screen después de la autenticación exitosa
        Intent intent = new Intent(SecurityActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevenir salida de la aplicación sin verificación
        if (!isDeviceVerified) {
            Toast.makeText(this, "Debe completar la verificación para continuar", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
    
    private void openSecurityConfig() {
        Intent intent = new Intent(SecurityActivity.this, SecurityConfigActivity.class);
        startActivity(intent);
    }
}
