package com.example.rangesplitter.journal

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.lifecycleScope
import com.example.rangesplitter.sync.TradeSyncManager
import com.example.rangesplitter.sync.BybitTradeApiImpl


class JournalFragment : Fragment(R.layout.fragment_journal) {

    private lateinit var tradeSyncManager: TradeSyncManager
    private lateinit var adapter: JournalAdapter
    private var listener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tradeSyncManager = TradeSyncManager(bybitApi = BybitTradeApiImpl())


        val recyclerView = view.findViewById<RecyclerView>(R.id.tradesRecyclerView)

        adapter = JournalAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("trades")
            .orderBy("entryTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val trades = snap.documents.map { doc ->
                    JournalTrade(
                        id = doc.getString("orderId") ?: doc.id,
                        symbol = doc.getString("symbol") ?: "",
                        side = doc.getString("side") ?: "",
                        qty = doc.getDouble("qty") ?: 0.0,
                        entryPrice = doc.getDouble("entryPrice") ?: 0.0,
                        exitPrice = doc.getDouble("exitPrice"),
                        pnlUsd = doc.getDouble("pnl"),
                        reason = doc.getString("reason"),
                        entryTime = doc.getTimestamp("entryTime"),
                        status = doc.getString("status") ?: "OPEN"
                    )
                }

                adapter.submitList(trades)
            }
    }
    override fun onStart() {
        super.onStart()
        tradeSyncManager.start(
            scope = viewLifecycleOwner.lifecycleScope,
            intervalMs = 30_000L
        )
    }

    override fun onStop() {
        super.onStop()
        tradeSyncManager.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
    }
}
