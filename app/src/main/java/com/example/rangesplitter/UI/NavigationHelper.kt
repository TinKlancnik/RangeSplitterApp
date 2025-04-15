package com.example.rangesplitter.UI

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.MenuFragment
import com.example.rangesplitter.R
import com.example.rangesplitter.SplitFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.rangesplitter.MainPageAdapter // Import your custom adapter

object NavigationHelper {

    fun setupBottomNav(
        fragmentActivity: FragmentActivity,
        bottomNav: BottomNavigationView,
        viewPager: ViewPager2
    ) {
        // Set up ViewPager2 adapter with your custom adapter
        val adapter = MainPageAdapter(fragmentActivity.supportFragmentManager, fragmentActivity.lifecycle)
        viewPager.adapter = adapter

        // Set up BottomNavigationView to handle fragment switching
        bottomNav.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_home -> 0 // MenuFragment
                R.id.nav_search -> 1 // SplitFragment
                R.id.nav_profile -> 0 // MenuFragment (or another fragment if needed)
                else -> 0 // Default to MenuFragment
            }
            viewPager.currentItem = position
            true
        }

        // Optional: Sync the ViewPager2 swipe with the BottomNavigationView
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_search
                    else -> R.id.nav_home
                }
                bottomNav.selectedItemId = itemId
            }
        })
    }
}
