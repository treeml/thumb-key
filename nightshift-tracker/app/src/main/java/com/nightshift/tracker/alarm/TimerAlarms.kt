package com.nightshift.tracker.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nightshift.tracker.MainActivity
import com.nightshift.tracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TimerAlarms {
    const val CHANNEL_ID = "job_timers"
    const val EXTRA_JOB_ID = "jobId"
    const val EXTRA_LABEL = "label"

    fun ensureChannel(context: Context) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Job timers",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Audible alarms when a job timer expires"
                setSound(
                    alarmSound,
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 250, 400, 250, 600)
                setBypassDnd(false)
            }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun pending(context: Context, jobId: String, label: String): PendingIntent {
        val intent =
            Intent(context, TimerAlarmReceiver::class.java).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_LABEL, label)
            }
        return PendingIntent.getBroadcast(
            context,
            jobId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, jobId: String, label: String, endAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, jobId, label)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        }
    }

    fun cancel(context: Context, jobId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, jobId, ""))
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerAlarms.ensureChannel(context)
        val label = intent.getStringExtra(TimerAlarms.EXTRA_LABEL).orEmpty()
        val jobId = intent.getStringExtra(TimerAlarms.EXTRA_JOB_ID).orEmpty()
        val tap =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, TimerAlarms.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Timer up")
                .setContentText(if (label.isBlank()) "Job timer expired" else label)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(tap)
                .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(jobId.hashCode(), notification)
    }
}

/** Re-arm all stored timers after a reboot — deadlines live in Room, not memory. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val db = AppDatabase.get(context)
                db.jobDao().withTimers().forEach { job ->
                    val end = job.timerEndAt ?: return@forEach
                    if (end > now) TimerAlarms.schedule(context, job.id, job.text, end)
                }
                db.reviewDao().withReminders().forEach { review ->
                    val at = review.remindAt ?: return@forEach
                    if (at > now) {
                        val who = review.patientName.ifBlank { "Bed ${review.bed}" }
                        TimerAlarms.schedule(context, review.id, "Review: $who", at)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
