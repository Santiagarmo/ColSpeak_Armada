package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Level_A1 : AppCompatActivity() {
    //We declare the variables of the functionality
    var score: Int = 0
    var resul: Int = 0
    var opportunities: Int = 5
    var nameUser: String? = null
    var string_score: String? = null
    var string_opportunities: String? = null

    //Firebase validation
    var eUser: FirebaseUser? = null
    var eAuth: FirebaseAuth? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null

    //I declare the xml variables
    var imageLife: ImageView? = null
    var namePerson: TextView? = null
    var percentage: TextView? = null
    var ask: TextView? = null
    var answer: EditText? = null
    var buttonAnswer: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_level_a1)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        //Firebase
        eAuth = FirebaseAuth.getInstance()
        eUser = eAuth!!.getCurrentUser()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference("ColSpeak")

        //We instantiate xml
        imageLife = findViewById<ImageView>(R.id.imageLife)
        namePerson = findViewById<TextView>(R.id.textViewPerson)
        percentage = findViewById<TextView>(R.id.textViewPercentage)
        ask = findViewById<TextView>(R.id.textViewAsk)
        answer = findViewById<EditText?>(R.id.editTextToAnswer)
        buttonAnswer = findViewById<Button?>(R.id.btnToAnswer)

        //To finish testing everything with a toast
        /*buttonAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Level_A1.this, "Activo", Toast.LENGTH_SHORT).show();
            }
        });*/
        personData()
    }

    //We created the method to obtain the person's data
    private fun personData() {
        //Validation that all the data in the registration fields are present
        if (eUser != null) {
            //Querying the database email

            val query = databaseReference!!.orderByChild("Email").equalTo(eUser!!.getEmail())
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //I will obtain all the user information as requested in the email.
                    for (ds in dataSnapshot.getChildren()) {
                        val uidUser = "id: " + ds.child("Uid").getValue()
                        val uidName = "" + ds.child("Name").getValue()
                        val uidEmail = "" + ds.child("Email").getValue()
                        val uidDate = "" + ds.child("RegistrationDate").getValue()

                        //We pass the data
                        //eTextUid.setText(uidUser);
                        namePerson!!.setText(uidName)
                        //eTextEmail.setText(uidEmail);
                        //eTextDate.setText(uidDate);
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        } else {
            Toast.makeText(this@Level_A1, "", Toast.LENGTH_SHORT).show()
        }
    }

    //logic, comparamos la respuesta que esta colocando el usuario
    fun relate() {
        val result = ask!!.getText().toString()

        if (result != "") {
            val request_person = result.toInt()
            if (resul == request_person) {
                score++
                percentage!!.setText("" + score)
                ask!!.setText("")
                DB()
            } else {
                //logic, we compare the answer that the user is placing
                opportunities--
                DB()
                when (opportunities) {
                    5 -> {
                        Toast.makeText(this@Level_A1, "5 OPPORTUNITIES", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                    }

                    4 -> {
                        Toast.makeText(this@Level_A1, "4 OPPORTUNITIES", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                    }

                    3 -> {
                        Toast.makeText(this@Level_A1, "3 OPPORTUNITIES", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                    }

                    2 -> {
                        Toast.makeText(this@Level_A1, "2 OPPORTUNITIES", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                    }

                    1 -> {
                        Toast.makeText(this@Level_A1, "1 OPPORTUNITIES", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                    }

                    0 -> {
                        Toast.makeText(this@Level_A1, "GAME OVER", Toast.LENGTH_SHORT).show()
                        imageLife!!.setImageResource(R.drawable.mushroom_7010764_640)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                ask!!.setText("")
            }
            TextRandom()
        } else {
            Toast.makeText(this@Level_A1, "Respuesta Obligatoria", Toast.LENGTH_SHORT).show()
        }
    }

    private fun TextRandom() {
    }

    private fun DB() {
    }
}