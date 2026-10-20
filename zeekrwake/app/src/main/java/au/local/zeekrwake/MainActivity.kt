package au.local.zeekrwake

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.*
import android.app.Activity
import androidx.core.app.ActivityCompat

class MainActivity : Activity() {

    private lateinit var macField: EditText
    private lateinit var uuidField: EditText
    private lateinit var pkgField: EditText
    private lateinit var labelField: EditText
    private lateinit var gateField: EditText
    private lateinit var log: TextView

    private val seen = linkedMapOf<String, String>()
    private var liveScan: ScanCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.appCtx = applicationContext

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        macField = field("Car BLE MAC (AA:BB:...)", Prefs.mac, root)
        uuidField = field("or service UUID (if MAC rotates)", Prefs.serviceUuid, root)
        pkgField = field("Zeekr package name", Prefs.target, root)
        labelField = field("Button text to tap (comma separated)", Prefs.tapLabels, root)
        gateField = field("Only wake above this RSSI (-75 is a few metres)",
            Prefs.rssiGate.toString(), root)

        button("Find the Zeekr package", root) { listZeekrPackages() }
        button("Scan 15s: list nearby BLE devices", root) { discover() }
        button("Grant: nearby devices permission", root) { askPerms() }
        button("Grant: display over other apps", root) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        if (BuildConfig.FLAVOR == "tap") {
            button("Open accessibility settings", root) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        button("Battery: set unrestricted", root) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")))
        }
        button("Show last detection", root) { say("last detection: ${Prefs.lastSeen}") }
        button("ARM (register background scan)", root) { save(); say(Wake.register(this)) }
        button("Disarm", root) { Wake.unregister(this); say("Scan unregistered") }
        button("Test: launch + tap now", root) {
            save()
            ZeekrTapService.armUntil = android.os.SystemClock.elapsedRealtime() + 20_000L
            say("launch: ${Wake.launchTarget(this)}")
        }

        log = TextView(this).apply { setTextIsSelectable(true) }
        root.addView(log)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onPause() { super.onPause(); save() }

    private fun save() {
        Prefs.mac = macField.text.toString()
        Prefs.serviceUuid = uuidField.text.toString()
        Prefs.target = pkgField.text.toString()
        Prefs.tapLabels = labelField.text.toString()
        gateField.text.toString().trim().toIntOrNull()?.let { Prefs.rssiGate = it }
    }

    private fun field(hint: String, value: String, parent: LinearLayout): EditText {
        parent.addView(TextView(this).apply { text = hint })
        val e = EditText(this).apply {
            setText(value)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        parent.addView(e)
        return e
    }

    private fun button(label: String, parent: LinearLayout, onClick: (View) -> Unit) {
        parent.addView(Button(this).apply { text = label; setOnClickListener(onClick) })
    }

    private fun say(s: String) {
        log.text = "$s\n\n${log.text}"
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    private fun askPerms() {
        val want = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            want += Manifest.permission.BLUETOOTH_SCAN
            want += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            want += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            want += Manifest.permission.POST_NOTIFICATIONS
        }
        ActivityCompat.requestPermissions(this, want.toTypedArray(), 1)
    }

    private fun listZeekrPackages() {
        val pm = packageManager
        @Suppress("DEPRECATION")
        val hits = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }   // not ourselves
            .filter { it.packageName.contains("zeekr", true) ||
                      pm.getApplicationLabel(it).toString().contains("zeekr", true) }
            .map { "${pm.getApplicationLabel(it)}  ->  ${it.packageName}" }
        say(if (hits.isEmpty()) "No Zeekr app found" else hits.joinToString("\n"))
        hits.firstOrNull()?.substringAfter("->")?.trim()?.let { pkgField.setText(it) }
    }

    /**
     * The top two bits of a BLE random address say whether it rotates. 01 is a resolvable
     * private address, which changes every ~15 minutes, so filtering on that MAC is useless
     * and you want the service UUID instead.
     */
    private fun addrKind(mac: String): String {
        val msb = mac.substringBefore(':').toIntOrNull(16) ?: return ""
        return when (msb shr 6) {
            0b01 -> "[rotates]"
            0b11 -> "[static]"
            else -> "[fixed]"
        }
    }

    /** One-off foreground scan so you can identify the car without a third party tool. */
    @SuppressLint("MissingPermission")
    private fun discover() {
        val scanner = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter?.bluetoothLeScanner ?: return say("Bluetooth is off")
        seen.clear()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val uuids = result.scanRecord?.serviceUuids?.joinToString(",") ?: "-"
                seen[result.device.address] =
                    "${result.device.address} ${addrKind(result.device.address)}  " +
                    "${result.scanRecord?.deviceName ?: "?"}  " +
                    "rssi=${result.rssi}  uuids=$uuids"
            }
        }
        liveScan = cb
        scanner.startScan(null, ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
        say("Scanning 15s. Stand next to the car, lock and unlock it with the fob to make it shout.")
        Handler(Looper.getMainLooper()).postDelayed({
            scanner.stopScan(cb)
            liveScan = null
            say(seen.values.sortedBy { it }.joinToString("\n").ifEmpty { "nothing found" })
        }, 15_000L)
    }
}
