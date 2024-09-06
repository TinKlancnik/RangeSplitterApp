package com.example.rangesplitter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity: AppCompatActivity (){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        supportActionBar?.hide()
        window.navigationBarColor = resources.getColor(android.R.color.black)

        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener{
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }
}