package com.example.rangesplitter.UI

//import OpenOrdersFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rangesplitter.OpenOrdersFragment
import com.example.rangesplitter.OpenPositionsFragment

class ViewPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OpenOrdersFragment()
            1 -> OpenPositionsFragment()
            else -> OpenOrdersFragment()
        }
    }
}
