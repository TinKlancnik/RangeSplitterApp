package com.example.rangesplitter

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for current app session.
 * If app restarts, this resets (later you can persist with Room/DataStore if needed).
 */
object TradeSession {

    data class TradeGroup(
        val tradeGroupId: String,
        val symbol: String,
        val side: String,
        val createdAtMs: Long,
        val orderIds: MutableList<String> = mutableListOf(),
        val orderLinkIds: MutableList<String> = mutableListOf()
    )

    // tradeGroupId -> group info
    private val groups = ConcurrentHashMap<String, TradeGroup>()

    // orderId -> tradeGroupId
    private val orderIdToGroup = ConcurrentHashMap<String, String>()

    // orderLinkId -> tradeGroupId
    private val orderLinkIdToGroup = ConcurrentHashMap<String, String>()

    fun createGroup(symbol: String, side: String): TradeGroup {
        val id = "T_${System.currentTimeMillis()}_${symbol}_${side.uppercase()}"
        val g = TradeGroup(
            tradeGroupId = id,
            symbol = symbol,
            side = side,
            createdAtMs = System.currentTimeMillis()
        )
        groups[id] = g
        return g
    }

    fun getGroup(tradeGroupId: String): TradeGroup? = groups[tradeGroupId]

    fun addOrderLinkId(tradeGroupId: String, orderLinkId: String) {
        groups[tradeGroupId]?.orderLinkIds?.add(orderLinkId)
        orderLinkIdToGroup[orderLinkId] = tradeGroupId
    }

    fun bindOrderIdToGroup(tradeGroupId: String, orderId: String) {
        groups[tradeGroupId]?.orderIds?.add(orderId)
        orderIdToGroup[orderId] = tradeGroupId
    }

    fun findGroupIdByOrderId(orderId: String): String? = orderIdToGroup[orderId]
    fun findGroupIdByOrderLinkId(orderLinkId: String): String? = orderLinkIdToGroup[orderLinkId]
}
