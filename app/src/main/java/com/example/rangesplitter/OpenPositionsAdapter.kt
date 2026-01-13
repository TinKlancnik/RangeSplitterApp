import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.OpenPosition
import com.example.rangesplitter.R
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class OpenPositionsAdapter(
    private val openPositions: List<OpenPosition>,
) : RecyclerView.Adapter<OpenPositionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.coinName)
        private val sideTextView: TextView = itemView.findViewById(R.id.direction)
        private val sizeTextView: TextView = itemView.findViewById(R.id.quantity)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.avgPrice)
        private val positionValueTextView: TextView = itemView.findViewById(R.id.leverage)
        private val unrealizedPnlTextView: TextView = itemView.findViewById(R.id.unrealizedPnl)
        private val btnSaveToJournal: Button = itemView.findViewById(R.id.journalButton)


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

            btnSaveToJournal.setOnClickListener {
                val context = itemView.context
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                if (uid == null) {
                    Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val qty = openPosition.size.toDoubleOrNull() ?: 0.0
                val entry = openPosition.avgPrice.toDoubleOrNull() ?: 0.0
                val side = when {
                    openPosition.side.equals("Buy", true) || openPosition.side.equals("Long", true) -> "LONG"
                    openPosition.side.equals("Sell", true) || openPosition.side.equals("Short", true) -> "SHORT"
                    else -> openPosition.side.uppercase()
                }

                val data = hashMapOf(
                    "symbol" to openPosition.symbol,
                    "side" to side,
                    "qty" to qty,
                    "entryPrice" to entry,
                    "entryTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "status" to "DRAFT"
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("tradeDrafts")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Saved to Journal", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
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

