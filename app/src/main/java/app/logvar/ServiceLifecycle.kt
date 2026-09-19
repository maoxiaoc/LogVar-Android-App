package app.logvar

import android.content.Context
import android.content.Intent
import java.net.HttpURLConnection
import java.net.URL

internal object ServiceLifecycle {
    fun running(): Boolean {
        val connection = URL("http://127.0.0.1:9321/_logvar/health").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 300
            connection.readTimeout = 300
            connection.responseCode == 200 && connection.inputStream.bufferedReader().use { it.readText() }.contains("\"ready\":true")
        } catch (_: Exception) { false } finally { connection.disconnect() }
    }

    fun setRunning(context: Context, value: Boolean) {
        val intent = Intent(context, ServerService::class.java)
        if (value) context.startForegroundService(intent) else context.stopService(intent)
        // Bounded readiness checks only during a user-requested transition; no idle polling.
        repeat(if (value) 100 else 30) {
            if (running() == value) return
            Thread.sleep(100)
        }
        if (value) context.stopService(intent)
        error(if (value) "服务启动超时，请重试" else "服务尚未停止，请重试")
    }
}
