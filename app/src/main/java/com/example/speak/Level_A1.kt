package com.example.speak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Level_A1 extends AppCompatActivity {

    //We declare the variables of the functionality
    int score, resul, opportunities = 5;
    String nameUser;
    String string_score;
    String string_opportunities;

    //Firebase validation
    FirebaseUser eUser;
    FirebaseAuth eAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //I declare the xml variables
    ImageView imageLife;
    TextView namePerson;
    TextView percentage;
    TextView ask;
    EditText answer;
    Button buttonAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_a1);
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

        //We instantiate xml
        imageLife = findViewById(R.id.imageLife);
        namePerson = findViewById(R.id.textViewPerson);
        percentage = findViewById(R.id.textViewPercentage);
        ask = findViewById(R.id.textViewAsk);
        answer = findViewById(R.id.editTextToAnswer);
        buttonAnswer = findViewById(R.id.btnToAnswer);

        //To finish testing everything with a toast
        /*buttonAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Level_A1.this, "Activo", Toast.LENGTH_SHORT).show();
            }
        });*/
        personData();
    }

    //We created the method to obtain the person's data
    private void personData(){
        //Validation that all the data in the registration fields are present
        if (eUser != null) {

            //Querying the database email
            Query query = databaseReference.orderByChild("Email").equalTo(eUser.getEmail());
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //I will obtain all the user information as requested in the email.
                    for (DataSnapshot ds : dataSnapshot.getChildren()){
                        String uidUser = "id: "+ds.child("Uid").getValue();
                        String uidName = ""+ds.child("Name").getValue();
                        String uidEmail = ""+ds.child("Email").getValue();
                        String uidDate = ""+ds.child("RegistrationDate").getValue();

                        //We pass the data
                        //eTextUid.setText(uidUser);
                        namePerson.setText(uidName);
                        //eTextEmail.setText(uidEmail);
                        //eTextDate.setText(uidDate);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        } else {
            Toast.makeText(Level_A1.this,"", Toast.LENGTH_SHORT).show();

        }
    }

    //logic, comparamos la respuesta que esta colocando el usuario
    public void relate() {
        String result = ask.getText().toString();

        if (!result.equals("")) {
            int request_person = Integer.parseInt(result);
            if (resul == request_person) {
                score++;
                percentage.setText("" + score);
                ask.setText("");
                DB();
            } else {
                //logic, we compare the answer that the user is placing
                opportunities--;
                DB();
                switch (opportunities){
                    case 5:
                        Toast.makeText(Level_A1.this, "5 OPPORTUNITIES", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        break;
                    case 4:
                        Toast.makeText(Level_A1.this, "4 OPPORTUNITIES", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        break;
                    case 3:
                        Toast.makeText(Level_A1.this, "3 OPPORTUNITIES", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        break;
                    case 2:
                        Toast.makeText(Level_A1.this, "2 OPPORTUNITIES", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        break;
                    case 1:
                        Toast.makeText(Level_A1.this, "1 OPPORTUNITIES", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        break;
                    case 0:
                        Toast.makeText(Level_A1.this, "GAME OVER", Toast.LENGTH_SHORT).show();
                        imageLife.setImageResource(R.drawable.mushroom_7010764_640);
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                }
                ask.setText("");
            }
            TextRandom();
        } else {
            Toast.makeText(Level_A1.this, "Respuesta Obligatoria", Toast.LENGTH_SHORT).show();
        }
    }

    private void TextRandom() {

    }

    private void DB() {

    }
}