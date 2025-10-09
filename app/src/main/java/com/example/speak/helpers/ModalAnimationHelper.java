package com.example.speak.helpers;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.content.Context;
import android.util.Log;
import com.example.speak.R;


/**
 * Helper class para manejar las animaciones de los modales de alerta
 * Basado en los diseños de las imágenes proporcionadas
 */
public class ModalAnimationHelper {
    
    private static final String TAG = "ModalAnimationHelper";
    
    /**
     * Muestra el modal de respuesta correcta con animación
     */
    public static void showCorrectAnswerModal(View modal, Context context) {
        if (modal == null || context == null) {
            Log.w(TAG, "Modal or context is null");
            return;
        }
        
        try {
            // Hacer visible el modal
            modal.setVisibility(View.VISIBLE);
            
            // Aplicar animación de entrada desde abajo
            Animation slideUpAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_up_enter);
            modal.startAnimation(slideUpAnimation);
            
            Log.d(TAG, "Correct answer modal shown with animation");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing correct answer modal: " + e.getMessage(), e);
            // Fallback: solo hacer visible sin animación
            modal.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Muestra el modal de respuesta incorrecta con animación
     */
    public static void showIncorrectAnswerModal(View modal, Context context) {
        if (modal == null || context == null) {
            Log.w(TAG, "Modal or context is null");
            return;
        }
        
        try {
            // Hacer visible el modal
            modal.setVisibility(View.VISIBLE);
            
            // Aplicar animación de entrada desde abajo
            Animation slideUpAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_up_enter);
            modal.startAnimation(slideUpAnimation);
            
            Log.d(TAG, "Incorrect answer modal shown with animation");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing incorrect answer modal: " + e.getMessage(), e);
            // Fallback: solo hacer visible sin animación
            modal.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Oculta el modal con animación de salida
     */
    public static void hideModal(View modal, Context context) {
        if (modal == null || context == null) {
            Log.w(TAG, "Modal or context is null");
            return;
        }
        
        try {
            // Aplicar animación de salida hacia abajo
            Animation slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down_exit);
            
            // Listener para ocultar el modal cuando termine la animación
            slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // No hacer nada al inicio
                }
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    modal.setVisibility(View.GONE);
                    Log.d(TAG, "Modal hidden after animation");
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {
                    // No hacer nada
                }
            });
            
            modal.startAnimation(slideDownAnimation);
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding modal: " + e.getMessage(), e);
            // Fallback: ocultar inmediatamente sin animación
            modal.setVisibility(View.GONE);
        }
    }
    
    /**
     * Muestra el modal correcto y oculta el incorrecto
     */
    public static void showCorrectAndHideIncorrect(View correctModal, View incorrectModal, Context context) {
        try {
            // Ocultar modal incorrecto si está visible
            if (incorrectModal != null && incorrectModal.getVisibility() == View.VISIBLE) {
                hideModal(incorrectModal, context);
            }
            
            // Mostrar modal correcto
            if (correctModal != null) {
                showCorrectAnswerModal(correctModal, context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in showCorrectAndHideIncorrect: " + e.getMessage(), e);
        }
    }
    
    /**
     * Muestra el modal incorrecto y oculta el correcto
     */
    public static void showIncorrectAndHideCorrect(View correctModal, View incorrectModal, Context context) {
        try {
            // Ocultar modal correcto si está visible
            if (correctModal != null && correctModal.getVisibility() == View.VISIBLE) {
                hideModal(correctModal, context);
            }
            
            // Mostrar modal incorrecto
            if (incorrectModal != null) {
                showIncorrectAnswerModal(incorrectModal, context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in showIncorrectAndHideCorrect: " + e.getMessage(), e);
        }
    }
    
    /**
     * Oculta ambos modales
     */
    public static void hideBothModals(View correctModal, View incorrectModal, Context context) {
        try {
            if (correctModal != null && correctModal.getVisibility() == View.VISIBLE) {
                hideModal(correctModal, context);
            }
            
            if (incorrectModal != null && incorrectModal.getVisibility() == View.VISIBLE) {
                hideModal(incorrectModal, context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding both modals: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica si algún modal está visible
     */
    public static boolean isAnyModalVisible(View correctModal, View incorrectModal) {
        boolean correctVisible = correctModal != null && correctModal.getVisibility() == View.VISIBLE;
        boolean incorrectVisible = incorrectModal != null && incorrectModal.getVisibility() == View.VISIBLE;
        return correctVisible || incorrectVisible;
    }
}
