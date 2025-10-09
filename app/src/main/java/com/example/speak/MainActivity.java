package com.example.speak;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.widget.Button;

import org.checkerframework.common.value.qual.StringVal;

import java.util.HashMap;

import androidx.cardview.widget.CardView;

import com.example.speak.pronunciation.PronunciationActivity;
import com.example.speak.quiz.QuizActivity;
import com.example.speak.database.DatabaseHelper;

import android.content.SharedPreferences;
import android.provider.Settings;
import android.database.Cursor;

//Animacion
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import android.widget.ImageView;

import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    //Firebase validation
    FirebaseUser eUser;
    FirebaseAuth eAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private DatabaseHelper dbHelper;

    //Instantiate
    //TextView eTextUid;
    //TextView eTextName;
    //TextView eTextEmail;
    //TextView eTextDate;
    TextView eTextScore;

    private ImageView pronunciationCard;
    private ImageView quizCard;
    private ImageView textToSpeechCard;

    private boolean isGuestUser = false;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private SliderAdapter sliderAdapter;

    private boolean birdExpanded = false;
    private ImageView birdMenu;
    private ImageView quizMenu;
    private ImageView pronunMenu;

    private ImageView eButtonProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
        //eTextUid = findViewById(R.id.textViewUid);
        //eTextName = findViewById(R.id.textViewName);
        //eTextEmail = findViewById(R.id.textViewEmail);
        //eTextDate = findViewById(R.id.textViewDate);

        // Inicializar ViewPager2 y TabLayout
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        
        sliderAdapter = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);

        birdMenu = findViewById(R.id.imgBirdMenu);
        quizMenu = findViewById(R.id.imgQuizMenu);
        pronunMenu = findViewById(R.id.imgPronunMenu);
        
        // Conectar TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                // Opcional: personalizar las tabs si lo deseas
            }
        ).attach();

        // Eliminar las inicializaciones antiguas de las tarjetas
        // pronunciationCard = findViewById(R.id.pronunciationCard);
        // quizCard = findViewById(R.id.quizCard);
        // textToSpeechCard = findViewById(R.id.textToSpeechCard);
        // setupClickListeners();

        eButtonProfile = findViewById(R.id.btnProfile);
        eButtonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si es usuario invitado
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean isGuest = prefs.getBoolean("is_guest", false);
                
                if (isGuest) {
                    // Si es invitado, ir directamente al perfil
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
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
                    Intent intent = new Intent(MainActivity.this, QuizHistoryActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(MainActivity.this, QuizHistoryActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
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
                    Intent intent = new Intent(MainActivity.this, PronunciationHistoryActivity.class);
                    startActivity(intent);
                } else {
                    // Si no es invitado, verificar autenticación
                    if (eUser != null) {
                        Intent intent = new Intent(MainActivity.this, PronunciationHistoryActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Por favor inicia sesión", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                }
            }
        });

        // Lock the "Back" button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Leave empty to lock the back button
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

        // Redirigir automáticamente al último módulo si existe (solo en primer arranque)
        String lastModule = ModuleTracker.getLastModule(this);
        if (lastModule != null) {
            Intent redirectIntent = null;
            switch (lastModule) {
                case "listening":
                    redirectIntent = new Intent(this, MenuA1Activity.class); break;
                case "speaking":
                    redirectIntent = new Intent(this, MenuSpeakingActivity.class); break;
                case "reading":
                    redirectIntent = new Intent(this, MenuReadingActivity.class); break;
                case "writing":
                    redirectIntent = new Intent(this, MenuWritingActivity.class); break;
                case "word_order":
                    redirectIntent = new Intent(this, WordOrderActivity.class); break;
            }
            if (redirectIntent != null) {
                startActivity(redirectIntent);
                finish();
                return;
            }
        }
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

            //eTextEmail.setText("Email: " + email);
            //eTextName.setText("Usuario: Invitado");
            //eTextUid.setText("Contraseña: " + password);
            //eTextDate.setText("Modo: Offline");

            // Ocultar botones que no son necesarios para usuarios invitados
            //eButtonModify.setVisibility(View.GONE);
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

            //eTextUid.setText("UID: " + uid);
            //eTextEmail.setText("Email: " + email);

            databaseReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String date = dataSnapshot.child("date").getValue(String.class);

                        //eTextName.setText("Nombre: " + name);
                        //eTextDate.setText("Fecha de registro: " + date);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            Toast.makeText(MainActivity.this, "Modo Invitado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si no hay usuario invitado, crear uno nuevo
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId != null) {
            // Verificar si existe un usuario invitado para este dispositivo
            if (dbHelper.isGuestUserExists(deviceId)) {
                // Obtener el ID del usuario invitado existente
                userId = dbHelper.getGuestUserId(deviceId);
                if (userId != -1) {
                    // Guardar la información del usuario invitado
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_guest", true);
                    editor.putLong("user_id", userId);
                    editor.apply();
                    Toast.makeText(MainActivity.this, "Modo Invitado", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // Crear nuevo usuario invitado
                userId = dbHelper.createGuestUser(deviceId);
                if (userId != -1) {
                    // Guardar la información del nuevo usuario invitado
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_guest", true);
                    editor.putLong("user_id", userId);
                    editor.apply();
                    Toast.makeText(MainActivity.this, "Modo Invitado", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
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
                    Toast.makeText(MainActivity.this, "Modo Offline", Toast.LENGTH_SHORT).show();
                } else {
                    // La sesión offline ha expirado
                    clearLoginState();
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                    finish();
                }
            } else if (eUser != null) {
                Toast.makeText(MainActivity.this, "En Línea", Toast.LENGTH_SHORT).show();
            }
        } else {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
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

}