package com.example.speak.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.speak.R
import com.example.speak.helpers.ModalAnimationHelper

/**
 * Componente reutilizable para modales de alerta (correcto/incorrecto)
 * Se puede usar en cualquier actividad
 */
class ModalAlertComponent : LinearLayout {
    // Views
    private var topDivider: View? = null
    private var decorativeLine: View? = null
    private var bannerLayout: LinearLayout? = null
    private var modalIcon: ImageView? = null
    private var bannerText: TextView? = null
    private var messageLayout: LinearLayout? = null
    private var primaryMessage: TextView? = null
    private var secondaryMessage: TextView? = null
    private var continueButton: Button? = null

    /**
     * Get current modal type
     */
    // Configuration
    var currentType: ModalType = ModalType.CORRECT
        private set
    private var listener: OnModalActionListener? = null
    private var isProcessing = false

    // Modal types
    enum class ModalType {
        CORRECT,
        INCORRECT
    }

    // Interface for callbacks
    interface OnModalActionListener {
        fun onContinuePressed(type: ModalType?)
        fun onModalHidden(type: ModalType?)
    }

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        try {
            // Inflate the layout
            LayoutInflater.from(context).inflate(R.layout.modal_alert_component, this, true)

            // Initialize views
            topDivider = findViewById<View?>(R.id.topDivider)
            decorativeLine = findViewById<View?>(R.id.decorativeLine)
            bannerLayout = findViewById<LinearLayout?>(R.id.bannerLayout)
            modalIcon = findViewById<ImageView?>(R.id.modalIcon)
            bannerText = findViewById<TextView?>(R.id.bannerText)
            messageLayout = findViewById<LinearLayout?>(R.id.messageLayout)
            primaryMessage = findViewById<TextView?>(R.id.primaryMessage)
            secondaryMessage = findViewById<TextView?>(R.id.secondaryMessage)
            continueButton = findViewById<Button?>(R.id.continueButton)

            // Set up click listener
            if (continueButton != null) {
                continueButton!!.setOnClickListener { v: View? ->
                    // Prevenir múltiples clics
                    if (isProcessing) {
                        Log.d(TAG, "Button click ignored - already processing")
                        return@setOnClickListener
                    }

                    isProcessing = true
                    continueButton!!.setEnabled(false)

                    hideModal()
                    if (listener != null) {
                        listener!!.onContinuePressed(currentType)
                    }
                }
            }

            // Initially hidden
            setVisibility(GONE)

            Log.d(TAG, "ModalAlertComponent initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ModalAlertComponent: " + e.message, e)
        }
    }

    /**
     * Set the listener for modal actions
     */
    fun setOnModalActionListener(listener: OnModalActionListener?) {
        this.listener = listener
    }

    /**
     * Show correct answer modal
     */
    fun showCorrectModal(primaryMsg: String?, secondaryMsg: String?) {
        try {
            // Resetear el flag de procesamiento y habilitar el botón
            isProcessing = false
            if (continueButton != null) {
                continueButton!!.setEnabled(true)
            }

            currentType = ModalType.CORRECT

            if (topDivider != null) {
                topDivider!!.setVisibility(GONE)
            }

            // Mostrar la línea decorativa para correcto con color verde
            if (decorativeLine != null) {
                decorativeLine!!.setVisibility(VISIBLE)
                decorativeLine!!.setBackgroundColor(Color.parseColor("#00AA00"))
            }

            // FORZAR fondo verde con drawable programático
            val bgDrawable = GradientDrawable()
            bgDrawable.setColor(Color.parseColor("#80D580"))
            bgDrawable.setCornerRadius(dpToPx(20).toFloat())
            setBackground(bgDrawable)

            // Configurar badge REDONDEADO
            if (bannerLayout != null) {
                val badgeDrawable = GradientDrawable()
                badgeDrawable.setColor(Color.parseColor("#2E7D32"))
                badgeDrawable.setCornerRadius(dpToPx(18).toFloat())
                bannerLayout!!.setBackground(badgeDrawable)
            }

            // Configurar icono
            if (modalIcon != null) {
                modalIcon!!.setImageResource(R.drawable.checkmark)
                modalIcon!!.setBackgroundResource(R.drawable.circle_green)
            }

            // Configurar textos
            if (bannerText != null) {
                bannerText!!.setText("Correct Answer")
            }
            if (primaryMessage != null) {
                primaryMessage!!.setText(if (primaryMsg != null) primaryMsg else "¡Muy bien!, tu nivel de inglés está mejorando")
                primaryMessage!!.setTextColor(Color.parseColor("#2E7D32"))
            }
            if (secondaryMessage != null) {
                secondaryMessage!!.setText(if (secondaryMsg != null) secondaryMsg else "Amazing, you are improving your English")
                secondaryMessage!!.setTextColor(Color.parseColor("#1B5E20"))
            }

            // MessageLayout transparente
            if (messageLayout != null) {
                messageLayout!!.setBackgroundColor(Color.TRANSPARENT)
            }

            ModalAnimationHelper.showCorrectAnswerModal(this, getContext())
            Log.d(TAG, "Correct modal shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing correct modal: " + e.message, e)
        }
    }

    /**
     * Show incorrect answer modal
     */
    fun showIncorrectModal(primaryMsg: String?, secondaryMsg: String?) {
        try {
            // Resetear el flag de procesamiento y habilitar el botón
            isProcessing = false
            if (continueButton != null) {
                continueButton!!.setEnabled(true)
            }

            currentType = ModalType.INCORRECT

            if (topDivider != null) {
                topDivider!!.setVisibility(GONE)
            }

            // Mostrar la línea decorativa para incorrecto con color rojo
            if (decorativeLine != null) {
                decorativeLine!!.setVisibility(VISIBLE)
                decorativeLine!!.setBackgroundColor(Color.parseColor("#DC3545"))
            }

            // FORZAR fondo ROSA con drawable programático
            val bgDrawable = GradientDrawable()
            bgDrawable.setColor(Color.parseColor("#FF8699"))
            bgDrawable.setCornerRadius(dpToPx(20).toFloat())
            setBackground(bgDrawable)

            // Configurar badge redondeado
            if (bannerLayout != null) {
                val badgeDrawable = GradientDrawable()
                badgeDrawable.setColor(Color.parseColor("#DC3545"))
                badgeDrawable.setCornerRadius(dpToPx(18).toFloat())
                bannerLayout!!.setBackground(badgeDrawable)
            }

            // Configurar icono
            if (modalIcon != null) {
                modalIcon!!.setImageResource(R.drawable.close)
                modalIcon!!.setBackgroundResource(R.drawable.circle_red)
            }

            // Configurar textos
            if (bannerText != null) {
                bannerText!!.setText("Incorrect Answer")
            }
            if (primaryMessage != null) {
                primaryMessage!!.setText(if (primaryMsg != null) primaryMsg else "¡Ten cuidado!, sigue intentando")
                primaryMessage!!.setTextColor(Color.parseColor("#FFFFFF"))
            }
            if (secondaryMessage != null) {
                secondaryMessage!!.setText(if (secondaryMsg != null) secondaryMsg else "Be careful, try again")
                secondaryMessage!!.setTextColor(Color.parseColor("#FFFFFF"))
            }

            // MessageLayout transparente
            if (messageLayout != null) {
                messageLayout!!.setBackgroundColor(Color.TRANSPARENT)
            }

            ModalAnimationHelper.showIncorrectAnswerModal(this, getContext())
            Log.d(TAG, "Incorrect modal shown with PINK background")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing incorrect modal: " + e.message, e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = getContext().getResources().getDisplayMetrics().density
        return Math.round(dp * density)
    }

    /**
     * Hide modal with animation
     */
    fun hideModal() {
        try {
            ModalAnimationHelper.hideModal(this, getContext())
            if (listener != null) {
                listener!!.onModalHidden(currentType)
            }
            Log.d(TAG, "Modal hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding modal: " + e.message, e)
            setVisibility(GONE)
        }
    }

    val isVisible: Boolean
        /**
         * Check if modal is visible
         */
        get() = getVisibility() == VISIBLE

    companion object {
        private const val TAG = "ModalAlertComponent"
    }
}