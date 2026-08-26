package com.nightshift.tracker.data

import android.content.Context
import com.nightshift.tracker.alarm.TimerAlarms
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

/**
 * Single write path for the whole app. Every method commits to Room
 * immediately and then schedules the JSON auto-backup — there is no other
 * way to mutate state, so nothing ever exists only in memory.
 */
class Repository(
    private val context: Context,
    val db: AppDatabase,
    scope: CoroutineScope,
) {
    val backup = BackupManager(context, db, scope)

    val shiftDao get() = db.shiftDao()
    val jobDao get() = db.jobDao()
    val reviewDao get() = db.reviewDao()
    val wardRoundDao get() = db.wardRoundDao()
    val bedDao get() = db.bedDao()
    val procedureDao get() = db.procedureDao()
    val learningDao get() = db.learningDao()

    private suspend fun <T> commit(block: suspend () -> T): T {
        val result = block()
        backup.scheduleBackup()
        return result
    }

    suspend fun startShift(label: String): Shift =
        commit {
            val shift =
                Shift(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    startedAt = System.currentTimeMillis(),
                )
            shiftDao.upsert(shift)
            shift
        }

    suspend fun archiveShift(shift: Shift) =
        commit {
            // Kill any live alarms on this shift before it leaves the board —
            // nothing is worse than a finished shift waking you at 0400.
            jobDao.forShiftOnce(shift.id).forEach { job ->
                if (job.timerEndAt != null) {
                    TimerAlarms.cancel(context, job.id)
                    jobDao.update(job.copy(timerEndAt = null))
                }
            }
            reviewDao.forShiftOnce(shift.id).forEach { review ->
                if (review.remindAt != null) {
                    TimerAlarms.cancel(context, review.id)
                    reviewDao.upsert(review.copy(remindAt = null))
                }
            }
            shiftDao.upsert(shift.copy(archived = true, archivedAt = System.currentTimeMillis()))
        }

    suspend fun deleteShiftCascade(shift: Shift): ShiftSnapshot =
        commit {
            val snapshot =
                ShiftSnapshot(
                    shift = shift,
                    jobs = jobDao.forShiftOnce(shift.id),
                    reviews = reviewDao.forShiftOnce(shift.id),
                    rounds = wardRoundDao.forShiftOnce(shift.id),
                    beds = bedDao.forShiftOnce(shift.id),
                )
            snapshot.jobs.forEach {
                if (it.timerEndAt != null) TimerAlarms.cancel(context, it.id)
                jobDao.delete(it.id)
            }
            snapshot.reviews.forEach {
                if (it.remindAt != null) TimerAlarms.cancel(context, it.id)
                reviewDao.delete(it.id)
            }
            snapshot.rounds.forEach { wardRoundDao.delete(it.id) }
            snapshot.beds.forEach { bedDao.delete(it.id) }
            shiftDao.delete(shift.id)
            snapshot
        }

    suspend fun restoreShiftCascade(data: ShiftSnapshot) =
        commit {
            shiftDao.upsert(data.shift)
            data.beds.forEach { bedDao.upsert(it) }
            val now = System.currentTimeMillis()
            data.jobs.forEach { job ->
                jobDao.upsert(job)
                job.timerEndAt?.let { if (it > now) TimerAlarms.schedule(context, job.id, job.text, it) }
            }
            data.reviews.forEach { review ->
                reviewDao.upsert(review)
                review.remindAt?.let {
                    if (it > now) {
                        val who = review.patientName.ifBlank { "Bed " + review.bed }
                        TimerAlarms.schedule(context, review.id, "Review: " + who, it)
                    }
                }
            }
            data.rounds.forEach { wardRoundDao.upsert(it) }
        }

    // ---- Beds ----

    suspend fun addBed(shiftId: String, label: String): Bed =
        commit {
            val bed =
                Bed(
                    id = UUID.randomUUID().toString(),
                    shiftId = shiftId,
                    label = label.trim(),
                    createdAt = System.currentTimeMillis(),
                )
            bedDao.upsert(bed)
            bed
        }

    /** Renaming a bed rewrites the label on its jobs, so notes stay truthful. */
    suspend fun updateBed(bed: Bed) =
        commit {
            bedDao.upsert(bed)
            jobDao.forBedOnce(bed.id).forEach { job ->
                if (job.bed != bed.label) jobDao.update(job.copy(bed = bed.label))
            }
        }

    /**
     * Deleting a bed keeps its jobs — they fall back to unassigned rather than
     * disappearing with the container. Losing work to a mis-tap is the one
     * thing this app must never do.
     */
    suspend fun deleteBed(bed: Bed): List<Job> =
        commit {
            val orphaned = jobDao.forBedOnce(bed.id)
            orphaned.forEach { jobDao.update(it.copy(bedId = null)) }
            bedDao.delete(bed.id)
            orphaned
        }

    suspend fun restoreBed(bed: Bed, jobs: List<Job>) =
        commit {
            bedDao.upsert(bed)
            jobs.forEach { jobDao.update(it.copy(bedId = bed.id, bed = bed.label)) }
        }

    suspend fun addJob(shiftId: String): Job =
        commit {
            val job = Job(id = UUID.randomUUID().toString(), shiftId = shiftId, createdAt = System.currentTimeMillis())
            jobDao.upsert(job)
            job
        }

    suspend fun updateJob(job: Job) = commit { jobDao.update(job) }

    /** Done: hide from the active list; kill any running timer with it. */
    suspend fun completeJob(job: Job) =
        commit {
            if (job.timerEndAt != null) TimerAlarms.cancel(context, job.id)
            jobDao.update(job.copy(status = 2, timerEndAt = null))
        }

    suspend fun deleteJob(job: Job) =
        commit {
            if (job.timerEndAt != null) TimerAlarms.cancel(context, job.id)
            jobDao.delete(job.id)
        }

    /** Undo restore: same id + createdAt puts it back in its original sort position. */
    suspend fun restoreJob(job: Job) =
        commit {
            jobDao.upsert(job)
            job.timerEndAt?.let { end ->
                if (end > System.currentTimeMillis()) {
                    TimerAlarms.schedule(context, job.id, job.text, end)
                }
            }
        }

    suspend fun setJobTimer(job: Job, endAt: Long?) =
        commit {
            if (endAt == null) {
                TimerAlarms.cancel(context, job.id)
            } else {
                TimerAlarms.schedule(context, job.id, job.text, endAt)
            }
            jobDao.update(job.copy(timerEndAt = endAt))
        }

    suspend fun addReview(shiftId: String): Review =
        commit {
            val review = Review(id = UUID.randomUUID().toString(), shiftId = shiftId, createdAt = System.currentTimeMillis())
            reviewDao.upsert(review)
            review
        }

    suspend fun updateReview(review: Review) = commit { reviewDao.upsert(review) }

    /** Done: park it in the Completed drawer, and stop it alarming. */
    suspend fun completeReview(review: Review) =
        commit {
            if (review.remindAt != null) TimerAlarms.cancel(context, review.id)
            reviewDao.upsert(review.copy(done = true, remindAt = null))
        }

    suspend fun setReviewReminder(review: Review, at: Long?) =
        commit {
            if (at == null) {
                TimerAlarms.cancel(context, review.id)
            } else {
                val who = review.patientName.ifBlank { "Bed ${review.bed}" }
                TimerAlarms.schedule(context, review.id, "Review: $who", at)
            }
            reviewDao.upsert(review.copy(remindAt = at))
        }

    suspend fun deleteReview(review: Review) =
        commit {
            if (review.remindAt != null) TimerAlarms.cancel(context, review.id)
            reviewDao.delete(review.id)
        }

    suspend fun restoreReview(review: Review) =
        commit {
            reviewDao.upsert(review)
            review.remindAt?.let { at ->
                if (at > System.currentTimeMillis()) {
                    val who = review.patientName.ifBlank { "Bed " + review.bed }
                    TimerAlarms.schedule(context, review.id, "Review: " + who, at)
                }
            }
        }

    suspend fun addRound(shiftId: String): WardRound =
        commit {
            val round =
                WardRound(id = UUID.randomUUID().toString(), shiftId = shiftId, createdAt = System.currentTimeMillis())
            wardRoundDao.upsert(round)
            round
        }

    suspend fun updateRound(round: WardRound) = commit { wardRoundDao.upsert(round) }

    suspend fun deleteRound(round: WardRound) = commit { wardRoundDao.delete(round.id) }

    suspend fun restoreRound(round: WardRound) = commit { wardRoundDao.upsert(round) }

    suspend fun updateShift(shift: Shift) = commit { shiftDao.upsert(shift) }

    suspend fun recordBreak(shift: Shift) =
        commit { shiftDao.upsert(shift.copy(lastBreakAt = System.currentTimeMillis())) }

    // ---- Procedure logbook (survives shift archiving and deletion) ----

    suspend fun logProcedure(
        name: String,
        supervision: String,
        outcome: String,
        notes: String,
        shiftId: String?,
    ): ProcedureLog =
        commit {
            val entry =
                ProcedureLog(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    supervision = supervision,
                    outcome = outcome,
                    notes = notes,
                    shiftId = shiftId,
                    performedAt = System.currentTimeMillis(),
                )
            procedureDao.upsert(entry)
            entry
        }

    suspend fun updateProcedure(entry: ProcedureLog) = commit { procedureDao.upsert(entry) }

    suspend fun deleteProcedure(entry: ProcedureLog) = commit { procedureDao.delete(entry.id) }

    suspend fun restoreProcedure(entry: ProcedureLog) = commit { procedureDao.upsert(entry) }

    // ---- Learning questions ----

    suspend fun addLearning(
        question: String,
        context: String,
        shiftId: String?,
    ): LearningItem =
        commit {
            val item =
                LearningItem(
                    id = UUID.randomUUID().toString(),
                    question = question,
                    context = context,
                    shiftId = shiftId,
                    createdAt = System.currentTimeMillis(),
                )
            learningDao.upsert(item)
            item
        }

    suspend fun updateLearning(item: LearningItem) = commit { learningDao.upsert(item) }

    suspend fun deleteLearning(item: LearningItem) = commit { learningDao.delete(item.id) }

    suspend fun restoreLearning(item: LearningItem) = commit { learningDao.upsert(item) }
}
