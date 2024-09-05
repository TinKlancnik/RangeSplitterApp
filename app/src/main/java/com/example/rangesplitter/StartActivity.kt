package com.example.rangesplitter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StartActivity: AppCompatActivity (){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        // Hide the action bar
        supportActionBar?.hide()

        // Make the navigation bar black
        window.navigationBarColor = resources.getColor(android.R.color.black)
    }
}