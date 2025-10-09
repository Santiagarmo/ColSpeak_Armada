package com.example.speak.helpers;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.util.Log;

import com.example.speak.R;

public class StarEarnedDialog extends Dialog {
    
    private static final String TAG = "StarEarnedDialog";
    private static final int AUTO_DISMISS_DELAY = 5000; // 5 segundos
    private Context context;
    private Handler autoDismissHandler;
    private Runnable autoDismissRunnable;
    
    public StarEarnedDialog(Context context) {
        super(context);
        this.context = context;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar el diálogo
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.star_earned_dialog);
        
        // Hacer el diálogo no cancelable
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        
        // Configurar el botón de cerrar
        Button closeButton = findViewById(R.id.btnCloseStarDialog);
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        
        // Configurar auto-dismiss
        setupAutoDismiss();
        
        Log.d(TAG, "Modal de estrella creado");
    }
    
    /**
     * Configura el auto-dismiss del modal
     */
    private void setupAutoDismiss() {
        autoDismissHandler = new Handler(Looper.getMainLooper());
        autoDismissRunnable = new Runnable() {
            @Override
            public void run() {
                if (isShowing()) {
                    dismiss();
                    Log.d(TAG, "Modal de estrella cerrado automáticamente después de 5 segundos");
                }
            }
        };
    }
    
    @Override
    public void show() {
        super.show();
        
        // Programar auto-dismiss
        if (autoDismissHandler != null && autoDismissRunnable != null) {
            autoDismissHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY);
        }
        
        Log.d(TAG, "Modal de estrella mostrado - se cerrará automáticamente en 5 segundos");
    }
    
    @Override
    public void dismiss() {
        // Cancelar auto-dismiss si se cierra manualmente
        if (autoDismissHandler != null && autoDismissRunnable != null) {
            autoDismissHandler.removeCallbacks(autoDismissRunnable);
        }
        
        super.dismiss();
    }
    
    /**
     * Muestra el modal de estrella ganada
     */
    public static void show(Context context) {
        try {
            StarEarnedDialog dialog = new StarEarnedDialog(context);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar modal de estrella: " + e.getMessage());
        }
    }
}