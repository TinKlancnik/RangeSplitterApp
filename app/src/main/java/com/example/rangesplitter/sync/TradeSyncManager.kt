package com.example.rangesplitter.sync

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        closedPnlLookbackMs: Long = 24 * 60 * 60 * 1000L,
        closeEventWindowMs: Long = 5_000L
    ) {
        if (job?.isActive == true) return

        job = scope.launch(dispatcher) {
            while (isActive) {
                try {
                    syncOpenTrades(closedPnlLookbackMs, closeEventWindowMs)
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

    private suspend fun syncOpenTrades(lookbackMs: Long, windowMs: Long) {
        val uid = auth.currentUser?.uid ?: return

        val tradesCol = db.collection("users").document(uid).collection("trades")

        val snap = tradesCol
            .whereEqualTo("status", "OPEN")
            .limit(100)
            .get()
            .await()

        if (snap.isEmpty) return

        val openTrades = snap.documents.mapNotNull { doc ->
            val symbol = (doc.getString("symbol") ?: "").removeSuffix(".P")
            val orderId = doc.getString("orderId") ?: doc.id
            val entryTime = doc.getTimestamp("entryTime")
            if (symbol.isBlank() || orderId.isBlank() || entryTime == null) return@mapNotNull null

            FireTrade(
                orderId = orderId,
                symbol = symbol,
                entryTime = entryTime
            )
        }

        if (openTrades.isEmpty()) return

        val now = System.currentTimeMillis()
        val start = now - lookbackMs

        openTrades.groupBy { it.symbol }.forEach { (symbol, symbolTrades) ->
            val closed = try {
                bybitApi.fetchClosedPnl(symbol, start, now)
            } catch (t: Throwable) {
                Log.e(TAG, "fetchClosedPnl failed for $symbol: ${t.message}", t)
                emptyList()
            }

            if (closed.isEmpty()) return@forEach

            val events = buildCloseEvents(closed, windowMs)
            if (events.isEmpty()) return@forEach

            for (event in events) {
                val eventEndMs = event.maxOfOrNull { it.updatedTimeMs ?: 0L } ?: 0L
                if (eventEndMs <= 0L) continue

                val eventPnl = event.sumOf { it.closedPnl }
                val eventExit = weightedExitPrice(event)

                symbolTrades.forEach { tr ->
                    val entryMs = tr.entryTime.toDate().time
                    if (entryMs <= eventEndMs) {
                        markTradeClosed(uid, tr.orderId, eventExit, eventPnl, eventEndMs)
                    }
                }
            }
        }
    }
    private fun buildCloseEvents(items: List<ClosedPnlItem>, windowMs: Long): List<List<ClosedPnlItem>> {
        val sorted = items
            .filter { (it.updatedTimeMs ?: 0L) > 0L }
            .sortedBy { it.updatedTimeMs!! }

        if (sorted.isEmpty()) return emptyList()

        val events = mutableListOf<MutableList<ClosedPnlItem>>()
        for (item in sorted) {
            if (events.isEmpty()) {
                events.add(mutableListOf(item))
                continue
            }

            val lastEvent = events.last()
            val lastTime = lastEvent.last().updatedTimeMs ?: 0L
            val curTime = item.updatedTimeMs ?: 0L

            if (curTime - lastTime <= windowMs) {
                lastEvent.add(item)
            } else {
                events.add(mutableListOf(item))
            }
        }
        return events
    }

    private fun weightedExitPrice(event: List<ClosedPnlItem>): Double {
        val den = event.sumOf { (it.closedSize ?: 0.0).coerceAtLeast(0.0) }
        if (den > 0.0) {
            val num = event.sumOf { it.avgExitPrice * (it.closedSize ?: 0.0) }
            return num / den
        }
        return event.map { it.avgExitPrice }.average()
    }

    private suspend fun markTradeClosed(
        uid: String,
        orderId: String,
        exitPrice: Double,
        pnl: Double,
        eventEndMs: Long
    ) {
        val doc = db.collection("users").document(uid).collection("trades").document(orderId)

        val updates = hashMapOf<String, Any?>(
            "status" to "CLOSED",
            "exitPrice" to exitPrice,
            "pnl" to pnl,
            "closedTime" to FieldValue.serverTimestamp(),
            "bybitCloseTimeMs" to eventEndMs,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        doc.update(updates).await()
        Log.d(TAG, "Closed Firestore trade orderId=$orderId exit=$exitPrice pnl=$pnl")
    }
}

private data class FireTrade(
    val orderId: String,
    val symbol: String,
    val entryTime: Timestamp
)

interface BybitTradeApi {
    suspend fun fetchClosedPnl(symbol: String, startTimeMs: Long, endTimeMs: Long): List<ClosedPnlItem>
}

data class ClosedPnlItem(
    val orderId: String,
    val closedPnl: Double,
    val avgExitPrice: Double,
    val createdTimeMs: Long?,
    val updatedTimeMs: Long?,
    val closedSize: Double?
)

private fun msToReadable(ms: Long?): String =
    ms?.let {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(java.util.Date(it))
    } ?: "null"
