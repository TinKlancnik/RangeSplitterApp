package com.example.rangesplitter

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.example.rangesplitter.UI.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var balanceTextView: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceTextView = view.findViewById(R.id.balanceTextView)
        val mainButton = view.findViewById<Button>(R.id.splitButton)

        // FIXED: use view.findViewById
        val coinListButton = view.findViewById<Button>(R.id.trade)

        // FIXED: open CoinSelectFragment using FragmentTransaction
        coinListButton.setOnClickListener {
            (requireActivity() as MainActivity).openCoinSelect()
        }

        TradeUtils.fetchBalance { balance ->
            balanceTextView.text = "$balance USD"
        }

        // Initialize ViewPager + Tabs
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orders"
                1 -> "Positions"
                else -> ""
            }
        }.attach()

        // Main button click
        mainButton.setOnClickListener {
            viewPager.currentItem = 2 // adjust if needed
        }
    }
}

