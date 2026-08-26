package com.nightshift.tracker.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.nightshift.tracker.ui.theme.RoutineGreen
import com.nightshift.tracker.ui.theme.SoonYellow
import com.nightshift.tracker.ui.theme.UrgentRed
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * How close a deadline is, expressed as one colour.
 *
 * A job with a time on it stops being "priority 2" and starts being "due in
 * eleven minutes" — so once there's a clock running, the clock owns the colour
 * and the card walks green → amber → red on its own. The only exception is a
 * job flagged URGENT, which stays red whatever its timer says; hiding an urgent
 * job behind a calm green because it isn't due for three hours would be worse
 * than useless.
 *
 * Most of the change is packed into the last twenty minutes, because that's
 * where the feeling actually is: an hour out is "fine", eighteen minutes out is
 * "move".
 */
private const val AMBER_AT_MS = 60 * 60 * 1000L // an hour out: starts warming
private const val RED_FROM_MS = 20 * 60 * 1000L // twenty minutes out: going red

/** Colour for a job/review with deadline [endAt]; null endAt falls back to priority. */
fun dueColor(
    endAt: Long?,
    priority: Int,
    now: Long,
): Color {
    if (priority == 1) return UrgentRed
    if (endAt == null) {
        return when (priority) {
            2 -> SoonYellow
            else -> RoutineGreen
        }
    }
    val left = endAt - now
    return when {
        left <= 0L -> UrgentRed
        left <= RED_FROM_MS -> lerp(UrgentRed, SoonYellow, left.toFloat() / RED_FROM_MS)
        left <= AMBER_AT_MS ->
            lerp(
                SoonYellow,
                RoutineGreen,
                (left - RED_FROM_MS).toFloat() / (AMBER_AT_MS - RED_FROM_MS),
            )
        else -> RoutineGreen
    }
}

/** True once the deadline has passed — drives the snooze row and the sort. */
fun isOverdue(
    endAt: Long?,
    now: Long,
): Boolean = endAt != null && endAt <= now

private val CLOCK = SimpleDateFormat("HH:mm", Locale.getDefault())

/** "2h 05m" / "18m" — how long is left, or how long it's been. */
fun spanText(millis: Long): String {
    val mins = (millis / 60_000L).coerceAtLeast(0)
    return if (mins >= 60) "${mins / 60}h ${"%02d".format(mins % 60)}m" else "${mins}m"
}

/** The line under a job: when it's due, and how long that is from now. */
fun dueText(
    endAt: Long,
    now: Long,
): String {
    val left = endAt - now
    val at = CLOCK.format(Date(endAt))
    return if (left <= 0L) "OVERDUE by ${spanText(-left)}" else "$at · ${spanText(left)}"
}

/**
 * A clock the list can sort against.
 *
 * Deliberately coarse. Re-ordering the board every second would mean a job
 * sliding out from under a thumb mid-tap; half a minute is fine for "what's
 * next" and invisible for everything else. Cards run their own one-second
 * ticker for their own countdowns.
 */
@Composable
fun rememberNow(periodMs: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}
