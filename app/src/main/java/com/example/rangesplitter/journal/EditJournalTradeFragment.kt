package com.example.rangesplitter.journal

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.rangesplitter.R

class EditJournalTradeFragment : Fragment(R.layout.fragment_edit_journal_trade) {

    companion object {
        private const val ARG_ORDER_ID = "order_id"

        fun newInstance(orderId: String) = EditJournalTradeFragment().apply {
            arguments = Bundle().apply { putString(ARG_ORDER_ID, orderId) }
        }
    }

    private val orderId: String by lazy {
        requireArguments().getString(ARG_ORDER_ID)!!
    }
}
