package com.example.rangesplitter.journal

import com.google.firebase.Timestamp

data class JournalTrade(
    val id: String = "",
    val symbol: String = "",
    val side: String = "",
    val qty: Double = 0.0,
    val entryPrice: Double = 0.0,
    val exitPrice: Double? = null,
    val pnlUsd: Double? = null,
    val reason: String? = null,
    val entryTime: Timestamp? = null,
    val status: String = "DRAFT"
)
