package com.example.rangesplitter.UI

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.rangesplitter.MainPageAdapter // Import your custom adapter

object NavigationHelper {

    fun setupBottomNav(
        fragmentActivity: FragmentActivity,
        bottomNav: BottomNavigationView,
        viewPager: ViewPager2
    ) {
        val adapter = MainPageAdapter(
            fragmentActivity.supportFragmentManager,
            fragmentActivity.lifecycle
        )
        viewPager.adapter = adapter

        // keep pages alive (optional but helps)
        viewPager.offscreenPageLimit = 3

        bottomNav.setOnItemSelectedListener { item ->

            // Close Split overlay if open
            fragmentActivity.supportFragmentManager.findFragmentByTag("SPLIT")?.let { split ->
                fragmentActivity.supportFragmentManager.beginTransaction()
                    .remove(split)
                    .commitNow() // IMPORTANT: remove immediately to avoid 1-frame weirdness

                fragmentActivity.findViewById<View>(R.id.fragmentContainer).visibility = View.GONE
            }

            val position = when (item.itemId) {
                R.id.nav_home  -> 0
                R.id.nav_trade -> 1
                R.id.nav_chart -> 2
                else -> 0
            }

            viewPager.setCurrentItem(position, false) // IMPORTANT: no smooth scroll
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_trade
                    2 -> R.id.nav_chart
                    else -> R.id.nav_home
                }
                if (bottomNav.selectedItemId != itemId) {
                    bottomNav.selectedItemId = itemId
                }

                // disable swipe only on chart if you want
                viewPager.isUserInputEnabled = position != 2
            }
        })
    }
}


