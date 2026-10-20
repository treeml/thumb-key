package au.local.zeekrwake

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Presses the connect button inside the Zeekr app. Only active for a short window after the car
 * is detected, so it is not poking at the UI while you are actually using the phone.
 */
class ZeekrTapService : AccessibilityService() {

    companion object {
        /** elapsedRealtime deadline; set by ScanReceiver. */
        @Volatile var armUntil: Long = 0L
    }

    private var lastTapMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = SystemClock.elapsedRealtime()
        if (now > armUntil) return
        if (event?.packageName?.toString() != Prefs.target) return
        if (now - lastTapMs < 2_000L) return

        val root = rootInActiveWindow ?: return
        val labels = Prefs.tapLabels.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        for (label in labels) {
            val hits = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (node in hits) {
                if (clickable(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    Log.i(Wake.TAG, "tapped '$label'")
                    lastTapMs = now
                    armUntil = 0L   // one tap is enough
                    return
                }
            }
        }
    }

    /** The text node is often a label inside a clickable parent. Walk up until something is. */
    private fun clickable(start: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var n = start
        var depth = 0
        while (n != null && depth < 6) {
            if (n.isClickable) return n
            n = n.parent
            depth++
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        Prefs.appCtx = applicationContext
        Log.i(Wake.TAG, "accessibility service connected")
    }
}
