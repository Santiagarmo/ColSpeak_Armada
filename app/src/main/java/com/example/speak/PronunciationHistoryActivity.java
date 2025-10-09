package com.example.speak;

// Importaciones necesarias para trabajar con UI, base de datos y diseño
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Importación de una clase personalizada para acceder a la base de datos
import com.example.speak.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Clase que muestra el historial de textos referentes de la pronunciación.
 * Extiende AppCompatActivity, lo que le permite ser una pantalla dentro de la app.
 */
public class PronunciationHistoryActivity extends AppCompatActivity{

    private static final String TAG = "PronunciationHistory";
    // Referencias a la base de datos y al diseño (tabla)
    private DatabaseHelper dbHelper;
    private TableLayout tableLayout;

    /**
     * Método que se ejecuta al crear la actividad.
     * Inicializa el layout, la base de datos y carga el historial.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_quiz_history); // Carga el diseño XML

            // Cambiar el título del historial
            TextView titleTextView = findViewById(R.id.historyTitle);
            if (titleTextView != null) {
                titleTextView.setText("Historial de Pronunciación");
            }

            dbHelper = new DatabaseHelper(this);
            tableLayout = findViewById(R.id.quizHistoryTable);

            if (tableLayout != null) {
                loadPronunciationHistory(); // Carga los datos en la tabla
            } else {
                Log.e(TAG, "TableLayout not found");
                Toast.makeText(this, "Error al cargar la tabla", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error al iniciar la actividad", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Carga el historial de quizzes desde la base de datos y lo muestra en una tabla.
     */
    private void loadPronunciationHistory() {
        try {
            long currentUserId = dbHelper.getCurrentUserId();

            if (currentUserId == -1) {
                Toast.makeText(this, "Error: No hay usuario autenticado", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Cursor cursor = dbHelper.getPronunciationHistory();

            if (cursor != null && cursor.moveToFirst()) {
                tableLayout.removeAllViews();

                // Crear encabezado
                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(getResources().getColor(R.color.header_blue));

                String[] headers = {"Date", "Texto Ref.", "Pronunciación", "Puntaje", "Topic", "Level"};
                for (String header : headers) {
                    TextView textView = new TextView(this);
                    textView.setText(header);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(16);
                    textView.setPadding(16, 12, 16, 12);
                    textView.setTypeface(null, Typeface.BOLD);
                    textView.setMinWidth(220);
                    textView.setMaxLines(1);
                    textView.setSingleLine(true);
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                    headerRow.addView(textView);
                }
                tableLayout.addView(headerRow);

                do {
                    long recordUserId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID));

                    if (recordUserId == currentUserId) {
                    TableRow dataRow = new TableRow(this);
                        dataRow.setBackgroundColor(cursor.getPosition() % 2 == 0 ? 
                            getResources().getColor(R.color.white) : 
                            getResources().getColor(R.color.light_gray));

                        long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(new Date(timestamp));
                        String referenceText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_REFERENCE_TEXT));
                        String spokenText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SPOKEN_TEXT));
                        double score = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_SCORE));
                        String topic = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TOPIC));
                        String level = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LEVEL));

                        String[] cellContents = {
                            date,
                            referenceText,
                            spokenText,
                            String.format("%.1f%%", score * 100),
                            topic,
                            level
                        };

                        for (int i = 0; i < cellContents.length; i++) {
                            TextView textView = new TextView(this);
                            textView.setText(cellContents[i]);
                            textView.setTextSize(14);
                            textView.setPadding(16, 12, 16, 12);
                            textView.setMinWidth(220);
                            textView.setMaxLines(3);
                            textView.setEllipsize(TextUtils.TruncateAt.END);
                            
                            // Aplicar color al puntaje
                            if (i == 3) { // La columna del puntaje
                                textView.setTextColor(score >= 0.7 ? Color.GREEN : 
                                                    score >= 0.5 ? Color.rgb(255, 165, 0) : // Naranja
                                                    Color.RED);
                                textView.setTextSize(16);
                                textView.setTypeface(null, Typeface.BOLD);
                            }
                            
                            dataRow.addView(textView);
                        }

                        tableLayout.addView(dataRow);
                    }
                } while (cursor.moveToNext());

                cursor.close();
            } else {
                Toast.makeText(this, "No hay historial de pronunciación disponible", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading pronunciation history: " + e.getMessage());
            Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Se llama cuando la actividad se destruye. Cierra la base de datos para liberar recursos.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
