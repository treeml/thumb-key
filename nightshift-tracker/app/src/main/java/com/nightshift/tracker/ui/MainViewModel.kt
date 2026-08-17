package com.nightshift.tracker.ui

import android.app.Application
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nightshift.tracker.NightshiftApp
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveSearchHit(
    val shift: Shift,
    val snippet: String,
)

sealed interface Screen {
    data object Home : Screen

    data object ActiveShift : Screen

    data class ArchiveDetail(val shiftId: String) : Screen
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val repo = (app as NightshiftApp).repository

    val snackbarHostState = SnackbarHostState()

    // Navigation is deliberately shallow: home -> shift, home -> archived detail.
    val screen = MutableStateFlow<Screen>(Screen.Home)

    // Bumped after import/restore so text fields re-seed from the database.
    val dataGeneration = MutableStateFlow(0)

    val activeShift: StateFlow<Shift?> =
        repo.shiftDao.activeShift()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val archivedShifts: StateFlow<List<Shift>> =
        repo.shiftDao.archivedShifts()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val jobs: StateFlow<List<Job>> =
        activeShift
            .flatMapLatest { shift ->
                if (shift == null) flowOf(emptyList()) else repo.jobDao.forShift(shift.id)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reviews: StateFlow<List<Review>> =
        activeShift
            .flatMapLatest { shift ->
                if (shift == null) flowOf(emptyList()) else repo.reviewDao.forShift(shift.id)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startShift(label: String) =
        viewModelScope.launch {
            repo.startShift(label)
            screen.value = Screen.ActiveShift
        }

    fun archiveActiveShift() =
        viewModelScope.launch {
            activeShift.value?.let { repo.archiveShift(it) }
            screen.value = Screen.Home
        }

    fun addJob() = viewModelScope.launch { activeShift.value?.let { repo.addJob(it.id) } }

    fun updateJob(job: Job) = viewModelScope.launch { repo.updateJob(job) }

    fun completeJob(job: Job) = viewModelScope.launch { repo.completeJob(job) }

    fun reopenJob(job: Job) = viewModelScope.launch { repo.updateJob(job.copy(status = 1)) }

    fun completeReview(review: Review) = viewModelScope.launch { repo.updateReview(review.copy(done = true)) }

    fun reopenReview(review: Review) = viewModelScope.launch { repo.updateReview(review.copy(done = false)) }

    fun setJobTimer(job: Job, endAt: Long?) = viewModelScope.launch { repo.setJobTimer(job, endAt) }

    fun deleteJobWithUndo(job: Job) =
        viewModelScope.launch {
            repo.deleteJob(job)
            undoSnackbar("Job deleted") { repo.restoreJob(job) }
        }

    fun addReview() = viewModelScope.launch { activeShift.value?.let { repo.addReview(it.id) } }

    fun updateReview(review: Review) = viewModelScope.launch { repo.updateReview(review) }

    fun deleteReviewWithUndo(review: Review) =
        viewModelScope.launch {
            repo.deleteReview(review)
            undoSnackbar("Review deleted") { repo.restoreReview(review) }
        }

    fun deleteArchivedShiftWithUndo(shift: Shift) =
        viewModelScope.launch {
            val snapshot = repo.deleteShiftCascade(shift)
            if (screen.value is Screen.ArchiveDetail) screen.value = Screen.Home
            undoSnackbar("Shift deleted") { repo.restoreShiftCascade(snapshot) }
        }

    /**
     * 6-second undo window. Compose's SnackbarHost places the snackbar in the
     * normal composition with real hit-testing (unlike the old web app's
     * pointer-events bug), so the action is reliably tappable.
     */
    private fun undoSnackbar(
        message: String,
        restore: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            val showJob =
                launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = "UNDO",
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        restore()
                        bumpGeneration()
                    }
                }
            launch {
                kotlinx.coroutines.delay(6000)
                // Dismiss at exactly 6 s — but never interrupt a restore that
                // is already running from an UNDO tap.
                if (showJob.isActive) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }

    fun noteCopied() =
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                "DHR note copied — paste into the record",
                duration = SnackbarDuration.Short,
            )
        }

    fun bumpGeneration() {
        dataGeneration.value += 1
    }

    // ---- Archive detail & search ----

    suspend fun archivedShiftDetail(shiftId: String): Triple<Shift?, List<Job>, List<Review>> =
        Triple(
            repo.shiftDao.byId(shiftId),
            repo.jobDao.forShiftOnce(shiftId),
            repo.reviewDao.forShiftOnce(shiftId),
        )

    /** Free-text search across every archived shift's jobs and reviews. */
    suspend fun searchArchived(query: String): List<ArchiveSearchHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val shifts = archivedShifts.value
        val allJobs = repo.jobDao.allOnce().groupBy { it.shiftId }
        val allReviews = repo.reviewDao.allOnce().groupBy { it.shiftId }
        return shifts.mapNotNull { shift ->
            val hits = mutableListOf<String>()
            if (shift.label.lowercase().contains(q)) hits += shift.label
            allJobs[shift.id].orEmpty().forEach { job ->
                listOf(job.text, job.bed).forEach { f ->
                    if (f.lowercase().contains(q)) hits += "Job: $f"
                }
            }
            allReviews[shift.id].orEmpty().forEach { r ->
                listOf(
                    r.bed, r.patientName, r.mrn, r.reason, r.a, r.b, r.c, r.d, r.e,
                    r.investigations, r.impression, r.plan,
                ).forEach { f ->
                    if (f.lowercase().contains(q)) {
                        hits += "Review ${r.patientName.ifBlank { r.bed }}: ${f.take(80)}"
                    }
                }
            }
            if (hits.isEmpty()) null else ArchiveSearchHit(shift, hits.first().take(100))
        }
    }

    // ---- Export / import ----

    fun exportBackup(uri: Uri) =
        viewModelScope.launch {
            val result = repo.backup.exportTo(uri)
            snackbarHostState.showSnackbar(
                result.fold({ "Backup exported" }, { "Export failed: ${it.message}" }),
                duration = SnackbarDuration.Short,
            )
        }

    fun importBackup(uri: Uri) =
        viewModelScope.launch {
            val result = repo.backup.importFrom(uri)
            bumpGeneration()
            snackbarHostState.showSnackbar(
                result.fold({ it }, { "Import failed: ${it.message}" }),
                duration = SnackbarDuration.Long,
            )
        }
}
