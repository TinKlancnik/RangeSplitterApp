import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.R

class OpenPositionsAdapter(
    private val openPositions: List<OpenPosition>,
    private val onCancelClick: (OpenPosition) -> Unit // Add this second parameter
) : RecyclerView.Adapter<OpenPositionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.coinName)
        private val sideTextView: TextView = itemView.findViewById(R.id.direction)
        private val sizeTextView: TextView = itemView.findViewById(R.id.quantity)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.avgPrice)
        private val positionValueTextView: TextView = itemView.findViewById(R.id.leverage)
        private val unrealizedPnlTextView: TextView = itemView.findViewById(R.id.unrealizedPnl)

        fun bind(openPosition: OpenPosition) {
            symbolTextView.text = openPosition.symbol
            sideTextView.text = openPosition.side
            sizeTextView.text = openPosition.size
            avgPriceTextView.text = openPosition.avgPrice
            positionValueTextView.text = openPosition.leverage
            unrealizedPnlTextView.text = openPosition.unrealisedPnl
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

        // Handle the cancel button click action
        holder.itemView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            onCancelClick(openPosition)
        }
    }

    override fun getItemCount(): Int = openPositions.size
}

