package com.example.rangesplitter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.UI.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var balanceTextView: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceTextView = view.findViewById(R.id.balanceTextView)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        TradeUtils.fetchBalance { balance ->
            balanceTextView.text = "$balance USD"
        }

        val adapter = ViewPagerAdapter(this)   // ⬅️ changed from requireActivity()
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orders"
                1 -> "Positions"
                else -> ""
            }
        }.attach()

        // Pull-to-refresh behavior
        swipeRefreshLayout.setOnRefreshListener {
            val currentItem = viewPager.currentItem
            val tag = "f$currentItem"  // default tag pattern used by FragmentStateAdapter

            val currentFragment = childFragmentManager.findFragmentByTag(tag)

            if (currentFragment is RefreshableFragment) {
                currentFragment.refreshData()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }
}
