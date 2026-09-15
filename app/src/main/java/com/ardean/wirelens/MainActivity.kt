package com.ardean.wirelens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ardean.wirelens.data.ConnectionRecord
import com.ardean.wirelens.data.ConnectionStore
import com.ardean.wirelens.monitor.WireLensMonitorService
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: MaterialButton
    private lateinit var status: TextView
    private lateinit var adapter: ConnectionAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            adapter.submit(ConnectionStore.snapshot())
            val vpnOn = ConnectionStore.vpnActive || isVpnTransportActive()
            status.text = when {
                !ConnectionStore.running -> getString(R.string.status_off)
                vpnOn -> getString(
                    R.string.status_on_with_vpn,
                    adapter.itemCount
                )
                else -> getString(R.string.status_on, adapter.itemCount)
            }
            status.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (ConnectionStore.running) R.color.accent else R.color.warn
                )
            )
            btnToggle.text = getString(
                if (ConnectionStore.running) R.string.stop_monitor else R.string.start_monitor
            )
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        status = findViewById(R.id.status)
        val list = findViewById<RecyclerView>(R.id.list)
        adapter = ConnectionAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            ConnectionStore.clear()
            adapter.submit(emptyList())
        }

        btnToggle.setOnClickListener {
            if (ConnectionStore.running) {
                stopMonitor()
            } else {
                maybeRequestNotifThenMonitor()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
        // If user just granted Usage Access in Settings, they return here —
        // do not auto-start; they tap Start again. Status text stays accurate.
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun maybeRequestNotifThenMonitor() {
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!ok) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIF
                )
                return
            }
        }
        ensureUsageAccessThenStart()
    }

    private fun ensureUsageAccessThenStart() {
        if (!hasUsageAccess()) {
            Toast.makeText(
                this,
                R.string.usage_access_needed,
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.usage_access_open_failed, Toast.LENGTH_LONG).show()
            }
            return
        }
        startMonitor()
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) {
            // Proceed even if notification denied — FGS still works; banner may be limited.
            ensureUsageAccessThenStart()
        }
    }

    private fun startMonitor() {
        val i = Intent(this, WireLensMonitorService::class.java)
        ContextCompat.startForegroundService(this, i)
        ConnectionStore.running = true
    }

    private fun stopMonitor() {
        val i = Intent(this, WireLensMonitorService::class.java).apply {
            action = WireLensMonitorService.ACTION_STOP
        }
        startService(i)
        ConnectionStore.running = false
    }

    companion object {
        private const val REQ_NOTIF = 1002
    }
}

class ConnectionAdapter : RecyclerView.Adapter<ConnectionAdapter.VH>() {
    private var items: List<ConnectionRecord> = emptyList()

    fun submit(list: List<ConnectionRecord>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val destination: TextView = itemView.findViewById(R.id.destination)
        private val meta: TextView = itemView.findViewById(R.id.meta)
        private val why: TextView = itemView.findViewById(R.id.why)

        fun bind(r: ConnectionRecord) {
            appName.text = r.appLabel.ifBlank { "Unknown app" }
            destination.text = itemView.context.getString(R.string.dest_usage_placeholder)
            meta.text = itemView.context.getString(
                R.string.meta_usage,
                formatBytes(r.bytesIn),
                formatBytes(r.bytesOut),
                formatRate(r.rateBytesPerSec),
                r.packageName.ifBlank { "—" }
            )
            why.text = itemView.context.getString(R.string.why_prefix, r.whyItMightMatter)
        }

        private fun formatBytes(n: Long): String {
            if (n < 1024) return "$n B"
            if (n < 1024 * 1024) return String.format("%.1f KB", n / 1024.0)
            if (n < 1024L * 1024L * 1024L) return String.format("%.1f MB", n / (1024.0 * 1024.0))
            return String.format("%.2f GB", n / (1024.0 * 1024.0 * 1024.0))
        }

        private fun formatRate(bps: Long): String {
            if (bps < 1024) return "$bps B/s"
            if (bps < 1024 * 1024) return String.format("%.1f KB/s", bps / 1024.0)
            return String.format("%.1f MB/s", bps / (1024.0 * 1024.0))
        }
    }
}
