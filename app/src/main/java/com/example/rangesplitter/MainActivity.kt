package com.example.rangesplitter

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.journal.EditJournalTradeFragment
import com.example.rangesplitter.ui.NavigationHelper
import com.example.rangesplitter.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager2
        private set

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        NavigationHelper.setupBottomNav(this, bottomNav, viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                swipeRefreshLayout.isEnabled = position != 2
            }
        })

        if (savedInstanceState == null) viewPager.currentItem = 0

        swipeRefreshLayout.setOnRefreshListener {
            refreshCurrentFragment()
            swipeRefreshLayout.isRefreshing = false
        }

        setupBottomNavActiveColor()

        bottomNav.isItemActiveIndicatorEnabled = false

        onBackPressedDispatcher.addCallback(this) {
            if (isOverlayOpen()) closeOverlay()
            else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    fun openSplitForSymbol(symbol: String) {
        openOverlay(SplitFragment.newInstance(symbol), "SPLIT")
    }

    fun openSettings() {
        openOverlay(SettingsFragment(), "SETTINGS")
    }
    fun openEditJournalTrade(tradeId: String) {
        openOverlay(EditJournalTradeFragment.newInstance(tradeId), "EDIT_TRADE")
    }
    private fun openOverlay(fragment: androidx.fragment.app.Fragment, tag: String) {
        findViewById<View>(R.id.fragmentContainer).apply {
            visibility = View.VISIBLE
            bringToFront()
        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun isOverlayOpen(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null
    }

    fun closeOverlay() {
        supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        findViewById<View>(R.id.fragmentContainer).visibility = View.GONE
    }

    private fun refreshCurrentFragment() {
        val currentFragment =
            supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? RefreshableFragment
        currentFragment?.refreshData()
    }

    private fun setupBottomNavActiveColor() {
        val activeColor = ContextCompat.getColor(this, R.color.colorActive)
        val inactiveColor = ContextCompat.getColor(this, R.color.colorInactive)

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        bottomNav.itemIconTintList = ColorStateList(states, intArrayOf(activeColor, inactiveColor))
        bottomNav.itemTextColor = ColorStateList(states, intArrayOf(activeColor, inactiveColor))
    }
}
