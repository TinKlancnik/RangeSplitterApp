package com.example.rangesplitter

import com.example.rangesplitter.TradeUtils
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.shared.AccountType
import com.example.rangesplitter.UI.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var balanceTextView: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceTextView = view.findViewById(R.id.balanceTextView)
        val mainButton = view.findViewById<Button>(R.id.splitButton)

        TradeUtils.fetchBalance { balance ->
            balanceTextView.text = "$balance USD"
        }


        // Initialize the ViewPager and TabLayout
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        // Set up the ViewPager with the adapter
        val adapter = ViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // Set up the TabLayout with the ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orders"
                1 -> "Positions"
                else -> ""
            }
        }.attach()

        // Set up the mainButton click listener
        mainButton.setOnClickListener {
            viewPager.currentItem = 2  // Ensure this is the correct position
        }
    }
}
