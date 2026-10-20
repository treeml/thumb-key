package au.local.zeekrwake

import android.content.Context

object Prefs {
    private const val FILE = "zeekrwake"

    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Car BLE MAC, e.g. "AA:BB:CC:DD:EE:FF". Blank if filtering by UUID instead. */
    var mac: String
        get() = p(appCtx!!).getString("mac", "") ?: ""
        set(v) = p(appCtx!!).edit().putString("mac", v.trim().uppercase()).apply()

    /** Car BLE service UUID. Use this if the MAC rotates (resolvable private address). */
    var serviceUuid: String
        get() = p(appCtx!!).getString("uuid", "") ?: ""
        set(v) = p(appCtx!!).edit().putString("uuid", v.trim()).apply()

    /** Package name of the Zeekr app. */
    var target: String
        get() = p(appCtx!!).getString("target", "") ?: ""
        set(v) = p(appCtx!!).edit().putString("target", v.trim()).apply()

    /** Button labels the accessibility service will tap, comma separated. */
    var tapLabels: String
        get() = p(appCtx!!).getString("labels", "Disconnected,Connect,Bluetooth,连接") ?: ""
        set(v) = p(appCtx!!).edit().putString("labels", v).apply()

    /** Ignore detections weaker than this, so the car is not woken from across the car park. */
    var rssiGate: Int
        get() = p(appCtx!!).getInt("rssiGate", -75)
        set(v) = p(appCtx!!).edit().putInt("rssiGate", v).apply()

    var lastWakeMs: Long
        get() = p(appCtx!!).getLong("lastWake", 0L)
        set(v) = p(appCtx!!).edit().putLong("lastWake", v).apply()

    /** Human readable record of the last detection, so the UI can show it without adb. */
    var lastSeen: String
        get() = p(appCtx!!).getString("lastSeen", "(nothing yet)") ?: ""
        set(v) = p(appCtx!!).edit().putString("lastSeen", v).apply()

    var appCtx: Context? = null
}
