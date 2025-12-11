package com.example.rangesplitter.UI

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rangesplitter.OpenOrdersFragment
import com.example.rangesplitter.OpenPositionsFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OpenOrdersFragment()
            1 -> OpenPositionsFragment()
            else -> OpenOrdersFragment()
        }
    }
}
