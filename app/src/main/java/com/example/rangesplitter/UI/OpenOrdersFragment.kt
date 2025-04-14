import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.websocket.ByBitWebsocketTopic
import com.example.rangesplitter.OpenOrdersAdapter
import com.example.rangesplitter.R

class OpenOrdersFragment(private val openOrdersList: List<ByBitWebsocketTopic.PrivateTopic.Order>) : RecyclerView.Adapter<OpenOrdersAdapter.OrderViewHolder>() {

    // Create a view holder for each item
    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coinNameTextView: TextView = itemView.findViewById(R.id.coinName)
        val directionTextView: TextView = itemView.findViewById(R.id.direction)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantity)
        val triggerPriceTextView: TextView = itemView.findViewById(R.id.triggerPrice)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
    }

    // Create the view holder and bind data to the views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_open_orders, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = openOrdersList[position]

        holder.coinNameTextView.text = order.coinName
        holder.directionTextView.text = order.direction
        holder.quantityTextView.text = order.quantity.toString()
        holder.triggerPriceTextView.text = order.triggerPrice.toString()

        // Handle cancel button click
        holder.cancelButton.setOnClickListener {
            // Cancel order logic here
        }
    }

    override fun getItemCount(): Int {
        return openOrdersList.size
    }
}
