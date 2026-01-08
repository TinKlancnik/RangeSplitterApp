package com.example.rangesplitter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rangesplitter.journal.JournalFragment

class MainPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 5 // Number of fragments

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> SplitFragment()
            2 -> ChartFragment()
            3 -> CoinSelectFragment()
            4 -> JournalFragment()
            else -> HomeFragment()
        }
    }
}
