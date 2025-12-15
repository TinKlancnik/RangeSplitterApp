package com.example.rangesplitter

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.UI.NavigationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager2
        private set

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        private const val ARG_SYMBOL = "symbol"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // Views
        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Set up ViewPager + BottomNav
        NavigationHelper.setupBottomNav(this, bottomNav, viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                swipeRefreshLayout.isEnabled = position != 2
            }
        })

        if (savedInstanceState == null) {
            viewPager.currentItem = 0 // MenuFragment
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshCurrentFragment()
            swipeRefreshLayout.isRefreshing = false
        }

        setupBottomNavActiveColor()

        bottomNav.itemBackground = null
        bottomNav.itemRippleColor = null

        bottomNav.isItemActiveIndicatorEnabled = false

    }

    fun openSplitForSymbol(symbol: String) {

        // show overlay container
        findViewById<View>(R.id.fragmentContainer).visibility = View.VISIBLE

        val fragment = SplitFragment.newInstance(symbol)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, "SPLIT")
            .commit()
    }

    private fun refreshCurrentFragment() {
        val currentFragment =
            supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? RefreshableFragment
        currentFragment?.refreshData()
    }

    private fun setupBottomNavActiveColor() {
        val activeColor = ContextCompat.getColor(this, R.color.colorActive)
        val inactiveColor = ContextCompat.getColor(this, R.color.colorInactive)

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
