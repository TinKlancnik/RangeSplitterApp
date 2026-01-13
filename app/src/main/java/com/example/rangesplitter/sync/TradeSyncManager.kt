package com.example.rangesplitter.sync

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class TradeSyncManager(
    private val bybitApi: BybitTradeApi,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "TradeSyncManager"
    }

    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        intervalMs: Long = 30_000L,
        closedPnlLookbackMs: Long = 24 * 60 * 60 * 1000L
    ) {
        if (job?.isActive == true) return

        job = scope.launch(dispatcher) {
            while (isActive) {
                try {
                    syncOpenTrades(closedPnlLookbackMs)
                } catch (t: Throwable) {
                    Log.e(TAG, "sync loop error: ${t.message}", t)
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }


    private suspend fun syncOpenTrades(closedPnlLookbackMs: Long) {
        val uid = auth.currentUser?.uid ?: return

        val tradesCol = db.collection("users").document(uid).collection("trades")

        val snap = tradesCol
            .whereEqualTo("status", "OPEN")
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        if (snap.isEmpty) return

        val trades = snap.documents.map { doc ->
            FireTrade(
                docId = doc.id,
                orderId = doc.getString("orderId") ?: doc.id,
                orderLinkId = doc.getString("orderLinkId") ?: "",
                symbol = doc.getString("symbol") ?: "",
                side = doc.getString("side") ?: "",
                qty = doc.getDouble("qty") ?: 0.0,
                entryPrice = doc.getDouble("entryPrice"),
                entryTime = doc.getTimestamp("entryTime")
            )
        }.filter { it.symbol.isNotBlank() && it.orderId.isNotBlank() }

        trades.filter { it.entryPrice == null || it.entryPrice == 0.0 }
            .forEach { trade ->
                fillEntryPriceFromExecutions(uid, trade)
            }
        val now = System.currentTimeMillis()
        val start = now - closedPnlLookbackMs

        trades.groupBy { it.symbol }.forEach { (symbol, symbolTrades) ->
            if (symbol.isBlank()) return@forEach

            val closed = try {
                bybitApi.fetchClosedPnl(
                    symbol = symbol,
                    startTimeMs = start,
                    endTimeMs = now
                )
            } catch (t: Throwable) {
                Log.e(TAG, "fetchClosedPnl failed for $symbol: ${t.message}", t)
                emptyList()
            }

            if (closed.isEmpty()) return@forEach

            // index by orderId for fast match
            val byOrderId = closed.associateBy { it.orderId }

            symbolTrades.forEach { tr ->
                val closedItem = byOrderId[tr.orderId] ?: return@forEach
                markTradeClosed(uid, tr.orderId, closedItem)
            }
        }
    }

    private suspend fun fillEntryPriceFromExecutions(uid: String, trade: FireTrade) {
        val avg = try {
            bybitApi.fetchAvgEntryPriceFromExecutions(
                orderId = trade.orderId,
                symbol = trade.symbol
            )
        } catch (t: Throwable) {
            Log.e(TAG, "fetch executions failed orderId=${trade.orderId}: ${t.message}", t)
            null
        }

        if (avg == null || avg <= 0.0) return

        val doc = db.collection("users").document(uid).collection("trades").document(trade.orderId)
        doc.update(
            mapOf(
                "entryPrice" to avg,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun markTradeClosed(uid: String, orderId: String, closed: ClosedPnlItem) {
        val doc = db.collection("users").document(uid).collection("trades").document(orderId)

        val updates = hashMapOf<String, Any?>(
            "status" to "CLOSED",
            "exitPrice" to closed.avgExitPrice,
            "pnl" to closed.closedPnl, // you can also store pnlPercent separately
            "closedTime" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        doc.update(updates).await()
        Log.d(TAG, "Closed orderId=$orderId pnl=${closed.closedPnl} exit=${closed.avgExitPrice}")
    }
}

private data class FireTrade(
    val docId: String,
    val orderId: String,
    val orderLinkId: String,
    val symbol: String,
    val side: String,
    val qty: Double,
    val entryPrice: Double?,
    val entryTime: Timestamp?
)

interface BybitTradeApi {

    suspend fun fetchAvgEntryPriceFromExecutions(orderId: String, symbol: String): Double?
    suspend fun fetchClosedPnl(symbol: String, startTimeMs: Long, endTimeMs: Long): List<ClosedPnlItem>
}

data class ClosedPnlItem(
    val orderId: String,
    val closedPnl: Double,
    val avgExitPrice: Double
)
