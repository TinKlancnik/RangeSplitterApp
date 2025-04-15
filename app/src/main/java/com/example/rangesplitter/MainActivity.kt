package com.example.rangesplitter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.rangesplitter.UI.NavigationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the bottom navigation view, ViewPager2, and SwipeRefreshLayout
        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Set up the bottom navigation and ViewPager2
        NavigationHelper.setupBottomNav(this, bottomNav, viewPager)

        // Explicitly set the initial fragment if needed
        if (savedInstanceState == null) {
            viewPager.currentItem = 0 // Set initial fragment (MenuFragment)
        }

        // Set up swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener {
            // Call the method to refresh the current fragment
            refreshCurrentFragment()

            // Stop the refreshing animation once done
            swipeRefreshLayout.isRefreshing = false
        }
    }

    // This function refreshes the current fragment by notifying ViewPager2 to reload
    private fun refreshCurrentFragment() {
        // Get the current position of the ViewPager
        val currentPosition = viewPager.currentItem

        // Notify the ViewPager2 to reload the current fragment
        viewPager.adapter?.notifyItemChanged(currentPosition)
    }
}
