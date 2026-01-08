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

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var balanceTextView: TextView
    private lateinit var totalValueChange: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var name: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceTextView = view.findViewById(R.id.balanceTextView)
        totalValueChange = view.findViewById(R.id.totalValueChange)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        name = view.findViewById(R.id.userName)

        name.setOnClickListener {
            (requireActivity() as MainActivity).openSettings()
        }

        TradeUtils.fetchBalance { balance ->
            balanceTextView.text = "$balance USD"
        }

        TradeUtils.fetchTotalValueChange { formatted, value ->
            totalValueChange.text = "$formatted USD"

            val colorRes = if (value >= 0)
                R.color.vibrant_green
            else
                R.color.vibrant_red

            totalValueChange.setTextColor(requireContext().getColor(colorRes))
        }

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orders"
                1 -> "Positions"
                else -> ""
            }
        }.attach()

        swipeRefreshLayout.setOnRefreshListener {
            val currentItem = viewPager.currentItem
            val tag = "f$currentItem"

            val currentFragment = childFragmentManager.findFragmentByTag(tag)

            if (currentFragment is RefreshableFragment) {
                currentFragment.refreshData()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }
}
