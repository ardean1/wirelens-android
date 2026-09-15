package com.ardean.wirelens.data

import java.util.concurrent.ConcurrentHashMap

object ConnectionStore {
    private val map = ConcurrentHashMap<String, ConnectionRecord>()
    @Volatile var running: Boolean = false
    /** True when ConnectivityManager reports an active TRANSPORT_VPN. */
    @Volatile var vpnActive: Boolean = false

    fun clear() = map.clear()

    fun snapshot(): List<ConnectionRecord> =
        map.values.sortedByDescending { it.rateBytesPerSec * 2 + it.byteCount }

    /**
     * Upsert a usage-monitor row from NetworkStats deltas.
     */
    fun observeUsage(
        key: String,
        uid: Int,
        appLabel: String,
        packageName: String,
        bytesIn: Long,
        bytesOut: Long,
        rateBytesPerSec: Long,
        windowBytesIn: Long,
        windowBytesOut: Long
    ) {
        val total = bytesIn + bytesOut
        val existing = map[key]
        if (existing != null) {
            existing.bytesIn = bytesIn
            existing.bytesOut = bytesOut
            existing.byteCount = total
            existing.rateBytesPerSec = rateBytesPerSec
            existing.lastSeenMs = System.currentTimeMillis()
            existing.whyItMightMatter = Heuristics.evaluateUsage(
                appLabel = appLabel,
                rateBytesPerSec = rateBytesPerSec,
                windowBytes = windowBytesIn + windowBytesOut,
                bytesIn = bytesIn,
                bytesOut = bytesOut,
                flags = existing.flags
            )
            return
        }
        val flags = mutableListOf<String>()
        val why = Heuristics.evaluateUsage(
            appLabel = appLabel,
            rateBytesPerSec = rateBytesPerSec,
            windowBytes = windowBytesIn + windowBytesOut,
            bytesIn = bytesIn,
            bytesOut = bytesOut,
            flags = flags
        )
        map[key] = ConnectionRecord(
            key = key,
            uid = uid,
            appLabel = appLabel,
            packageName = packageName,
            destIp = "",
            destPort = 0,
            protocol = "usage",
            hostname = null,
            byteCount = total,
            bytesIn = bytesIn,
            bytesOut = bytesOut,
            rateBytesPerSec = rateBytesPerSec,
            flags = flags,
            whyItMightMatter = why
        )
    }
}

object Heuristics {
    /** ~50 KB/s sustained in the poll window — "sudden busy". */
    private const val SUDDEN_BUSY_BPS = 50_000L
    /** ~5 MB in a short window — high recent use. */
    private const val HIGH_WINDOW_BYTES = 5L * 1024L * 1024L
    /** High cumulative bytes with little recent rate can still be "background hungry". */
    private const val HIGH_BACKGROUND_BYTES = 20L * 1024L * 1024L

    fun evaluateUsage(
        appLabel: String,
        rateBytesPerSec: Long,
        windowBytes: Long,
        bytesIn: Long,
        bytesOut: Long,
        flags: MutableList<String>
    ): String {
        flags.clear()
        val bits = mutableListOf<String>()
        if (rateBytesPerSec >= SUDDEN_BUSY_BPS || windowBytes >= HIGH_WINDOW_BYTES) {
            flags += "sudden_busy"
            bits += "Sudden busy: lots of traffic in the recent window — often streaming, sync, or updates."
        }
        val total = bytesIn + bytesOut
        if (total >= HIGH_BACKGROUND_BYTES && rateBytesPerSec < SUDDEN_BUSY_BPS / 5) {
            flags += "high_background"
            bits += "High background use over the monitored period — check if you expect $appLabel online when idle."
        }
        if (bits.isEmpty()) {
            return "Ordinary usage so far for this window. Still not a security verdict. Destinations need the VPN slot — WireLens leaves that for your real VPN."
        }
        return bits.joinToString(" ") + " WireLens is not antivirus."
    }
}
