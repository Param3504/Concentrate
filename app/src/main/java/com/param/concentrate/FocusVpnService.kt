package com.param.concentrate

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.IOException
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFocus()
                stopSelf()
            }
            else -> {
                val keepOnline = intent?.getStringArrayListExtra(EXTRA_KEEP_ONLINE).orEmpty()
                if (!startFocus(keepOnline)) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        stopFocus()
        stopSelf()
    }

    override fun onDestroy() {
        stopFocus()
        super.onDestroy()
    }

    private fun startFocus(keepOnline: List<String>): Boolean {
        stopFocus()

        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(IPV4_ADDRESS, 32)
            .addRoute("0.0.0.0", 0)
            .addAddress(IPV6_ADDRESS, 128)
            .addRoute("::", 0)
            .setBlocking(true)

        (keepOnline + packageName).distinct().forEach { pkg ->
            try {
                builder.addDisallowedApplication(pkg)
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }

        val descriptor = builder.establish() ?: return false
        tunnel = descriptor
        _running.value = true

        thread(name = "concentrate-drain", isDaemon = true) {
            val input = FileInputStream(descriptor.fileDescriptor)
            val buffer = ByteArray(MAX_PACKET_SIZE)
            try {
                while (true) {
                    if (input.read(buffer) < 0) break
                }
            } catch (_: IOException) {
            }
        }
        return true
    }

    private fun stopFocus() {
        tunnel?.close()
        tunnel = null
        _running.value = false
    }

    companion object {
        const val ACTION_START = "com.param.concentrate.action.START"
        const val ACTION_STOP = "com.param.concentrate.action.STOP"
        const val EXTRA_KEEP_ONLINE = "keep_online"

        private const val IPV4_ADDRESS = "10.111.0.1"
        private const val IPV6_ADDRESS = "fd11:111::1"
        private const val MAX_PACKET_SIZE = 32767

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()
    }
}
