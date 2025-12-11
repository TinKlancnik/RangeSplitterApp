package com.example.rangesplitter

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import bybit.sdk.rest.APIResponseV5
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.order.PlaceOrderParams
import bybit.sdk.rest.order.PlaceOrderResponse
import bybit.sdk.rest.position.LeverageParams
import bybit.sdk.shared.Category
import bybit.sdk.shared.OrderType
import bybit.sdk.shared.Side
import bybit.sdk.shared.TimeInForce
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket.TickerListener
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket.TickerUpdate
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class SplitFragment : Fragment(R.layout.fragment_split), TickerListener {

    companion object {
        private const val ARG_SYMBOL = "symbol"

        fun newInstance(symbol: String): SplitFragment {
            return SplitFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SYMBOL, symbol)
                }
            }
        }
    }

    private lateinit var symbol: String

    private lateinit var coinPriceTextView: TextView
    private lateinit var coinNameTextView: TextView

    private var totalBalance = ""
    private var stopLoss: String? = null
    private var takeProfit: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        symbol = arguments?.getString(ARG_SYMBOL) ?: "BTCUSDT"
        Log.d("SplitFragment", "Received symbol: $symbol")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinNameTextView = view.findViewById(R.id.coinName)
        coinPriceTextView = view.findViewById(R.id.coinPrice)

        // show symbol at the top
        coinNameTextView.text = symbol

        // WebSocket price subscription
        val wsSymbol = normalizeSymbolForWs(symbol)
        BybitLinearTickerWebSocket.addListener(this)
        BybitLinearTickerWebSocket.subscribe(wsSymbol)

        val spinner = view.findViewById<Spinner>(R.id.spinnerValues)
        val rangeTopEditText = view.findViewById<EditText>(R.id.editTextRangeTop)
        val rangeLowEditText = view.findViewById<EditText>(R.id.editTextRangeLow)
        val buyButton = view.findViewById<Button>(R.id.buttonBuy)
        val sellButton = view.findViewById<Button>(R.id.buttonSell)
        val backButton = view.findViewById<ImageView>(R.id.backButton)

        val items = arrayOf(3, 5)
        val spinnerAdapter = android.widget.ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            items
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        spinner.setSelection(0)

        // fetch balance once
        TradeUtils.fetchBalance { balance ->
            totalBalance = balance
            Log.d("SplitFragment", "Fetched balance: $totalBalance")
        }

        view.findViewById<Button>(R.id.setSlTpButton).setOnClickListener {
            showSlTpDialog()
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
            requireActivity().findViewById<View>(R.id.fragmentContainer).visibility = View.GONE
        }

        buyButton.setOnClickListener { btn ->
            val riskStr = view.findViewById<TextView>(R.id.risk).text.toString()
            val risk = riskStr.toFloatOrNull() ?: 0f

            val rangeTop = rangeTopEditText.text.toString().toFloatOrNull() ?: 0f
            val rangeLow = rangeLowEditText.text.toString().toFloatOrNull() ?: 0f
            val numberOfValues = spinner.selectedItem.toString().toIntOrNull() ?: 0
            val sl = stopLoss?.toFloatOrNull()

            val balanceFloat = totalBalance
                .replace(",", "")
                .toFloatOrNull() ?: 0f

            if (sl != null && totalBalance.isNotEmpty()) {
                val positionData = calculatePositionSizes(
                    top = rangeTop,
                    low = rangeLow,
                    sl = sl,
                    totalBalance = balanceFloat,
                    numberOfBids = numberOfValues,
                    totalRiskPercent = risk
                )

                for ((price, amount) in positionData) {
                    val qtyStr = adjustQtyForSymbol(amount) ?: continue
                    placeATrade(price.toString(), qtyStr, Side.Buy)
                }

                closeKeyboard(btn)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Set a valid Stop Loss and wait for balance",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        sellButton.setOnClickListener { btn ->
            val riskStr = view.findViewById<TextView>(R.id.risk).text.toString()
            val risk = riskStr.toFloatOrNull() ?: 0f

            val rangeTop = rangeTopEditText.text.toString().toFloatOrNull() ?: 0f
            val rangeLow = rangeLowEditText.text.toString().toFloatOrNull() ?: 0f
            val numberOfValues = spinner.selectedItem.toString().toIntOrNull() ?: 0
            val sl = stopLoss?.toFloatOrNull()

            val balanceFloat = totalBalance
                .replace(",", "")
                .toFloatOrNull() ?: 0f

            if (sl != null && totalBalance.isNotEmpty()) {
                val positionData = calculatePositionSizes(
                    top = rangeTop,
                    low = rangeLow,
                    sl = sl,
                    totalBalance = balanceFloat,
                    numberOfBids = numberOfValues,
                    totalRiskPercent = risk
                )

                for ((price, amount) in positionData) {
                    val qtyStr = adjustQtyForSymbol(amount) ?: continue
                    placeATrade(price.toString(), qtyStr, Side.Sell)
                }


                closeKeyboard(btn)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Set a valid Stop Loss and wait for balance",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val wsSymbol = normalizeSymbolForWs(symbol)
        BybitLinearTickerWebSocket.unsubscribe(wsSymbol)
        BybitLinearTickerWebSocket.removeListener(this)
    }

    // ---------------- SL/TP dialog ----------------

    private fun showSlTpDialog() {
        val dialogView = layoutInflater.inflate(R.layout.sl_tp_dialog, null)
        val slInput = dialogView.findViewById<EditText>(R.id.editTextSL)
        val tpInput = dialogView.findViewById<EditText>(R.id.editTextTP)
        val applyBtn = dialogView.findViewById<Button>(R.id.applyButton)

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.show()

        applyBtn.setOnClickListener {
            val slValue = slInput.text.toString().trim()
            val tpValue = tpInput.text.toString().trim()

            stopLoss = slValue.ifEmpty { null }
            takeProfit = tpValue.ifEmpty { null }

            Toast.makeText(
                requireContext(),
                "SL set: ${stopLoss ?: "Not set"} | TP set: ${takeProfit ?: "Not set"}",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }
    }

    // ---------------- helpers ----------------

    private fun leverage(midBid: Float, sl: Float) {
        val slPercentage = ((midBid - sl) / midBid) * 100
        Log.d("leverage", "Mid bid: $midBid, SL: $sl, SL Percentage: $slPercentage%")

        if (slPercentage > 5) {
            val multiplier = (50 / slPercentage).toInt()
            val bybitClient = BybitClientManager.client
            val leverageParams = LeverageParams(
                category = Category.linear,
                symbol = symbol,
                buyLeverage = multiplier.toString(),
                sellLeverage = multiplier.toString()
            )
            val callback = object : ByBitRestApiCallback<APIResponseV5> {
                override fun onSuccess(result: APIResponseV5) {
                    Log.d("Leverage", "Leverage set successfully: $result")
                    showTradeResultDialog("Success", "Leverage set successfully for $symbol.")
                }

                override fun onError(error: Throwable) {
                    Log.e("Leverage", "Error encountered: ${error.message}")
                    showTradeResultDialog("Error", "Failed to set leverage: ${error.message}")
                }
            }
            bybitClient.positionClient.setLeverage(leverageParams, callback)
        }
    }

    private fun calculatePositionSizes(
        top: Float,
        low: Float,
        sl: Float,
        totalBalance: Float,
        numberOfBids: Int,
        totalRiskPercent: Float
    ): List<Pair<Float, Float>> {
        val riskPerBid = (totalBalance * (totalRiskPercent / 100f)) / numberOfBids
        val bidPrices = calculateNumbers(low, top, numberOfBids)

        val midBid = bidPrices.size / 2
        val midBidPrice = bidPrices[midBid]
        leverage(midBidPrice, sl)

        return bidPrices.map { price ->
            val risk = abs(price - sl)
            val positionSize = if (risk > 0) riskPerBid / risk else 0f
            Pair(price, positionSize)
        }
    }

    private fun calculateNumbers(low: Float, top: Float, count: Int): List<Float> {
        val step = (top - low) / (count - 1)
        return List(count) { low + it * step }
    }

    private fun closeKeyboard(view: View) {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun placeATrade(price: String, amount: String, side: Side) {
        val bybitClient = BybitClientManager.client

        val tradeParams = PlaceOrderParams(
            category = Category.linear,
            symbol = symbol,
            side = side,
            orderType = OrderType.Limit,
            price = price,
            qty = amount,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            timeInForce = TimeInForce.GTC,
            reduceOnly = false
        )

        val callback = object : ByBitRestApiCallback<PlaceOrderResponse> {
            override fun onSuccess(result: PlaceOrderResponse) {
                Log.d("Trade", "Trade placed successfully: $result")
                showTradeResultDialog(
                    "Success",
                    "Trade placed successfully for $symbol at $price (qty $amount)."
                )
            }

            override fun onError(error: Throwable) {
                Log.e("Trade", "Error encountered: ${error.message}", error)
                showTradeResultDialog("Error", "Failed to place trade: ${error.message}")
            }
        }

        bybitClient.orderClient.placeOrder(tradeParams, callback)
    }

    private fun showTradeResultDialog(title: String, message: String) {
        activity?.runOnUiThread {
            val ctx = context ?: return@runOnUiThread
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(ctx)
            dialogBuilder.setTitle(title)
            dialogBuilder.setMessage(message)
            dialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            dialogBuilder.show()
        }
    }

    // ---------------- WebSocket price listener ----------------

    private fun normalizeSymbolForWs(sym: String): String = sym.removeSuffix(".P")

    override fun onTicker(update: TickerUpdate) {
        val wsSymbol = normalizeSymbolForWs(symbol)
        if (update.symbol != wsSymbol) return

        val price = update.markPrice ?: update.indexPrice ?: update.lastPrice
        price?.let {
            activity?.runOnUiThread {
                coinPriceTextView.text = String.format(Locale.US, "%.2f", it)
            }
        }
    }
    private fun adjustQtyForSymbol(rawQty: Float): String? {
        if (rawQty <= 0f) return null

        // crude per-symbol rules; you can refine these later or fetch from instrumentsInfo
        val (minQty, step, decimals) = when {
            symbol.startsWith("BTC") -> Triple(0.001, 0.001, 3)
            symbol.startsWith("ETH") -> Triple(0.01, 0.01, 2)
            else -> Triple(0.1, 0.1, 1)   // default for smaller alts; adjust if needed
        }

        val raw = rawQty.toDouble()

        // snap down to nearest step
        val steps = kotlin.math.floor(raw / step + 1e-9)
        val normalized = steps * step

        if (normalized < minQty) {
            // too small to be valid
            return null
        }

        return String.format(Locale.US, "%.${decimals}f", normalized)
    }

}