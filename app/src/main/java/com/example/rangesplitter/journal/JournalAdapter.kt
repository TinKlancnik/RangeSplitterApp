package com.example.rangesplitter.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.R
import java.text.SimpleDateFormat
import java.util.Locale

class JournalAdapter(
    private val items: MutableList<JournalTrade> = mutableListOf()
) : RecyclerView.Adapter<JournalAdapter.JournalVH>() {

    class JournalVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coinName: TextView = itemView.findViewById(R.id.coinName)
        val direction: TextView = itemView.findViewById(R.id.direction)
        val entry: TextView = itemView.findViewById(R.id.entryPrice)
        val exit: TextView = itemView.findViewById(R.id.exitPrice)
        val quantity: TextView = itemView.findViewById(R.id.quantity)
        val pnlPercent: TextView = itemView.findViewById(R.id.pnlPercent)
        val reason: TextView = itemView.findViewById(R.id.reason)
        val date: TextView = itemView.findViewById(R.id.tradeDate)
        val pnlBar: View = itemView.findViewById(R.id.pnlBar)
        val statusDot: View = itemView.findViewById(R.id.statusDot)

        val side: TextView = itemView.findViewById(R.id.direction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_journal, parent, false)
        return JournalVH(view)
    }

    override fun onBindViewHolder(holder: JournalVH, position: Int) {
        val t = items[position]
        val ctx = holder.itemView.context

        holder.coinName.text = t.symbol
        holder.direction.text = t.side
        if(t.side=="SELL") {
            holder.direction.setTextColor(ctx.getColor(R.color.vibrant_red))
        }

        holder.entry.text = t.entryPrice.toString()
        holder.exit.text = t.exitPrice?.toString() ?: "-"
        holder.quantity.text = t.qty.toString()

        val pnl = t.pnlUsd

        if (pnl == null) {
            holder.pnlPercent.text = "—"
            holder.pnlBar.setBackgroundResource(R.drawable.bg_pnl_positive)
        } else if (pnl >= 0) {
            holder.pnlPercent.text = "+$" + String.format("%.2f", pnl)
            holder.pnlBar.setBackgroundResource(R.drawable.bg_pnl_positive)
            holder.pnlPercent.setTextColor(ctx.getColor(R.color.vibrant_green))
        } else {
            holder.pnlPercent.text = "-$" + String.format("%.2f", kotlin.math.abs(pnl))
            holder.pnlBar.setBackgroundResource(R.drawable.bg_pnl_negative)
            holder.statusDot.setBackgroundResource(R.drawable.circle_red)
            holder.pnlPercent.setTextColor(ctx.getColor(R.color.vibrant_red))
        }


        holder.reason.text = t.reason ?: "Add note…"

        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        holder.date.text = t.entryTime?.toDate()?.let { sdf.format(it) } ?: "—"
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<JournalTrade>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
