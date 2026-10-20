package com.nightshift.tracker.ui.reviews

import com.nightshift.tracker.data.Bed
import com.nightshift.tracker.data.Review
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The same review, written as SOAP.
 *
 * The DHR format this app started with mirrors how an overnight review gets
 * typed into the hospital record. SOAP is what a scribe or a summarising tool
 * expects to be handed, and it is what most people want when they are pasting
 * a note somewhere else — so both exist and you pick at the point of export
 * rather than at the point of writing.
 *
 * ABCDE collapses into Objective, because that is what it is: findings. The
 * mapping is deliberately lossless — every letter that was filled in appears,
 * labelled, in the order it was taken.
 */
fun buildSoapNote(review: Review, bed: Bed? = null): String {
    val now = Date()
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

    val who =
        listOfNotNull(
            review.patientName.trim().ifBlank { bed?.patientName?.trim().orEmpty() }.ifBlank { null },
            review.bed.trim().ifBlank { bed?.label?.trim().orEmpty() }.ifBlank { null }?.let { "Bed $it" },
            review.mrn.trim().ifBlank { bed?.mrn?.trim().orEmpty() }.ifBlank { null }?.let { "MRN $it" },
        ).joinToString(" | ")

    val objective =
        listOf("A" to review.a, "B" to review.b, "C" to review.c, "D" to review.d, "E" to review.e)
            .filter { it.second.isNotBlank() }
            .joinToString("\n") { "${it.first}: ${it.second.trim()}" }

    return buildString {
        appendLine("CLINICAL REVIEW — $date $time")
        if (who.isNotBlank()) appendLine(who)
        appendLine()

        appendLine("S:")
        appendLine(review.reason.trim().ifBlank { "Asked to review." })
        appendLine()

        appendLine("O:")
        if (objective.isNotBlank()) {
            appendLine(objective)
        }
        if (review.investigations.isNotBlank()) {
            if (objective.isNotBlank()) appendLine()
            appendLine("Investigations:")
            appendLine(review.investigations.trim())
        }
        if (objective.isBlank() && review.investigations.isBlank()) {
            appendLine("—")
        }
        appendLine()

        appendLine("A:")
        appendLine(review.impression.trim().ifBlank { "—" })
        appendLine()

        appendLine("P:")
        appendLine(review.plan.trim().ifBlank { "—" })

        if (review.escalatedAt != null) {
            appendLine()
            appendLine(
                "Escalated to ${review.escalatedTo.ifBlank { "registrar" }} at " +
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(review.escalatedAt)) + ".",
            )
        } else if (review.registrarNotified) {
            appendLine()
            appendLine("Registrar notified and aware.")
        }
    }
}
