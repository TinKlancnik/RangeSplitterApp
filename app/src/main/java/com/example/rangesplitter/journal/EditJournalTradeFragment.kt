package com.example.rangesplitter.journal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.rangesplitter.R
import com.example.rangesplitter.databinding.FragmentEditJournalTradeBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditJournalTradeFragment : Fragment(R.layout.fragment_edit_journal_trade) {

    private var _binding: FragmentEditJournalTradeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditJournalTradeBinding.bind(view)

        loadTrade()
        setupSave()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        private const val ARG_ORDER_ID = "order_id"

        fun newInstance(orderId: String) = EditJournalTradeFragment().apply {
            arguments = Bundle().apply { putString(ARG_ORDER_ID, orderId) }
        }
    }

    private val orderId: String by lazy {
        requireArguments().getString(ARG_ORDER_ID)!!
    }
    private fun loadTrade() {
        FirebaseFirestore.getInstance()
            .collection("trades")
            .document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val trade = doc.toObject(JournalTrade::class.java)
                    ?.copy(id = doc.id)
                    ?: return@addOnSuccessListener

                populateFields(trade)
            }
    }
    private fun populateFields(t: JournalTrade) {
        binding.tvTitle.setText(t.symbol)
        binding.etQty.setText(t.qty.toString())
        binding.etEntry.setText(t.entryPrice.toString())
        binding.etExit.setText(t.exitPrice?.toString() ?: "")
        binding.etNotes.setText(t.reason ?: "")
    }
    private fun setupSave() {
        binding.btnSave.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("journal")
                .document(orderId)
                .update(
                    mapOf(
                        "symbol" to binding.tvTitle.text.toString(),
                        "qty" to binding.etQty.text.toString().toDoubleOrNull(),
                        "entryPrice" to binding.etEntry.text.toString().toDoubleOrNull(),
                        "exitPrice" to binding.etExit.text.toString().toDoubleOrNull(),
                        "reason" to binding.etNotes.text.toString()
                    )
                )
        }
    }
}
