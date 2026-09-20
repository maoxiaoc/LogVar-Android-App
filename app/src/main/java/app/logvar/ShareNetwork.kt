package app.logvar

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

internal data class ShareNetwork(val ip: String = "127.0.0.1", val description: String = "本机地址 · 仅本机可用")

internal fun readShareNetwork(context: Context): ShareNetwork = try {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val wifi = manager.allNetworks.firstNotNullOfOrNull { network ->
        val caps = manager.getNetworkCapabilities(network)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            manager.getLinkProperties(network)?.linkAddresses?.firstNotNullOfOrNull {
                (it.address as? Inet4Address)?.takeUnless { address -> address.isLoopbackAddress || address.isLinkLocalAddress }?.hostAddress
            }
        } else null
    }
    if (wifi != null) ShareNetwork(wifi, "Wi-Fi 局域网 · 供同一网络的设备使用")
    else {
        // Hotspot downstream interfaces are not represented by allNetworks on many phones.
        // Never fall back to a cellular or VPN interface, even if it has a private address.
        val candidates = NetworkInterface.getNetworkInterfaces().toList().filter {
            it.isUp && !it.isLoopback && ShareAddressPolicy.isHotspotInterface(it.name)
        }.flatMap { network ->
            network.inetAddresses.toList().filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress }
                .mapNotNull { it.hostAddress }
        }.distinct()
        when (candidates.size) {
            1 -> ShareNetwork(candidates.single(), "手机热点 · 供连接此热点的设备使用")
            0 -> ShareNetwork(description="未检测到共享网络 · 仅本机可用")
            else -> ShareNetwork(description="无法确定热点地址 · 仅本机可用")
        }
    }
} catch (_: Exception) {
    ShareNetwork(description="暂时无法读取共享地址 · 仅本机可用")
}
