package com.example.rangesplitter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rangesplitter.UI.NavigationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the bottom navigation view
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set up the bottom navigation
        NavigationHelper.setupBottomNav(supportFragmentManager, bottomNav)

        // Explicitly set the initial fragment if needed
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }
}
