package com.example.rangesplitter.sync

import android.util.Log
import bybit.sdk.rest.position.ClosedPnLParams
import bybit.sdk.rest.position.closedPnLs
import bybit.sdk.shared.Category
import com.example.rangesplitter.BybitClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val pnl = it.closedPnl.toDoubleOrNull() ?: return@mapNotNull null
            val exit = it.avgExitPrice.toDoubleOrNull() ?: return@mapNotNull null

            // these are Strings with epoch-ms in Bybit v5
            val createdMs = it.createdTime.toLongOrNull()
            val updatedMs = it.updatedTime.toLongOrNull()

            // important for weighted exit
            val closedSize = it.closedSize.toDoubleOrNull()

            ClosedPnlItem(
                orderId = it.orderId,
                closedPnl = pnl,
                avgExitPrice = exit,
                createdTimeMs = createdMs,
                updatedTimeMs = updatedMs,
                closedSize = closedSize
            )
        }

        Log.d(TAG, "fetchClosedPnl($symbol) -> ${items.size} items")
        items
    }
}
