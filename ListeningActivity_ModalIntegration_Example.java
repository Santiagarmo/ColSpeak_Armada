package com.example.speak;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import com.example.speak.helpers.ModalAnimationHelper;

/**
 * EJEMPLO de cómo integrar los modales de alerta en ListeningActivity
 * 
 * IMPORTANTE: Este es solo un ejemplo. Para implementar completamente,
 * necesitas agregar estas modificaciones a tu ListeningActivity.java existente.
 */
public class ListeningActivity_ModalIntegration_Example extends AppCompatActivity {
    
    // Variables para los modales (agregar a ListeningActivity)
    private LinearLayout correctAnswerModal;
    private LinearLayout incorrectAnswerModal;
    private Button continueCorrectButton;
    private Button continueIncorrectButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);
        
        // Inicializar los modales (agregar después de las otras inicializaciones)
        initializeModals();
        
        // ... resto del código existente ...
    }
    
    /**
     * Método para inicializar los modales (AGREGAR a ListeningActivity)
     */
    private void initializeModals() {
        try {
            // Inicializar vistas de los modales
            correctAnswerModal = findViewById(R.id.correctAnswerModal);
            incorrectAnswerModal = findViewById(R.id.incorrectAnswerModal);
            continueCorrectButton = findViewById(R.id.continueCorrectButton);
            continueIncorrectButton = findViewById(R.id.continueIncorrectButton);
            
            // Configurar listeners de los botones
            if (continueCorrectButton != null) {
                continueCorrectButton.setOnClickListener(v -> hideCorrectModal());
            }
            
            if (continueIncorrectButton != null) {
                continueIncorrectButton.setOnClickListener(v -> hideIncorrectModal());
            }
            
            // Asegurar que los modales estén ocultos inicialmente
            if (correctAnswerModal != null) {
                correctAnswerModal.setVisibility(View.GONE);
            }
            if (incorrectAnswerModal != null) {
                incorrectAnswerModal.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "Modals initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing modals: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método modificado checkAnswer con integración de modales
     * REEMPLAZAR el método checkAnswer existente en ListeningActivity
     */
    private void checkAnswer(int buttonIndex) {
        // ... código existente hasta la evaluación de la respuesta ...
        
        if (isCorrect) {
            // ... código existente para respuesta correcta ...
            
            // MOSTRAR MODAL DE RESPUESTA CORRECTA
            showCorrectAnswerModal();
            
        } else {
            // ... código existente para respuesta incorrecta ...
            
            // MOSTRAR MODAL DE RESPUESTA INCORRECTA
            showIncorrectAnswerModal();
        }
        
        // ... resto del código existente ...
    }
    
    /**
     * Método para mostrar el modal de respuesta correcta
     * AGREGAR a ListeningActivity
     */
    private void showCorrectAnswerModal() {
        try {
            if (correctAnswerModal != null) {
                // Ocultar cualquier modal incorrecto que esté visible
                ModalAnimationHelper.showCorrectAndHideIncorrect(
                    correctAnswerModal, 
                    incorrectAnswerModal, 
                    this
                );
                
                Log.d(TAG, "Correct answer modal shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing correct answer modal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para mostrar el modal de respuesta incorrecta
     * AGREGAR a ListeningActivity
     */
    private void showIncorrectAnswerModal() {
        try {
            if (incorrectAnswerModal != null) {
                // Ocultar cualquier modal correcto que esté visible
                ModalAnimationHelper.showIncorrectAndHideCorrect(
                    correctAnswerModal, 
                    incorrectAnswerModal, 
                    this
                );
                
                Log.d(TAG, "Incorrect answer modal shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing incorrect answer modal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para ocultar el modal correcto
     * AGREGAR a ListeningActivity
     */
    private void hideCorrectModal() {
        try {
            if (correctAnswerModal != null) {
                ModalAnimationHelper.hideModal(correctAnswerModal, this);
                Log.d(TAG, "Correct modal hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding correct modal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para ocultar el modal incorrecto
     * AGREGAR a ListeningActivity
     */
    private void hideIncorrectModal() {
        try {
            if (incorrectAnswerModal != null) {
                ModalAnimationHelper.hideModal(incorrectAnswerModal, this);
                Log.d(TAG, "Incorrect modal hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding incorrect modal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para ocultar ambos modales (útil para limpiar)
     * AGREGAR a ListeningActivity
     */
    private void hideBothModals() {
        try {
            ModalAnimationHelper.hideBothModals(correctAnswerModal, incorrectAnswerModal, this);
            Log.d(TAG, "Both modals hidden");
        } catch (Exception e) {
            Log.e(TAG, "Error hiding both modals: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para verificar si algún modal está visible
     * AGREGAR a ListeningActivity
     */
    private boolean isAnyModalVisible() {
        return ModalAnimationHelper.isAnyModalVisible(correctAnswerModal, incorrectAnswerModal);
    }
    
    /**
     * Método para manejar el botón "Next" con modales
     * MODIFICAR el método existente que maneja el botón Next
     */
    private void handleNextButton() {
        try {
            // Si hay algún modal visible, ocultarlo primero
            if (isAnyModalVisible()) {
                hideBothModals();
                return; // No avanzar hasta que se oculte el modal
            }
            
            // ... resto del código existente para avanzar a la siguiente pregunta ...
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling next button: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método para limpiar modales al destruir la actividad
     * AGREGAR al método onDestroy existente
     */
    @Override
    protected void onDestroy() {
        try {
            // Ocultar modales si están visibles
            hideBothModals();
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up modals: " + e.getMessage(), e);
        }
        
        // ... resto del código existente de onDestroy ...
        super.onDestroy();
    }
}

/**
 * INSTRUCCIONES DE IMPLEMENTACIÓN:
 * 
 * 1. Agregar las variables de los modales al inicio de ListeningActivity
 * 2. Llamar a initializeModals() en onCreate() después de las otras inicializaciones
 * 3. Modificar el método checkAnswer() para mostrar los modales correspondientes
 * 4. Agregar los métodos de manejo de modales
 * 5. Modificar el manejo del botón "Next" para ocultar modales primero
 * 6. Agregar limpieza de modales en onDestroy()
 * 
 * Los modales se mostrarán automáticamente con animaciones cuando:
 * - El usuario selecciona una respuesta correcta (modal verde)
 * - El usuario selecciona una respuesta incorrecta (modal rojo)
 * 
 * Los modales se ocultan cuando:
 * - El usuario presiona el botón "Continuar / Continue"
 * - El usuario presiona el botón "Next" (si hay modales visibles)
 * - Se destruye la actividad
 */
