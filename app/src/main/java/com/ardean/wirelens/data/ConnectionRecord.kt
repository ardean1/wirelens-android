package com.ardean.wirelens.data

/**
 * Per-app network usage row for the usage-monitor mode (NetworkStatsManager).
 * Destination IP/port are blank in this mode — we do not claim Android's VPN slot.
 */
data class ConnectionRecord(
    val key: String,
    val uid: Int,
    val appLabel: String,
    val packageName: String,
    val destIp: String = "",
    val destPort: Int = 0,
    val protocol: String = "usage",
    val hostname: String? = null,
    var packetCount: Long = 0,
    var byteCount: Long = 0,
    var bytesIn: Long = 0,
    var bytesOut: Long = 0,
    /** Approximate recent rate in bytes/sec (tx+rx over the poll window). */
    var rateBytesPerSec: Long = 0,
    val firstSeenMs: Long = System.currentTimeMillis(),
    var lastSeenMs: Long = System.currentTimeMillis(),
    val flags: MutableList<String> = mutableListOf(),
    var whyItMightMatter: String = ""
)
