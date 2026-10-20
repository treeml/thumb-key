package com.nightshift.tracker.ui.shift

import com.nightshift.tracker.data.Bed
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.data.WardRound
import com.nightshift.tracker.ui.capture.bedLabel
import com.nightshift.tracker.ui.reviews.buildSoapNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The whole shift as one piece of text.
 *
 * Everything typed in here should be able to leave, and the only integration a
 * sideloaded app reliably has is the share sheet with text/plain — so the
 * export is text, and deliberately readable rather than machine-shaped. It goes
 * wherever the phone will take it: a scribe, email, Files, a notes app.
 *
 * Organised by patient rather than by type, because that is how it will be
 * read. Reviews are rendered as SOAP; jobs as a checklist under the bed they
 * belong to.
 */
/**
 * One patient's section: who they are, their jobs as a checklist, their
 * reviews as SOAP, their ward round entries.
 *
 * Shared by the whole-shift export and the single-patient one so the two can
 * never drift into different formats — the difference between them is which
 * sections get emitted, not how a patient is written.
 */
private fun patientSection(
    key: String,
    beds: List<Bed>,
    jobs: List<Job>,
    reviews: List<Review>,
    rounds: List<WardRound>,
): String {
    val bed = beds.firstOrNull { it.label.trim().equals(key.trim(), true) }
    val theirJobs = jobs.filter { it.bed.trim().equals(key, true) }
    val theirReviews = reviews.filter { it.bed.trim().equals(key, true) }
    val theirRounds = rounds.filter { it.bed.trim().equals(key, true) }
    if (theirJobs.isEmpty() && theirReviews.isEmpty() && theirRounds.isEmpty()) return ""

    val who =
        listOfNotNull(bed?.patientName, bed?.mrn)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    return buildString {
        appendLine("-".repeat(46))
        appendLine(bedLabel(key) + if (who.isNotBlank()) "  ·  $who" else "")
        if (bed?.watch == true) appendLine("** WATCHING **")
        appendLine("-".repeat(46))

        if (theirJobs.isNotEmpty()) {
            appendLine()
            appendLine("Jobs:")
            theirJobs
                .sortedBy { it.createdAt }
                .forEach { job ->
                    val mark = if (job.status == 2) "[x]" else "[ ]"
                    val due =
                        job.timerEndAt?.let {
                            "  (due ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))})"
                        }.orEmpty()
                    val flag = if (job.priority == 1) " !" else ""
                    appendLine("  $mark ${job.text.ifBlank { "(no text)" }}$due$flag")
                }
        }

        theirReviews.sortedBy { it.createdAt }.forEach { review ->
            appendLine()
            appendLine(buildSoapNote(review, bed).trim())
        }

        theirRounds.sortedBy { it.createdAt }.forEach { round ->
            appendLine()
            appendLine("Ward round — ${round.dxOp.ifBlank { "entry" }}")
            listOf(
                "Overnight" to round.overnight,
                "O/E" to round.exam,
                "Results" to round.results,
                "Plan" to round.plan,
            ).filter { it.second.isNotBlank() }
                .forEach { appendLine("${it.first}: ${it.second.trim()}") }
        }
        appendLine()
    }
}

/**
 * One patient, on their own.
 *
 * The same thing you would get for them inside a whole-shift export, with a
 * header saying which shift it came off — because a note handed to somebody
 * else needs to say when it was taken, and by implication by whom.
 */
fun buildPatientExport(
    shift: Shift,
    key: String,
    beds: List<Bed>,
    jobs: List<Job>,
    reviews: List<Review>,
    rounds: List<WardRound>,
): String {
    val stamp = SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault())
    val body = patientSection(key, beds, jobs, reviews, rounds)
    if (body.isBlank()) return "${bedLabel(key)} — nothing recorded this shift."
    return buildString {
        appendLine(shift.label)
        appendLine("Exported ${stamp.format(Date())}")
        appendLine()
        append(body)
    }
}

fun buildShiftExport(
    shift: Shift,
    beds: List<Bed>,
    jobs: List<Job>,
    reviews: List<Review>,
    rounds: List<WardRound>,
): String {
    val stamp = SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault())
    // Group key is the bed text, matching how the board groups it.
    val keys =
        (jobs.map { it.bed } + reviews.map { it.bed } + rounds.map { it.bed })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.uppercase() }
            .sortedBy { it.uppercase() }

    return buildString {
        appendLine("=".repeat(46))
        appendLine(shift.label)
        appendLine("Started ${stamp.format(Date(shift.startedAt))}")
        appendLine("Exported ${stamp.format(Date())}")
        appendLine("=".repeat(46))
        appendLine()

        keys.forEach { key ->
            append(patientSection(key, beds, jobs, reviews, rounds))
        }

        val loose = jobs.filter { it.bed.isBlank() }
        if (loose.isNotEmpty()) {
            appendLine("-".repeat(46))
            appendLine("NO BED")
            appendLine("-".repeat(46))
            loose.sortedBy { it.createdAt }.forEach { job ->
                val mark = if (job.status == 2) "[x]" else "[ ]"
                appendLine("  $mark ${job.text.ifBlank { "(no text)" }}")
            }
            appendLine()
        }

        if (shift.handoverNote.isNotBlank()) {
            appendLine("-".repeat(46))
            appendLine("WATCH OUT FOR")
            appendLine(shift.handoverNote.trim())
        }
    }
}
