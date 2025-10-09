package com.example.speak.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.util.Log;
import com.example.speak.R;

public class StarAnimationHelper {
    
    private static final String TAG = "StarAnimationHelper";
    
    /**
     * Muestra la animación de estrella voladora
     */
    public static void showStarAnimation(Activity activity) {
        try {
            // Crear el layout de la animación
            LayoutInflater inflater = activity.getLayoutInflater();
            View starView = inflater.inflate(R.layout.star_animation, null);
            
            // Obtener la estrella
            ImageView flyingStar = starView.findViewById(R.id.flyingStar);
            if (flyingStar == null) {
                Log.e(TAG, "No se pudo encontrar la estrella en el layout");
                return;
            }
            
            // Agregar la vista al contenedor principal
            FrameLayout rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.addView(starView);
                
                // Hacer visible la estrella
                flyingStar.setVisibility(View.VISIBLE);
                
                // Cargar y ejecutar la animación
                Animation starAnimation = AnimationUtils.loadAnimation(activity, R.anim.star_fly);
                
                starAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        Log.d(TAG, "Animación de estrella iniciada");
                    }
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Log.d(TAG, "Animación de estrella terminada");
                        // Remover la vista después de la animación
                        if (rootView != null) {
                            rootView.removeView(starView);
                        }
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // No necesario
                    }
                });
                
                // Iniciar la animación
                flyingStar.startAnimation(starAnimation);
                
            } else {
                Log.e(TAG, "No se pudo encontrar el contenedor principal");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar animación de estrella: " + e.getMessage());
        }
    }
    
    /**
     * Muestra la animación de estrella con callback
     */
    public static void showStarAnimation(Activity activity, Runnable onComplete) {
        try {
            LayoutInflater inflater = activity.getLayoutInflater();
            View starView = inflater.inflate(R.layout.star_animation, null);
            
            ImageView flyingStar = starView.findViewById(R.id.flyingStar);
            if (flyingStar == null) {
                Log.e(TAG, "No se pudo encontrar la estrella en el layout");
                if (onComplete != null) onComplete.run();
                return;
            }
            
            FrameLayout rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.addView(starView);
                flyingStar.setVisibility(View.VISIBLE);
                
                Animation starAnimation = AnimationUtils.loadAnimation(activity, R.anim.star_fly);
                
                starAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        Log.d(TAG, "Animación de estrella iniciada");
                    }
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Log.d(TAG, "Animación de estrella terminada");
                        if (rootView != null) {
                            rootView.removeView(starView);
                        }
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // No necesario
                    }
                });
                
                flyingStar.startAnimation(starAnimation);
                
            } else {
                Log.e(TAG, "No se pudo encontrar el contenedor principal");
                if (onComplete != null) onComplete.run();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar animación de estrella: " + e.getMessage());
            if (onComplete != null) onComplete.run();
        }
    }
}
