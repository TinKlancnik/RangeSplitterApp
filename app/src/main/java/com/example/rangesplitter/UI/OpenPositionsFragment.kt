package com.example.rangesplitter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.rest.ByBitRestClient
import com.example.rangesplitter.R
import com.example.rangesplitter.TradeUtils.fetchOpenPositions
import com.example.rangesplitter.TradeUtils.startPeriodicUpdate

class OpenPositionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_open_positions, container, false)
        recyclerView = view.findViewById(R.id.openPositionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Fetch and display open positions
        fetchOpenPositions(recyclerView)
        startPeriodicUpdate( recyclerView)

        // Add custom divider
        val dividerItemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        dividerItemDecoration.setDrawable(dividerDrawable!!)
        recyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }
}
