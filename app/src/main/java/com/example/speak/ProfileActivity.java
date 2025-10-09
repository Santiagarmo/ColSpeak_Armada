package com.example.speak;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.speak.database.DatabaseHelper;
import com.example.speak.helpers.TrophyHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import com.example.speak.MainActivity;

public class ProfileActivity extends AppCompatActivity {

    //Firebase validation
    FirebaseUser eUser;
    FirebaseAuth eAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private DatabaseHelper dbHelper;

    //End session
    Button eButtonEnd;
    Button eButtonModify;

    //Instantiate
    TextView eTextUid;
    TextView eTextName;
    TextView eTextEmail;
    TextView eTextDate;
    TextView eTextScore;
    
    // Contadores de trofeos y estrellas
    TextView trophyCount;
    TextView starCount;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView homeButton;

    private boolean isGuestUser = false;

    //Return Menú
    private LinearLayout eBtnReturnMenu;
    private SwitchCompat switchFreeRoam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Firebase
        eAuth = FirebaseAuth.getInstance();
        eUser = eAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("ColSpeak");

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        //TextView
        eTextUid = findViewById(R.id.textViewUid);
        eTextName = findViewById(R.id.textViewName);
        eTextEmail = findViewById(R.id.textViewEmail);
        eTextDate = findViewById(R.id.textViewDate);
        
        // Contadores de trofeos y estrellas
        trophyCount = findViewById(R.id.trophyCount);
        starCount = findViewById(R.id.starCount);
        
        // Click listener para la estrella (abrir vista de resultados)
        starCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, ResultsOverviewActivity.class);
                startActivity(intent);
            }
        });

        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        switchFreeRoam = findViewById(R.id.switchFreeRoam);

        //Log out
        eButtonEnd = findViewById(R.id.btnEndSession);
        eButtonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EndSession();
            }
        });

        //Change username
        eButtonModify = findViewById(R.id.btnModify);
        eButtonModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifyInfo();
            }
        });

        homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si es usuario invitado
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean isGuest = prefs.getBoolean("is_guest", false);

                if (isGuest) {
                    // Si es invitado, ir directamente al perfil
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    }
                }
            }
        });

        quizMenu = findViewById(R.id.imgQuizMenu);
        quizMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si es usuario invitado
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean isGuest = prefs.getBoolean("is_guest", false);

                if (isGuest) {
                    // Si es invitado, ir directamente al quiz history
                    Intent intent = new Intent(ProfileActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(ProfileActivity.this, QuizHistoryActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    }
                }
            }
        });

        pronunMenu = findViewById(R.id.imgPronunMenu);
        pronunMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si es usuario invitado
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean isGuest = prefs.getBoolean("is_guest", false);

                if (isGuest) {
                    // Si es invitado, ir directamente al historial
                    Intent intent = new Intent(ProfileActivity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(ProfileActivity.this, PronunciationHistoryActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    }
                }
            }
        });

        // Check if user is guest
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (dbHelper.isGuestUserExists(deviceId)) {
            isGuestUser = true;
            showGuestUserInfo(deviceId);
        } else {
            UserData();
        }
        
        // Actualizar contadores de trofeos y estrellas
        updateTrophyAndStarCounters();

        // Inicializar switch con valor guardado
        SharedPreferences prefs = getSharedPreferences("ProgressPrefs", MODE_PRIVATE);
        boolean freeRoam = prefs.getBoolean("FREE_ROAM", false);
        if (switchFreeRoam != null) {
            switchFreeRoam.setChecked(freeRoam);
            switchFreeRoam.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("FREE_ROAM", isChecked).apply();
                Toast.makeText(ProfileActivity.this, isChecked ? "Modo libre activado" : "Modo libre desactivado", Toast.LENGTH_SHORT).show();
            });
        }

        birdMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                birdExpanded = !birdExpanded;

                if (birdExpanded) {
                    birdMenu.setImageResource(R.drawable.bird1_menu);
                    fadeInView(quizMenu);
                    fadeInView(pronunMenu);
                } else {
                    birdMenu.setImageResource(R.drawable.bird0_menu);
                    fadeOutView(quizMenu);
                    fadeOutView(pronunMenu);
                }
            }
        });

        //Return Menu
        eBtnReturnMenu = findViewById(R.id.eBtnReturnMenu);
        eBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnMenu();
            }
        });
    }

    private void fadeInView(final View view) {
        view.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        view.startAnimation(fadeIn);
    }

    private void fadeOutView(final View view) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(fadeOut);
    }

    private void showGuestUserInfo(String deviceId) {
        Cursor cursor = dbHelper.getGuestUser(deviceId);
        if (cursor.moveToFirst()) {
            String email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL));
            String password = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));

            eTextEmail.setText("Email: " + email);
            eTextName.setText("Usuario: Invitado");
            eTextUid.setText("Contraseña: " + password);
            eTextDate.setText("Modo: Offline");

            // Ocultar botones que no son necesarios para usuarios invitados
            eButtonModify.setVisibility(View.GONE);
        }
        cursor.close();
    }

    private void UserData() {
        if (isGuestUser) {
            return;
        }

        if (eUser != null) {
            String uid = eUser.getUid();
            String email = eUser.getEmail();

            eTextUid.setText("UID: " + uid);
            eTextEmail.setText("Email: " + email);

            databaseReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String date = dataSnapshot.child("date").getValue(String.class);

                        eTextName.setText("Nombre: " + name);
                        eTextDate.setText("Fecha de registro: " + date);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //We modify the user data
    private void modifyInfo() {
        String[] options = {"Name"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (i == 0) {
                    ModifyName("Name");
                }
            }
        });
        builder.create().show();
    }

    private void ModifyName(final String key){
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Modificar el "+key);
        LinearLayoutCompat linearLayoutCompat = new LinearLayoutCompat(ProfileActivity.this);
        linearLayoutCompat.setOrientation(LinearLayoutCompat.VERTICAL);
        linearLayoutCompat.setPadding(5,5,5,5);
        final EditText editText = new EditText(ProfileActivity.this);
        editText.setHint("Actualiza Aquí" +key);
        linearLayoutCompat.addView(editText);
        builder.setView(linearLayoutCompat);
        //This will be executed if the user clicks on modify information.
        builder.setPositiveButton("Modificar información", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = editText.getText().toString().trim();
                HashMap<String,Object> result = new HashMap<>();
                result.put(key, value);
                databaseReference.child(eAuth.getUid()).updateChildren(result).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ProfileActivity.this, "Actualización correcta", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        //Cancel button
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(ProfileActivity.this, "Cancelación Satisfactoria", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create().show();
    }

    //We close the user session
    private void EndSession() {
        eAuth.signOut();
        startActivity(new Intent(ProfileActivity.this, MajorActivity.class));
        Toast.makeText(ProfileActivity.this, "Has finalizado sesión correctamente.", Toast.LENGTH_SHORT).show();
    }

    // Method to be executed when our game opens
    @Override
    protected void onStart() {
        LoggedUser();
        super.onStart();
    }

    // Method to check if a user is logged in
    private void LoggedUser() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);
        long userId = prefs.getLong("user_id", -1);

        if (isGuest && userId != -1) {
            // Usuario invitado, permitir acceso
            Toast.makeText(ProfileActivity.this, "Modo Invitado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si no es invitado, verificar autenticación normal
        prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        boolean isOffline = prefs.getBoolean("is_offline", false);
        String userEmail = prefs.getString("user_email", null);

        if (isLoggedIn) {
            if (isOffline && userEmail != null) {
                // Verificar si la sesión offline sigue siendo válida
                if (dbHelper.checkOfflineSession(userEmail)) {
                    Toast.makeText(ProfileActivity.this, "Modo Offline", Toast.LENGTH_SHORT).show();
                } else {
                    // La sesión offline ha expirado
                    clearLoginState();
                    startActivity(new Intent(ProfileActivity.this, RegisterActivity.class));
                    finish();
                }
            } else if (eUser != null) {
                Toast.makeText(ProfileActivity.this, "En Línea", Toast.LENGTH_SHORT).show();
            }
        } else {
            startActivity(new Intent(ProfileActivity.this, RegisterActivity.class));
            finish();
        }
    }

    private void clearLoginState() {
        // Limpiar Firebase Auth
        eAuth.signOut();

        // Limpiar SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SpeakApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.remove("user_email");
        editor.remove("is_logged_in");
        editor.remove("is_offline");
        editor.apply();

        // No limpiar los datos del usuario invitado
        // Los datos en "user_prefs" se mantienen para el modo invitado
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    //Return Menú
    private void ReturnMenu() {
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        Toast.makeText(ProfileActivity.this, "Has retornado al menú correctamente.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Actualiza los contadores de trofeos y estrellas
     */
    private void updateTrophyAndStarCounters() {
        if (trophyCount != null && starCount != null) {
            // Limpiar trofeos incorrectos primero
            TrophyHelper.cleanIncorrectTrophies(this);
            
            // Trofeos: combinación de progreso por temas (histórico) + por estrellas
            int trophiesByTopics = TrophyHelper.getTrophyCount(this);
            int trophiesByStars = com.example.speak.helpers.StarProgressHelper.getTrophyCountFromStars(this);
            int trophies = Math.max(trophiesByTopics, trophiesByStars);

            // Estrellas basadas en puntos acumulados (10 puntos = 1 estrella)
            int stars = com.example.speak.helpers.StarProgressHelper.getStarCount(this);
            
            trophyCount.setText(String.valueOf(trophies));
            starCount.setText(String.valueOf(stars));
            
            Log.d("ProfileActivity", "Contadores actualizados - Trofeos: " + trophies + ", Estrellas: " + stars);
        }
    }

}
