package com.example.rangesplitter.ui

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.MainActivity
import com.example.rangesplitter.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.rangesplitter.MainPageAdapter // Import your custom adapter

object NavigationHelper {

    fun setupBottomNav(
        fragmentActivity: FragmentActivity,
        bottomNav: BottomNavigationView,
        viewPager: ViewPager2
    ) {
        val adapter =
            MainPageAdapter(fragmentActivity.supportFragmentManager, fragmentActivity.lifecycle)
        viewPager.adapter = adapter

        bottomNav.setOnItemSelectedListener { item ->

            (fragmentActivity as? MainActivity)?.closeOverlay()

            val position = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_trade -> 3
                R.id.nav_chart -> 2
                R.id.nav_journal -> 4
                else -> 0
            }
            viewPager.setCurrentItem(position, false)
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val itemId = when (position) {
                    0 -> R.id.nav_home
                    3 -> R.id.nav_trade
                    2 -> R.id.nav_chart
                    4 -> R.id.nav_journal
                    else -> R.id.nav_home
                }
                bottomNav.selectedItemId = itemId
                viewPager.isUserInputEnabled = position != 2
            }
        })
    }
}

