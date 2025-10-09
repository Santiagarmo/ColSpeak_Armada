package com.example.speak.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.speak.R;
import com.example.speak.helpers.ModalAnimationHelper;

/**
 * Componente reutilizable para modales de alerta (correcto/incorrecto)
 * Se puede usar en cualquier actividad
 */
public class ModalAlertComponent extends LinearLayout {

    private static final String TAG = "ModalAlertComponent";

    // Views
    private View topDivider;
    private View decorativeLine;
    private LinearLayout bannerLayout;
    private ImageView modalIcon;
    private TextView bannerText;
    private LinearLayout messageLayout;
    private TextView primaryMessage;
    private TextView secondaryMessage;
    private Button continueButton;

    // Configuration
    private ModalType currentType = ModalType.CORRECT;
    private OnModalActionListener listener;

    // Modal types
    public enum ModalType {
        CORRECT,
        INCORRECT
    }

    // Interface for callbacks
    public interface OnModalActionListener {
        void onContinuePressed(ModalType type);
        void onModalHidden(ModalType type);
    }

    public ModalAlertComponent(Context context) {
        super(context);
        init(context);
    }

    public ModalAlertComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ModalAlertComponent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            // Inflate the layout
            LayoutInflater.from(context).inflate(R.layout.modal_alert_component, this, true);

            // Initialize views
            topDivider = findViewById(R.id.topDivider);
            decorativeLine = findViewById(R.id.decorativeLine);
            bannerLayout = findViewById(R.id.bannerLayout);
            modalIcon = findViewById(R.id.modalIcon);
            bannerText = findViewById(R.id.bannerText);
            messageLayout = findViewById(R.id.messageLayout);
            primaryMessage = findViewById(R.id.primaryMessage);
            secondaryMessage = findViewById(R.id.secondaryMessage);
            continueButton = findViewById(R.id.continueButton);

            // Set up click listener
            if (continueButton != null) {
                continueButton.setOnClickListener(v -> {
                    hideModal();
                    if (listener != null) {
                        listener.onContinuePressed(currentType);
                    }
                });
            }

            // Initially hidden
            setVisibility(GONE);

            Log.d(TAG, "ModalAlertComponent initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ModalAlertComponent: " + e.getMessage(), e);
        }
    }

    /**
     * Set the listener for modal actions
     */
    public void setOnModalActionListener(OnModalActionListener listener) {
        this.listener = listener;
    }

    /**
     * Show correct answer modal
     */
    public void showCorrectModal(String primaryMsg, String secondaryMsg) {
        try {
            currentType = ModalType.CORRECT;

            if (topDivider != null) {
                topDivider.setVisibility(GONE);
            }

            // Mostrar la línea decorativa para correcto con color verde
            if (decorativeLine != null) {
                decorativeLine.setVisibility(VISIBLE);
                decorativeLine.setBackgroundColor(android.graphics.Color.parseColor("#00AA00"));
            }

            // FORZAR fondo verde con drawable programático
            GradientDrawable bgDrawable = new GradientDrawable();
            bgDrawable.setColor(android.graphics.Color.parseColor("#80D580"));
            bgDrawable.setCornerRadius(dpToPx(20));
            setBackground(bgDrawable);

            // Configurar badge REDONDEADO
            if (bannerLayout != null) {
                GradientDrawable badgeDrawable = new GradientDrawable();
                badgeDrawable.setColor(android.graphics.Color.parseColor("#2E7D32"));
                badgeDrawable.setCornerRadius(dpToPx(18));
                bannerLayout.setBackground(badgeDrawable);
            }

            // Configurar icono
            if (modalIcon != null) {
                modalIcon.setImageResource(R.drawable.checkmark);
                modalIcon.setBackgroundResource(R.drawable.circle_green);
            }

            // Configurar textos
            if (bannerText != null) {
                bannerText.setText("Correct Answer");
            }
            if (primaryMessage != null) {
                primaryMessage.setText(primaryMsg != null ? primaryMsg : "¡Muy bien!, tu nivel de inglés está mejorando");
                primaryMessage.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
            }
            if (secondaryMessage != null) {
                secondaryMessage.setText(secondaryMsg != null ? secondaryMsg : "Amazing, you are improving your English");
                secondaryMessage.setTextColor(android.graphics.Color.parseColor("#1B5E20"));
            }

            // MessageLayout transparente
            if (messageLayout != null) {
                messageLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            ModalAnimationHelper.showCorrectAnswerModal(this, getContext());
            Log.d(TAG, "Correct modal shown");

        } catch (Exception e) {
            Log.e(TAG, "Error showing correct modal: " + e.getMessage(), e);
        }
    }

    /**
     * Show incorrect answer modal
     */
    public void showIncorrectModal(String primaryMsg, String secondaryMsg) {
        try {
            currentType = ModalType.INCORRECT;

            if (topDivider != null) {
                topDivider.setVisibility(GONE);
            }

            // Mostrar la línea decorativa para incorrecto con color rojo
            if (decorativeLine != null) {
                decorativeLine.setVisibility(VISIBLE);
                decorativeLine.setBackgroundColor(android.graphics.Color.parseColor("#DC3545"));
            }

            // FORZAR fondo ROSA con drawable programático
            GradientDrawable bgDrawable = new GradientDrawable();
            bgDrawable.setColor(android.graphics.Color.parseColor("#FF8699"));
            bgDrawable.setCornerRadius(dpToPx(20));
            setBackground(bgDrawable);

            // Configurar badge redondeado
            if (bannerLayout != null) {
                GradientDrawable badgeDrawable = new GradientDrawable();
                badgeDrawable.setColor(android.graphics.Color.parseColor("#DC3545"));
                badgeDrawable.setCornerRadius(dpToPx(18));
                bannerLayout.setBackground(badgeDrawable);
            }

            // Configurar icono
            if (modalIcon != null) {
                modalIcon.setImageResource(R.drawable.close);
                modalIcon.setBackgroundResource(R.drawable.circle_red);
            }

            // Configurar textos
            if (bannerText != null) {
                bannerText.setText("Incorrect Answer");
            }
            if (primaryMessage != null) {
                primaryMessage.setText(primaryMsg != null ? primaryMsg : "¡Ten cuidado!, sigue intentando");
                primaryMessage.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
            if (secondaryMessage != null) {
                secondaryMessage.setText(secondaryMsg != null ? secondaryMsg : "Be careful, try again");
                secondaryMessage.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            // MessageLayout transparente
            if (messageLayout != null) {
                messageLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            ModalAnimationHelper.showIncorrectAnswerModal(this, getContext());
            Log.d(TAG, "Incorrect modal shown with PINK background");

        } catch (Exception e) {
            Log.e(TAG, "Error showing incorrect modal: " + e.getMessage(), e);
        }
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Hide modal with animation
     */
    public void hideModal() {
        try {
            ModalAnimationHelper.hideModal(this, getContext());
            if (listener != null) {
                listener.onModalHidden(currentType);
            }
            Log.d(TAG, "Modal hidden");
        } catch (Exception e) {
            Log.e(TAG, "Error hiding modal: " + e.getMessage(), e);
            setVisibility(GONE);
        }
    }

    /**
     * Check if modal is visible
     */
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    /**
     * Get current modal type
     */
    public ModalType getCurrentType() {
        return currentType;
    }
}