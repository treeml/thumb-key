package com.nightshift.tracker.ui.handover

import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.data.WardRound
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())

private fun priorityTag(priority: Int): String =
    when (priority) {
        1 -> "[URGENT]"
        3 -> "[ROUTINE]"
        else -> "[SOON]"
    }

/**
 * Builds the written handover for the oncoming team, in the order they want to
 * hear it: what still needs doing, who is sick, what to watch for.
 *
 * Everything here comes from what was already typed during the shift — the
 * point is that a good handover costs zero extra keystrokes at 08:00.
 */
fun buildHandover(
    shift: Shift,
    jobs: List<Job>,
    reviews: List<Review>,
    rounds: List<WardRound>,
    now: Long = System.currentTimeMillis(),
): String {
    val outstanding = jobs.filter { it.status != 2 }.sortedBy { it.priority }
    val doneCount = jobs.count { it.status == 2 }
    val liveReviews = reviews.sortedBy { it.priority }
    val unseen = rounds.filter { !it.seen }

    return buildString {
        appendLine("CLINICAL HANDOVER")
        appendLine("${shift.label} · ${dateFmt.format(Date(shift.startedAt))}")
        appendLine("${timeFmt.format(Date(shift.startedAt))} – ${timeFmt.format(Date(now))}")
        appendLine()

        appendLine("OUTSTANDING (${outstanding.size})")
        if (outstanding.isEmpty()) {
            appendLine("- Nil outstanding.")
        } else {
            outstanding.forEach { job ->
                val bed = if (job.bed.isNotBlank()) "Bed ${job.bed} — " else ""
                val body = job.text.ifBlank { "(no text)" }
                val timer =
                    job.timerEndAt?.let { end ->
                        if (end <= now) {
                            " (timer EXPIRED ${timeFmt.format(Date(end))})"
                        } else {
                            " (due ${timeFmt.format(Date(end))})"
                        }
                    }.orEmpty()
                val progress = if (job.status == 1) " [in progress]" else ""
                appendLine("- ${priorityTag(job.priority)} $bed$body$timer$progress")
            }
        }
        appendLine()

        if (liveReviews.isNotEmpty()) {
            appendLine("PATIENTS REVIEWED (${liveReviews.size})")
            liveReviews.forEach { r ->
                val bed = if (r.bed.isNotBlank()) "Bed ${r.bed}" else "Bed —"
                val name = r.patientName.ifBlank { "Unnamed" }
                val mrn = if (r.mrn.isNotBlank()) " · MRN ${r.mrn}" else ""
                appendLine("- ${priorityTag(r.priority)} $bed · $name$mrn")
                if (r.reason.isNotBlank()) appendLine("    Reason: ${r.reason.trim()}")
                if (r.impression.isNotBlank()) appendLine("    Impression: ${r.impression.trim()}")
                if (r.plan.isNotBlank()) appendLine("    Plan: ${r.plan.trim()}")
                val escalation =
                    when {
                        r.escalatedTo.isNotBlank() && r.escalatedAt != null ->
                            "escalated to ${r.escalatedTo.trim()} at ${timeFmt.format(Date(r.escalatedAt))}"
                        r.registrarNotified -> "registrar notified"
                        else -> "NOT escalated"
                    }
                appendLine("    Escalation: $escalation")
            }
            appendLine()
        }

        if (unseen.isNotEmpty()) {
            appendLine("WARD ROUND — NOT YET SEEN (${unseen.size})")
            unseen.forEach { r ->
                val bed = if (r.bed.isNotBlank()) "Bed ${r.bed}" else "Bed —"
                val name = r.patientName.ifBlank { "Unnamed" }
                val dx = if (r.dxOp.isNotBlank()) " — ${r.dxOp.trim()}" else ""
                appendLine("- $bed · $name$dx")
            }
            appendLine()
        }

        if (shift.handoverNote.isNotBlank()) {
            appendLine("WATCH OUT FOR")
            appendLine(shift.handoverNote.trim())
            appendLine()
        }

        val hours = TimeUnit.MILLISECONDS.toHours(now - shift.startedAt)
        appendLine("Completed this shift: $doneCount job${if (doneCount == 1) "" else "s"}, " +
            "${reviews.size} review${if (reviews.size == 1) "" else "s"} over ${hours}h.")
    }
}

/** One thing that probably shouldn't be left as-is when the shift ends. */
data class ShiftIssue(
    val severity: Int, // 1 = should not leave, 2 = worth a look
    val label: String,
    val detail: String,
)

/**
 * The end-of-shift safety net. Not a nag — a last read-through of the things
 * that most often get dropped between shifts, so leaving them is a decision
 * rather than an accident.
 */
fun shiftIssues(
    jobs: List<Job>,
    reviews: List<Review>,
    rounds: List<WardRound>,
    now: Long = System.currentTimeMillis(),
): List<ShiftIssue> =
    buildList {
        jobs.filter { it.status != 2 && it.priority == 1 }.forEach { job ->
            add(
                ShiftIssue(
                    1,
                    "Urgent job not done",
                    (if (job.bed.isNotBlank()) "Bed ${job.bed} — " else "") + job.text.ifBlank { "(untitled job)" },
                ),
            )
        }
        jobs.filter { it.status != 2 && it.timerEndAt != null && it.timerEndAt <= now }.forEach { job ->
            add(
                ShiftIssue(
                    1,
                    "Timer expired, job still open",
                    (if (job.bed.isNotBlank()) "Bed ${job.bed} — " else "") + job.text.ifBlank { "(untitled job)" },
                ),
            )
        }
        reviews.filter { it.priority == 1 && !it.registrarNotified && it.escalatedTo.isBlank() }.forEach { r ->
            add(
                ShiftIssue(
                    1,
                    "Urgent review with no escalation recorded",
                    "Bed ${r.bed.ifBlank { "—" }} · ${r.patientName.ifBlank { "Unnamed" }}",
                ),
            )
        }
        reviews.filter { it.impression.isBlank() || it.plan.isBlank() }.forEach { r ->
            add(
                ShiftIssue(
                    2,
                    if (r.impression.isBlank()) "Review with no impression" else "Review with no plan",
                    "Bed ${r.bed.ifBlank { "—" }} · ${r.patientName.ifBlank { "Unnamed" }}",
                ),
            )
        }
        jobs.filter { it.status != 2 && it.priority != 1 }.let { rest ->
            if (rest.isNotEmpty()) {
                add(ShiftIssue(2, "Jobs still open", "${rest.size} non-urgent job(s) to hand over"))
            }
        }
        rounds.filter { !it.seen }.let { unseen ->
            if (unseen.isNotEmpty()) {
                add(ShiftIssue(2, "Ward round incomplete", "${unseen.size} patient(s) not marked seen"))
            }
        }
    }
