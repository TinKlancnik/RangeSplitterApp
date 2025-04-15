package com.example.rangesplitter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.UI.NavigationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the bottom navigation view and ViewPager2
        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Set up the bottom navigation and ViewPager2
        NavigationHelper.setupBottomNav(this, bottomNav, viewPager)

        // Explicitly set the initial fragment if needed
        if (savedInstanceState == null) {
            viewPager.currentItem = 0 // Set initial fragment (MenuFragment)
        }
    }
}
