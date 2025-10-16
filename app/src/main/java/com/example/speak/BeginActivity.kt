package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BeginActivity : AppCompatActivity() {
    //Declaramos las variables
    private var eButtonBegin: Button? = null

    //Return Menú
    private var eBtnReturnMenu: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_begin)

        //Inicializamos las variables
        initializeViews()
        setupClickListeners()

        //Return Menu
    }

    private fun initializeViews() {
        try {
            eButtonBegin = findViewById<Button>(R.id.eButtonBegin)
            eBtnReturnMenu = findViewById<LinearLayout>(R.id.eBtnReturnMenu)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        //Configuramos el botón de start
        eButtonBegin!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@BeginActivity, MenuA1Activity::class.java)
                startActivity(intent)
            }
        })

        eBtnReturnMenu!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@BeginActivity, MainActivity::class.java)
                startActivity(intent)
                Toast.makeText(
                    this@BeginActivity,
                    "Has retornado al menú correctamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
