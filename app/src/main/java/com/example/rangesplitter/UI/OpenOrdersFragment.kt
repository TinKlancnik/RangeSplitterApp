package com.example.rangesplitter

import TradeUtils.fetchOpenOrders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.rest.ByBitRestClient

class OpenOrdersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bybitClient: ByBitRestClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_open_orders, container, false)
        recyclerView = view.findViewById(R.id.openOrdersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Init Bybit client
        bybitClient = TradeUtils.getByBitClient()

        // Fetch and display open orders
        fetchOpenOrders(bybitClient, recyclerView)

        return view
    }
}
