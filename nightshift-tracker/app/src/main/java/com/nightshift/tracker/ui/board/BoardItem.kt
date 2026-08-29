package com.nightshift.tracker.ui.board

import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review

/**
 * One outstanding thing on the board, whichever kind it is.
 *
 * Jobs and reviews are stored differently and edited differently, but on a
 * night shift they are the same thing: something you have not done yet, for a
 * patient in a bed, possibly against a clock. Giving them one shape here is
 * what lets a single list sort and group them together without the list itself
 * having to know which is which.
 */
sealed interface BoardItem {
    val key: String

    /** Whatever bed text this carries — a job's copied label, a review's field. */
    val bedText: String
    val priority: Int

    /** The deadline, if it has one: a job timer or a review reminder. */
    val dueAt: Long?
    val createdAt: Long
    val title: String
    val inProgress: Boolean

    data class JobItem(val job: Job) : BoardItem {
        override val key get() = "job-${job.id}"
        override val bedText get() = job.bed
        override val priority get() = job.priority
        override val dueAt get() = job.timerEndAt
        override val createdAt get() = job.createdAt
        override val title get() = job.text.ifBlank { "New job" }
        override val inProgress get() = job.status == 1
    }

    data class ReviewItem(val review: Review) : BoardItem {
        override val key get() = "review-${review.id}"
        override val bedText get() = review.bed
        override val priority get() = review.priority
        override val dueAt get() = review.remindAt
        override val createdAt get() = review.createdAt
        override val title
            get() =
                review.reason.ifBlank { review.patientName.ifBlank { "Clinical review" } }

        /** A review with an impression started is one you are part-way through. */
        override val inProgress get() = review.impression.isNotBlank() || review.plan.isNotBlank()
    }
}

/**
 * What needs doing next, top of the list.
 *
 * Four bands, in the order a tired brain actually wants them:
 *   0  the clock has run out — longest overdue first
 *   1  flagged URGENT with no clock on it
 *   2  running against a clock — soonest due first
 *   3  everything else, by priority then by when it was written down
 *
 * Bands matter more than a single sort key would allow: a routine job due in
 * four hours must not outrank an urgent review just because it happens to
 * carry a time, and nothing urgent should sit below an expired countdown.
 */
fun boardOrder(items: List<BoardItem>, now: Long): List<BoardItem> =
    items.sortedWith(
        compareBy(
            { item: BoardItem ->
                val due = item.dueAt
                when {
                    due != null && due <= now -> 0
                    item.priority == 1 -> 1
                    due != null -> 2
                    else -> 3
                }
            },
            { it.dueAt ?: Long.MAX_VALUE },
            { it.priority },
            { it.createdAt },
        ),
    )
