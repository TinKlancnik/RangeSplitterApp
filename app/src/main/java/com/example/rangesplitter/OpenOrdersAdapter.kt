package com.example.rangesplitter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OpenOrdersAdapter(
    private val orders: List<SplitActivity.OpenOrder>,
    private val onCancelClick: (SplitActivity.OpenOrder) -> Unit
) : RecyclerView.Adapter<OpenOrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coinName: TextView = view.findViewById(R.id.coinName)
        val direction: TextView = view.findViewById(R.id.direction)
        val triggerPrice: TextView = view.findViewById(R.id.triggerPrice)
        val quantity: TextView = view.findViewById(R.id.quantity)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_open_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.coinName.text = order.symbol
        holder.direction.text = order.side
        holder.triggerPrice.text = order.triggerPrice
        holder.quantity.text = order.quantity

        // Optional: color the direction
        holder.direction.setTextColor(
            if (order.side == "Buy") Color.GREEN else Color.RED
        )

        holder.cancelButton.setOnClickListener {
            onCancelClick(order)
        }
    }

    override fun getItemCount() = orders.size
}
