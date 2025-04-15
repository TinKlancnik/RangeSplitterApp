package com.example.rangesplitter.UI

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.rangesplitter.MenuFragment
import com.example.rangesplitter.R
import com.example.rangesplitter.SplitFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationHelper {

    fun setupBottomNav(fragmentManager: FragmentManager, bottomNav: BottomNavigationView) {
        // Load default fragment
        loadFragment(fragmentManager, MenuFragment())

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> MenuFragment()
                R.id.nav_search -> SplitFragment()
                R.id.nav_profile -> MenuFragment()
                else -> MenuFragment()
            }

            loadFragment(fragmentManager, fragment)
            true
        }
    }

    private fun loadFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
