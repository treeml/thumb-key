package au.local.zeekrwake

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import java.util.UUID

object Wake {
    const val TAG = "ZeekrWake"
    private const val REQ = 2114

    /** Don't re-launch Zeekr more often than this. */
    const val COOLDOWN_MS = 60_000L

    private fun scanPendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, ScanReceiver::class.java).setAction(ScanReceiver.ACTION)
        // MUTABLE is required: the system writes the scan results into this intent.
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(ctx, REQ, i, flags)
    }

    /** "fff5" and "0000fff5" are accepted as shorthand for the full 128-bit form. */
    private fun expandUuid(s: String): UUID? {
        val t = s.trim().lowercase()
        val full = when {
            t.length == 4 -> "0000$t-0000-1000-8000-00805f9b34fb"
            t.length == 8 -> "$t-0000-1000-8000-00805f9b34fb"
            else -> t
        }
        return runCatching { UUID.fromString(full) }
            .onFailure { Log.w(TAG, "bad uuid: $s") }
            .getOrNull()
    }

    /**
     * Separate filters, not one combined filter. The scanner ORs the list, so this matches the
     * car by MAC *or* by any of its service UUIDs. That matters because the car advertises a
     * different service depending on whether it is asleep or awake, and a single filter with
     * both a MAC and a UUID would require both to match at once.
     */
    private fun filters(): List<ScanFilter> {
        val out = mutableListOf<ScanFilter>()

        Prefs.mac.takeIf { it.isNotEmpty() }?.let { mac ->
            runCatching { out += ScanFilter.Builder().setDeviceAddress(mac).build() }
                .onFailure { Log.w(TAG, "bad mac: $mac") }
        }
        for (part in Prefs.serviceUuid.split(",")) {
            if (part.isBlank()) continue
            val uuid = expandUuid(part) ?: continue
            out += ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build()
        }
        return out
    }

    private fun settings(): ScanSettings {
        val s = ScanSettings.Builder()
            // Low power is plenty: the chip does the filtering, not the CPU.
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            s.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        }
        return s.build()
    }

    /**
     * Hands the filter to the Bluetooth stack with a PendingIntent. This survives our process
     * being killed and (re-registered on boot) survives reboots. This is the whole trick.
     */
    @SuppressLint("MissingPermission")
    fun register(ctx: Context): String {
        val scanner = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter?.bluetoothLeScanner ?: return "Bluetooth is off"
        val f = filters()
        if (f.isEmpty()) return "Set a MAC or service UUID first"
        val rc = scanner.startScan(f, settings(), scanPendingIntent(ctx))
        return if (rc == 0) "Scan registered (${f.size} filter(s))"
               else "startScan failed (code $rc)"
    }

    @SuppressLint("MissingPermission")
    fun unregister(ctx: Context) {
        val scanner = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter?.bluetoothLeScanner ?: return
        scanner.stopScan(scanPendingIntent(ctx))
    }

    /**
     * Bring the Zeekr app to the foreground. Background activity starts are blocked on Android 10+
     * unless the app holds "Display over other apps", which is why we ask for it.
     */
    fun launchTarget(ctx: Context): Boolean {
        val pkg = Prefs.target
        if (pkg.isEmpty()) return false
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { ctx.startActivity(i); true }
            .onFailure { Log.w(TAG, "launch blocked", it) }
            .getOrDefault(false)
    }

    fun canDrawOverlays(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)
}
