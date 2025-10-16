package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speak.MainActivity;

import com.example.speak.pronunciation.PronunciationActivity;

public class BeginActivity extends AppCompatActivity {

    //Declaramos las variables
    private Button eButtonBegin;

    //Return Menú
    private LinearLayout eBtnReturnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_begin);

        //Inicializamos las variables
        initializeViews();
        setupClickListeners();

        //Return Menu
    }

    private void initializeViews() {
        try {
            eButtonBegin = findViewById(R.id.eButtonBegin);
            eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);

        } catch (Exception e) {
            Toast.makeText(this, "Error al inicializar las vistas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        //Configuramos el botón de start
        eButtonBegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BeginActivity.this, MenuA1Activity.class);
                startActivity(intent);
            }
        });

        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BeginActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(BeginActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
            }
        });

    }

}
