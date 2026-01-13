package com.example.rangesplitter.sync

import android.util.Log
import bybit.sdk.rest.order.OrdersOpenParams
import bybit.sdk.rest.position.ClosedPnLParams
import bybit.sdk.shared.Category
import com.example.rangesplitter.BybitClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import bybit.sdk.rest.position.closedPnLs
import bybit.sdk.rest.order.ordersOpen
import kotlinx.coroutines.delay


class BybitTradeApiImpl : BybitTradeApi {

    companion object {
        private const val TAG = "BybitTradeApiImpl"
    }

    override suspend fun fetchClosedPnl(
        symbol: String,
        startTimeMs: Long,
        endTimeMs: Long
    ): List<ClosedPnlItem> = withContext(Dispatchers.IO) {
        val client = BybitClientManager.client

        val params = ClosedPnLParams(
            category = Category.linear,
            symbol = symbol,
            startTime = startTimeMs,
            endTime = endTimeMs,
            limit = 50
        )

        val resp = client.positionClient.closedPnLs(params)

        val items = resp.result.list.mapNotNull { it ->
            val pnl = it.closedPnl.toDoubleOrNull()
            val exit = it.avgExitPrice.toDoubleOrNull()

            if (pnl == null || exit == null) return@mapNotNull null

            ClosedPnlItem(
                orderId = it.orderId,
                closedPnl = pnl,
                avgExitPrice = exit
            )
        }

        Log.d(TAG, "fetchClosedPnl($symbol) -> ${items.size} items")
        items
    }

    override suspend fun fetchAvgEntryPriceFromExecutions(
        orderId: String,
        symbol: String
    ): Double? = withContext(Dispatchers.IO) {

        val client = BybitClientManager.client

        repeat(8) { attempt ->
            val params = OrdersOpenParams(
                category = Category.linear,
                symbol = symbol,
                orderId = orderId,
                openOnly = 0,
                limit = 1
            )

            val resp = client.orderClient.ordersOpen(params)
            val listSize = resp.result.list.size
            val item = resp.result.list.firstOrNull()

            val avgStr = item?.avgPrice
            val cumStr = item?.cumExecQty
            val status = item?.orderStatus?.toString()

            Log.d(
                TAG,
                "ordersOpen attempt=$attempt orderId=$orderId symbol=$symbol listSize=$listSize status=$status avgPrice=$avgStr cumExecQty=$cumStr"
            )

            val avg = avgStr?.toDoubleOrNull()
            val filledQty = cumStr?.toDoubleOrNull()

            if (avg != null && avg > 0.0 && filledQty != null && filledQty > 0.0) {
                Log.d(TAG, "✅ entry avg found: $avg for orderId=$orderId")
                return@withContext avg
            }

            delay(400L + attempt * 250L)
        }

        Log.w(TAG, "❌ entry avg NOT found for orderId=$orderId symbol=$symbol")
        null
    }

}
