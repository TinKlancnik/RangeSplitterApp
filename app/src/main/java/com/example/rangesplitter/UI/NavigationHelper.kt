package com.example.rangesplitter.UI

import android.app.Activity
import android.content.Intent
import com.example.rangesplitter.MenuActivity
import com.example.rangesplitter.R
import com.example.rangesplitter.SplitActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationHelper {
    fun setupBottomNav(activity: Activity, bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (activity !is MenuActivity)
                        activity.startActivity(Intent(activity, MenuActivity::class.java))
                    true
                }
                R.id.nav_search -> {
                    if (activity !is SplitActivity)
                        activity.startActivity(Intent(activity, SplitActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    if (activity !is SplitActivity)
                        activity.startActivity(Intent(activity, SplitActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
