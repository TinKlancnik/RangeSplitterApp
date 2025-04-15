import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rangesplitter.R
import TradeUtils.fetchOpenPositions
import TradeUtils.startPeriodicUpdate
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import bybit.sdk.rest.ByBitRestClient

private lateinit var recyclerView: RecyclerView
private lateinit var bybitClient: ByBitRestClient

class OpenPositionsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_open_positions, container, false)
        recyclerView = view.findViewById(R.id.openPositionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Init Bybit client
        bybitClient = TradeUtils.getByBitClient()

        // Fetch and display open orders
        fetchOpenPositions(bybitClient, recyclerView)

        startPeriodicUpdate(bybitClient, recyclerView)

        // Add custom dividerb
        val dividerItemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        val dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        dividerItemDecoration.setDrawable(dividerDrawable!!)
        recyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }
}
