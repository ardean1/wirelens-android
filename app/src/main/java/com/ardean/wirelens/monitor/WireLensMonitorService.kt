package com.ardean.wirelens.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ardean.wirelens.MainActivity
import com.ardean.wirelens.R
import com.ardean.wirelens.data.ConnectionStore

/**
 * Foreground usage monitor via [NetworkStatsManager].
 * Does **not** use VpnService / TUN — coexists with a commercial VPN.
 */
class WireLensMonitorService : Service() {

    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private val previousTotals = HashMap<Int, Pair<Long, Long>>() // uid -> (rx, tx)
    private var lastSampleMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitor()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startMonitor()
        }
        return START_STICKY
    }

    private fun startMonitor() {
        if (workerThread != null) return
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
        ConnectionStore.running = true
        previousTotals.clear()
        lastSampleMs = 0L

        val thread = HandlerThread("wirelens-usage").also { it.start() }
        workerThread = thread
        val h = Handler(thread.looper)
        worker = h
        h.post(pollRunnable)
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                ConnectionStore.vpnActive = isVpnTransportActive()
                sampleNetworkStats()
            } catch (_: SecurityException) {
                // Usage access revoked while running
            } catch (_: Exception) {
            }
            worker?.postDelayed(this, POLL_MS)
        }
    }

    private fun isVpnTransportActive(): Boolean {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return false
            cm.allNetworks.any { network ->
                val caps = cm.getNetworkCapabilities(network)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun sampleNetworkStats() {
        val nsm = getSystemService(NetworkStatsManager::class.java) ?: return
        val end = System.currentTimeMillis()
        // Recent window for "window" heuristics + cumulative for display
        val windowStart = end - WINDOW_MS
        val epochStart = end - CUMULATIVE_MS

        val windowByUid = HashMap<Int, LongArray>() // [rx, tx]
        val totalByUid = HashMap<Int, LongArray>()

        collect(nsm, ConnectivityManager.TYPE_WIFI, null, windowStart, end, windowByUid)
        collect(nsm, ConnectivityManager.TYPE_MOBILE, subscriberIdSafe(), windowStart, end, windowByUid)
        collect(nsm, ConnectivityManager.TYPE_WIFI, null, epochStart, end, totalByUid)
        collect(nsm, ConnectivityManager.TYPE_MOBILE, subscriberIdSafe(), epochStart, end, totalByUid)

        val now = System.currentTimeMillis()
        val dtSec = if (lastSampleMs > 0) {
            ((now - lastSampleMs).coerceAtLeast(1L)).toDouble() / 1000.0
        } else {
            POLL_MS / 1000.0
        }

        for ((uid, totals) in totalByUid) {
            if (uid == Process.SYSTEM_UID || uid < 0) continue
            // Skip our own process noise optionally — still show it
            val rx = totals[0]
            val tx = totals[1]
            if (rx + tx <= 0L) continue

            val prev = previousTotals[uid]
            val rate = if (prev != null) {
                val dRx = (rx - prev.first).coerceAtLeast(0L)
                val dTx = (tx - prev.second).coerceAtLeast(0L)
                ((dRx + dTx).toDouble() / dtSec).toLong()
            } else {
                val w = windowByUid[uid]
                if (w != null) {
                    ((w[0] + w[1]).toDouble() / (WINDOW_MS / 1000.0)).toLong()
                } else 0L
            }
            previousTotals[uid] = rx to tx

            val (label, pkg) = labelForUid(uid)
            val w = windowByUid[uid] ?: longArrayOf(0L, 0L)
            ConnectionStore.observeUsage(
                key = "uid:$uid",
                uid = uid,
                appLabel = label,
                packageName = pkg,
                bytesIn = rx,
                bytesOut = tx,
                rateBytesPerSec = rate,
                windowBytesIn = w[0],
                windowBytesOut = w[1]
            )
        }
        lastSampleMs = now
    }

    private fun collect(
        nsm: NetworkStatsManager,
        networkType: Int,
        subscriberId: String?,
        start: Long,
        end: Long,
        into: HashMap<Int, LongArray>
    ) {
        var stats: NetworkStats? = null
        try {
            stats = nsm.querySummary(networkType, subscriberId, start, end)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid == NetworkStats.Bucket.UID_ALL ||
                    uid == NetworkStats.Bucket.UID_TETHERING ||
                    uid == NetworkStats.Bucket.UID_REMOVED
                ) {
                    continue
                }
                val arr = into.getOrPut(uid) { longArrayOf(0L, 0L) }
                arr[0] += bucket.rxBytes
                arr[1] += bucket.txBytes
            }
        } catch (_: Exception) {
        } finally {
            try {
                stats?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * On modern Android, null subscriberId is accepted for mobile summary queries.
     * Older APIs sometimes wanted TelephonyManager.subscriberId (restricted).
     */
    private fun subscriberIdSafe(): String? {
        if (Build.VERSION.SDK_INT >= 29) return null
        return try {
            val tm = getSystemService(TelephonyManager::class.java)
            @Suppress("DEPRECATION", "MissingPermission")
            tm?.subscriberId
        } catch (_: Exception) {
            null
        }
    }

    private fun labelForUid(uid: Int): Pair<String, String> {
        return try {
            val pm = packageManager
            val pkgs = pm.getPackagesForUid(uid)
            val pkg = pkgs?.firstOrNull() ?: return "uid:$uid" to ""
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
            label to pkg
        } catch (_: Exception) {
            "uid:$uid" to ""
        }
    }

    private fun stopMonitor() {
        worker?.removeCallbacksAndMessages(null)
        worker = null
        try {
            workerThread?.quitSafely()
        } catch (_: Exception) {
        }
        workerThread = null
        previousTotals.clear()
        ConnectionStore.running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopMonitor()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "wirelens_monitor"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "WireLens usage monitor",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.ardean.wirelens.STOP_MONITOR"
        private const val NOTIF_ID = 42
        private const val POLL_MS = 3000L
        private const val WINDOW_MS = 60_000L
        private const val CUMULATIVE_MS = 60L * 60L * 1000L // last hour for totals
    }
}
