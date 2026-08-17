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
            // Kill any live timers on this shift's jobs before it leaves the board.
            jobDao.forShiftOnce(shift.id).forEach { job ->
                if (job.timerEndAt != null) {
                    TimerAlarms.cancel(context, job.id)
                    jobDao.update(job.copy(timerEndAt = null))
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
                )
            snapshot.jobs.forEach { jobDao.delete(it.id) }
            snapshot.reviews.forEach { reviewDao.delete(it.id) }
            snapshot.rounds.forEach { wardRoundDao.delete(it.id) }
            shiftDao.delete(shift.id)
            snapshot
        }

    suspend fun restoreShiftCascade(data: ShiftSnapshot) =
        commit {
            shiftDao.upsert(data.shift)
            data.jobs.forEach { jobDao.upsert(it) }
            data.reviews.forEach { reviewDao.upsert(it) }
            data.rounds.forEach { wardRoundDao.upsert(it) }
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

    suspend fun deleteReview(review: Review) = commit { reviewDao.delete(review.id) }

    suspend fun restoreReview(review: Review) = commit { reviewDao.upsert(review) }

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
}
