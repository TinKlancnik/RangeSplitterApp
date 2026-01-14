package com.example.rangesplitter.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.R
import com.example.rangesplitter.TradeUtils.fetchOpenPositions
import com.example.rangesplitter.TradeUtils.startPeriodicUpdate
import com.example.rangesplitter.TradeUtils.stopPeriodicUpdate

class OpenPositionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var periodicHandler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_open_positions, container, false)
        recyclerView = view.findViewById(R.id.openPositionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchOpenPositions(recyclerView)

        periodicHandler = startPeriodicUpdate(recyclerView)

        val dividerItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        dividerItemDecoration.setDrawable(dividerDrawable!!)
        recyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        periodicHandler?.let { stopPeriodicUpdate(it) }
        periodicHandler = null
    }
}
