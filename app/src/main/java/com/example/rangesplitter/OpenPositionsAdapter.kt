import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.OpenPosition
import com.example.rangesplitter.R
import android.widget.Toast
import com.example.rangesplitter.TradeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class OpenPositionsAdapter(
    private val openPositions: List<OpenPosition>,
    private val onPositionClosed: () -> Unit
) : RecyclerView.Adapter<OpenPositionsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.coinName)
        private val sideTextView: TextView = itemView.findViewById(R.id.direction)
        private val sizeTextView: TextView = itemView.findViewById(R.id.quantity)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.avgPrice)
        private val positionValueTextView: TextView = itemView.findViewById(R.id.leverage)
        private val unrealizedPnlTextView: TextView = itemView.findViewById(R.id.unrealizedPnl)
        private val btnCloseTrade: Button = itemView.findViewById(R.id.btnCloseTrade)


        fun bind(openPosition: OpenPosition) {
            symbolTextView.text = openPosition.symbol
            sideTextView.text = openPosition.side
            sizeTextView.text = openPosition.size
            avgPriceTextView.text = openPosition.avgPrice
            positionValueTextView.text = "${openPosition.leverage}x"
            unrealizedPnlTextView.text = openPosition.unrealisedPnl

            val pnlValue = openPosition.unrealisedPnl.toDoubleOrNull() ?: 0.0
            val colorRes = if (pnlValue >= 0) R.color.vibrant_green else R.color.vibrant_red
            unrealizedPnlTextView.setTextColor(itemView.context.getColor(colorRes))
            val side =
                if (openPosition.side == "Sell") R.color.vibrant_red else R.color.vibrant_green
            sideTextView.setTextColor(itemView.context.getColor(side))

            btnCloseTrade.setOnClickListener {
                val ctx = itemView.context

                val positionSideEnum = when (openPosition.side.uppercase()) {
                    "BUY" -> bybit.sdk.shared.Side.Buy
                    "SELL" -> bybit.sdk.shared.Side.Sell
                    else -> {
                        Toast.makeText(ctx, "Unknown side: ${openPosition.side}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                TradeUtils.closePositionMarket(
                    symbol = openPosition.symbol,
                    qty = openPosition.size,
                    positionSide = positionSideEnum,
                    onSuccess = {
                        Toast.makeText(ctx, "Position closed", Toast.LENGTH_SHORT).show()
                        onPositionClosed()
                    },
                    onError = { err ->
                        Toast.makeText(ctx, "Close failed: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_open_position, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val openPosition = openPositions[position]
        holder.bind(openPosition)
    }

    override fun getItemCount(): Int = openPositions.size
}

