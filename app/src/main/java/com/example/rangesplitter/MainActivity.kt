package com.example.rangesplitter

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        supportActionBar?.hide()
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

        // Set up Bottom Navigation active item color change
        setupBottomNavActiveColor()
    }

    // This function refreshes the current fragment by notifying ViewPager2 to reload
    private fun refreshCurrentFragment() {
        val currentFragment =
            supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? RefreshableFragment
        currentFragment?.refreshData()
    }


    // This function sets the active color of the BottomNavigationView items
    private fun setupBottomNavActiveColor() {
        val activeColor = ContextCompat.getColor(this, R.color.colorActive)  // Active color
        val inactiveColor = ContextCompat.getColor(this, R.color.colorInactive)  // Inactive color

        bottomNav.itemIconTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(activeColor, inactiveColor)
        )

        bottomNav.itemTextColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(activeColor, inactiveColor)
        )
    }
}
