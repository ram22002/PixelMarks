package com.pixelmarks.core.pcserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Utility for detecting local network connectivity and IP address.
 */
object LocalNetworkUtil {

    /**
     * Resolves the primary local IPv4 address (e.g. 192.168.x.x, 10.x.x.x).
     * Prefers active Wi-Fi / LAN interfaces and rejects loopback or WAN addresses.
     */
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            var candidateIp: String? = null

            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val isWireless = networkInterface.name.startsWith("wlan", ignoreCase = true) ||
                        networkInterface.name.startsWith("ap", ignoreCase = true)

                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: continue
                        // Check if private / site-local IPv4
                        if (address.isSiteLocalAddress ||
                            hostAddress.startsWith("192.168.") ||
                            hostAddress.startsWith("10.") ||
                            hostAddress.startsWith("172.")
                        ) {
                            if (isWireless) {
                                return hostAddress // Return immediately if wireless interface
                            }
                            if (candidateIp == null) {
                                candidateIp = hostAddress
                            }
                        }
                    }
                }
            }
            candidateIp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if the device is currently connected to a Wi-Fi or Ethernet network.
     */
    fun isLocalNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Exception) {
            false
        }
    }
}
