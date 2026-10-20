package au.local.zeekrwake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** PendingIntent scans do not survive a reboot, so put the filter back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.appCtx = context.applicationContext
        Log.i(Wake.TAG, "boot: ${Wake.register(context)}")
    }
}
