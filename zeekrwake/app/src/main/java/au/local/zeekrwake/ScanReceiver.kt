package au.local.zeekrwake

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Fired by the Bluetooth stack when the car's advertisement matches our filter, even if this
 * app is not running. Everything downstream of here is "wake Zeekr up and press connect".
 */
class ScanReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "au.local.zeekrwake.CAR_SEEN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Prefs.appCtx = context.applicationContext

        val err = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1)
        if (err != -1) {
            Log.w(Wake.TAG, "scan error $err, re-registering")
            Wake.register(context)
            return
        }

        @Suppress("DEPRECATION")
        val results: List<ScanResult> =
            intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
                ?: emptyList()
        if (results.isEmpty()) return

        val rssi = results.maxOf { it.rssi }
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        Log.i(Wake.TAG, "car seen, rssi=$rssi, n=${results.size}")

        val gate = Prefs.rssiGate
        if (rssi < gate) {
            Prefs.lastSeen = "$stamp  rssi=$rssi  too far (gate $gate), ignored"
            Log.i(Wake.TAG, "below gate $gate, ignoring")
            return
        }
        Prefs.lastSeen = "$stamp  rssi=$rssi  woke Zeekr"

        val now = SystemClock.elapsedRealtime()
        if (now - Prefs.lastWakeMs < Wake.COOLDOWN_MS) return
        Prefs.lastWakeMs = now

        // Arm the accessibility service, then bring Zeekr forward. The service does the tapping
        // once Zeekr's window actually appears.
        ZeekrTapService.armUntil = now + 20_000L
        val launched = Wake.launchTarget(context)
        Log.i(Wake.TAG, "launch target: $launched")
    }
}
