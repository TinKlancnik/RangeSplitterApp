package com.example.rangesplitter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class StartActivity: AppCompatActivity (){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        supportActionBar?.hide()
        window.navigationBarColor = resources.getColor(android.R.color.black)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val username=findViewById<EditText>(R.id.usernameInput)
        val password=findViewById<EditText>(R.id.passwordInput)

        loginButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}