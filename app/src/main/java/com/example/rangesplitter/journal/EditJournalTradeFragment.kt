package com.example.rangesplitter.journal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.rangesplitter.R
import com.example.rangesplitter.databinding.FragmentEditJournalTradeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditJournalTradeFragment : Fragment(R.layout.fragment_edit_journal_trade) {

    private var _binding: FragmentEditJournalTradeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditJournalTradeBinding.bind(view)

        loadTrade()
        setupSave()
        setupBack()
        setupDelete()
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
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
        binding.etPair.setText(t.symbol)
        binding.etQty.setText(t.qty.toString())
        binding.etEntry.setText(t.entryPrice.toString())
        binding.etExit.setText(t.exitPrice?.toString() ?: "")
        binding.etPnL.setText(t.pnl?.toString() ?: "")
        binding.etNotes.setText(t.reason ?: "")
    }
    private fun setupSave() {
        binding.btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("trades")
                .document(orderId)
                .update(
                    mapOf(
                        "symbol" to binding.tvTitle.text.toString(),
                        "qty" to binding.etQty.text.toString().toDoubleOrNull(),
                        "entryPrice" to binding.etEntry.text.toString().toDoubleOrNull(),
                        "exitPrice" to binding.etExit.text.toString().toDoubleOrNull(),
                        "pnl" to binding.etPnL.text.toString().toDoubleOrNull(),
                        "reason" to binding.etNotes.text.toString()
                    )
                )
        }
    }
    private fun setupBack() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, JournalFragment())
                .commit()
        }
    }
    private fun setupDelete() {
        binding.btnDelete.setOnClickListener {

            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete trade?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteTrade() }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.vibrant_red))

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(requireContext().getColor(R.color.textPrimary))
            }

            dialog.show()
        }
    }
    private fun deleteTrade() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("trades")
            .document(orderId)
            .delete()
            .addOnSuccessListener {
                parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, JournalFragment())
                    .commit()
            }
    }
}
