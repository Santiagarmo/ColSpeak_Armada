package com.example.speak;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //We declare the variables
    //Record
    EditText eTextName, eTextEmail, eTextPassword;
    Button eButtonRegister;
    TextView eTextDate, eButtonLogin;
    //Record

    //Firebase
    FirebaseAuth eAuth;
    FirebaseDatabase eDatase;

    ProgressDialog eProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Instantiate
        //Record
        eTextName = findViewById(R.id.textName);
        eTextEmail = findViewById(R.id.textEmail);
        eTextPassword = findViewById(R.id.textPassword);
        eButtonRegister = findViewById(R.id.btnRegister);
        eButtonLogin = findViewById(R.id.btnLogin);
        eTextDate = findViewById(R.id.textDate);
        //Record

        eAuth = FirebaseAuth.getInstance();
        eDatase = FirebaseDatabase.getInstance();

        Date eDate = new Date();
        SimpleDateFormat eDat = new SimpleDateFormat("d ' de 'MMMM' del 'yyyy");
        final String StringDate = eDat.format(eDate);
        //We get the date
        eTextDate.setText(StringDate);

        //Click Event Listener to the button
        //In the button we are going to pass the string
        eButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = eTextName.getText().toString();
                String email = eTextEmail.getText().toString();
                String password = eTextPassword.getText().toString();
                String date = eTextDate.getText().toString();

                //We will validate the email Patterns
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    //If the email is entered incorrectly
                    eTextEmail.setError("Email Inválido");
                    eTextEmail.setFocusable(true);

                    //Validate password
                } else if (eTextPassword.length()>6) {
                    eTextPassword.setError("Por favor, introduce una contraseña de al menos 6 caracteres");
                    eTextPassword.setFocusable(true);
                } else {
                    clickRegister();
                }

                //We added a toast to watch it work well
                //Toast.makeText( LoginActivity.this, "Boton Registrar", Toast.LENGTH_SHORT).show();
            }
        });

        eButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                 startActivity(intent);
                 //finish();
            }
        });

        eProgress = new ProgressDialog( RegisterActivity.this);
        eProgress.setMessage("Procesando datos...");
        eProgress.setCancelable(false);
    }

    private void clickRegister() {
        final String name = eTextName.getText().toString();
        final String email = eTextEmail.getText().toString();
        final String password = eTextPassword.getText().toString();

        if(!name.isEmpty() && !email.isEmpty() && !password.isEmpty()){
            if (password.length() >= 6) {
                RegisterPerson(name, email, password);
            } else {
                Toast.makeText(RegisterActivity.this, "La longitud mínima de la contraseña es de 6 caracteres.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(RegisterActivity.this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void RegisterPerson(String name, String email, String password) {
        eProgress.show();
        //We already have the listeners and it is registered as such
        eAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //Let's validate if it was done correctly, where the eProgress dialog will be hidden ("Registering please wait")
                        if (task.isSuccessful()) {
                            eProgress.dismiss();
                            //Let's say that FirebaseUser eUser, validate it as a recurring user
                            FirebaseUser eUser = eAuth.getCurrentUser();
                            //We add a counter and leave it at 0, it is to leave an error if the user cannot access
                            int counter = 0;
                            //Error if user is not null
                            assert eUser != null;
                            //We pass the string
                            String uidString = eUser.getUid();
                            String nam = eTextName.getText().toString();
                            String emai = eTextEmail.getText().toString();
                            String passwor = eTextPassword.getText().toString();
                            String registrationDate = eTextDate.getText().toString();

                            //Make an object with personal data
                            HashMap<Object, Object> UserData = new HashMap<>();
                            UserData.put("Uid", uidString);
                            UserData.put("Name", nam);
                            UserData.put("Email", emai);
                            UserData.put("Password", passwor);
                            UserData.put("RegistrationDate", registrationDate);

                            //Let's call firebase
                            FirebaseDatabase eDatabase = FirebaseDatabase.getInstance();
                            //We create the database ColSpeak
                            DatabaseReference eReference = eDatabase.getReference("ColSpeak");
                            eReference.child((uidString)).setValue(UserData);
                            //We'll make it so that when you register it will make an attempt and send it to the menu
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            Toast.makeText(RegisterActivity.this, "Registro completado con éxito.", Toast.LENGTH_SHORT).show();
                            finish();

                        } else {
                            eProgress.dismiss();
                            Toast.makeText(RegisterActivity.this, "El registro no se ha podido completar.", Toast.LENGTH_SHORT).show();

                        }
                    }
                })

                //In case user registration fails, to get the error
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}