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

    suspend fun deleteShiftCascade(shift: Shift): Triple<Shift, List<Job>, List<Review>> =
        commit {
            val jobs = jobDao.forShiftOnce(shift.id)
            val reviews = reviewDao.forShiftOnce(shift.id)
            jobs.forEach { jobDao.delete(it.id) }
            reviews.forEach { reviewDao.delete(it.id) }
            shiftDao.delete(shift.id)
            Triple(shift, jobs, reviews)
        }

    suspend fun restoreShiftCascade(data: Triple<Shift, List<Job>, List<Review>>) =
        commit {
            shiftDao.upsert(data.first)
            data.second.forEach { jobDao.upsert(it) }
            data.third.forEach { reviewDao.upsert(it) }
        }

    suspend fun addJob(shiftId: String): Job =
        commit {
            val job = Job(id = UUID.randomUUID().toString(), shiftId = shiftId, createdAt = System.currentTimeMillis())
            jobDao.upsert(job)
            job
        }

    suspend fun updateJob(job: Job) = commit { jobDao.update(job) }

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
}
