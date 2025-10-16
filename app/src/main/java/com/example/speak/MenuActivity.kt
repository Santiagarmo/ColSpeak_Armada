package com.example.speak

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.speak.pronunciation.TopicListActivity

class MenuActivity : AppCompatActivity() {
    private var btnListening: ImageButton? = null
    private var btnPronunciation: ImageButton? = null
    private var btnWriting: ImageButton? = null
    private var btnProfile: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnListening = findViewById<ImageButton>(R.id.btnListening)
        btnPronunciation = findViewById<ImageButton>(R.id.btnPronunciation)
        btnWriting = findViewById<ImageButton>(R.id.btnWriting)
        btnProfile = findViewById<ImageButton>(R.id.btnProfile)
    }

    private fun setupClickListeners() {
        btnListening!!.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@MenuActivity, ListeningActivity::class.java)
            startActivity(intent)
        })

        btnPronunciation!!.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@MenuActivity, TopicListActivity::class.java)
            startActivity(intent)
        })

        btnWriting!!.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@MenuActivity, WritingActivity::class.java)
            startActivity(intent)
        })

        btnProfile!!.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@MenuActivity, ProfileActivity::class.java)
            startActivity(intent)
        })
    }
}