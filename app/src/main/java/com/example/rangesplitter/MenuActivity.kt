package com.example.rangesplitter

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MenuActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)


        val mainButton = findViewById<Button>(R.id.mainButton)

        mainButton.setOnClickListener {

        }
    }
}