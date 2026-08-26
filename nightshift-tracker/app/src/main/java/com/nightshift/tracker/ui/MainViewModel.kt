package com.nightshift.tracker.ui

import android.app.Application
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nightshift.tracker.NightshiftApp
import com.nightshift.tracker.ai.AiFactory
import com.nightshift.tracker.ai.AiPrefs
import com.nightshift.tracker.ai.deidentify
import com.nightshift.tracker.data.Bed
import com.nightshift.tracker.data.Job
import com.nightshift.tracker.data.LearningItem
import com.nightshift.tracker.data.ProcedureLog
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.Shift
import com.nightshift.tracker.data.ShiftSnapshot
import com.nightshift.tracker.data.WardRound
import com.nightshift.tracker.ui.capture.parseCapture
import com.nightshift.tracker.ui.handover.buildHandover
import com.nightshift.tracker.ui.reviews.ReviewTemplate
import com.nightshift.tracker.ui.rounds.buildRoundNote
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

    /** Review / tidy / email the notes for the selected beds. */
    data object BatchNotes : Screen

    /** Generated written handover for the oncoming team. */
    data object Handover : Screen

    /** End-of-shift safety net before archiving. */
    data object EndShift : Screen

    /** Procedure logbook + learning questions; lives across shifts. */
    data object Logbook : Screen

    /** Handedness, readability, note tidying. */
    data object Settings : Screen
}

sealed interface AiState {
    data object Idle : AiState

    data object Running : AiState

    data object Done : AiState

    data class Error(val message: String) : AiState
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

    val beds: StateFlow<List<Bed>> =
        activeShift
            .flatMapLatest { shift ->
                if (shift == null) flowOf(emptyList()) else repo.bedDao.forShift(shift.id)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rounds: StateFlow<List<WardRound>> =
        activeShift
            .flatMapLatest { shift ->
                if (shift == null) flowOf(emptyList()) else repo.wardRoundDao.forShift(shift.id)
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

    fun completeReview(review: Review) = viewModelScope.launch { repo.completeReview(review) }

    fun reopenReview(review: Review) = viewModelScope.launch { repo.updateReview(review.copy(done = false)) }

    fun setJobTimer(job: Job, endAt: Long?) = viewModelScope.launch { repo.setJobTimer(job, endAt) }

    fun deleteJobWithUndo(job: Job) =
        viewModelScope.launch {
            repo.deleteJob(job)
            undoSnackbar("Job deleted") { repo.restoreJob(job) }
        }

    fun addReview() = viewModelScope.launch { activeShift.value?.let { repo.addReview(it.id) } }

    /**
     * Starts a review from a presentation template: it fills the reason, the
     * priority and a workup PROMPT list. It never fills findings — a template
     * that pre-writes clinical findings is a lie waiting to be signed.
     */
    fun addReviewFromTemplate(template: ReviewTemplate) =
        viewModelScope.launch {
            val shift = activeShift.value ?: return@launch
            val review = repo.addReview(shift.id)
            repo.updateReview(
                review.copy(
                    reason = template.reason,
                    priority = template.priority,
                    investigations = template.workupPrompt,
                ),
            )
        }

    /** Push a live or expired timer out by [minutes] without retyping anything. */
    fun snoozeJob(job: Job, minutes: Int) =
        viewModelScope.launch {
            val base = maxOf(System.currentTimeMillis(), job.timerEndAt ?: 0L)
            repo.setJobTimer(job, base + minutes * 60_000L)
        }

    fun updateReview(review: Review) = viewModelScope.launch { repo.updateReview(review) }

    fun deleteReviewWithUndo(review: Review) =
        viewModelScope.launch {
            repo.deleteReview(review)
            undoSnackbar("Review deleted") { repo.restoreReview(review) }
        }

    // ---- Multi-bed selection and the batch notes screen ----

    val selectedRoundIds = MutableStateFlow<Set<String>>(emptySet())

    /** The working text on the note screen: raw at first, tidied if asked. */
    val batchText = MutableStateFlow("")

    /** What the note screen is showing, used for its title and email subject. */
    val batchSubject = MutableStateFlow("Notes")
    val aiState = MutableStateFlow<AiState>(AiState.Idle)

    /** Set when text came back from the model; cleared once the user ticks it off. */
    val batchNeedsReview = MutableStateFlow(false)

    fun toggleRoundSelected(id: String) {
        selectedRoundIds.value =
            selectedRoundIds.value.let { if (id in it) it - id else it + id }
    }

    fun selectAllVisibleRounds() {
        selectedRoundIds.value = rounds.value.filter { !it.seen }.map { it.id }.toSet()
    }

    fun clearRoundSelection() {
        selectedRoundIds.value = emptySet()
    }

    private fun selectedRoundsInOrder(): List<WardRound> =
        rounds.value.filter { it.id in selectedRoundIds.value }

    fun openBatchNotes() {
        val selected = selectedRoundsInOrder()
        if (selected.isEmpty()) return
        openNoteReview(
            text = selected.joinToString("\n\n———\n\n") { buildRoundNote(it).trim() },
            subject = "Ward round notes",
        )
    }

    /** Opens any generated note on the review/tidy/send screen. */
    fun openNoteReview(text: String, subject: String) {
        batchText.value = text
        batchSubject.value = subject
        aiState.value = AiState.Idle
        batchNeedsReview.value = false
        screen.value = Screen.BatchNotes
    }

    fun editBatchText(text: String) {
        batchText.value = text
    }

    fun markBatchReviewed() {
        batchNeedsReview.value = false
    }

    fun hasApiKey(): Boolean = AiPrefs.hasKey(getApplication())

    fun setApiKey(value: String) = AiPrefs.setApiKey(getApplication(), value)

    /**
     * Sends de-identified text to Claude and restores identifiers locally.
     * Names, MRNs and bed numbers never leave the device.
     */
    private suspend fun runTidy(text: String): Result<String> {
        val tidier = AiFactory.create(getApplication())
        if (tidier == null) {
            return Result.failure(
                IllegalStateException(
                    if (AiFactory.AVAILABLE) {
                        "Add your Anthropic API key in Settings first."
                    } else {
                        "Note tidying is not available in this build."
                    },
                ),
            )
        }
        val (payload, deidentifier) = deidentify(text, reviews.value, rounds.value)
        return tidier.tidy(payload).map { deidentifier.reidentify(it) }
    }

    fun tidyCurrentNote() =
        viewModelScope.launch {
            if (batchText.value.isBlank()) return@launch
            aiState.value = AiState.Running
            runTidy(batchText.value).fold(
                onSuccess = {
                    batchText.value = it
                    batchNeedsReview.value = true
                    aiState.value = AiState.Done
                },
                onFailure = { aiState.value = AiState.Error(it.message ?: "Request failed.") },
            )
        }

    fun tidyHandover() =
        viewModelScope.launch {
            if (handoverText.value.isBlank()) return@launch
            aiState.value = AiState.Running
            runTidy(handoverText.value).fold(
                onSuccess = {
                    handoverText.value = it
                    batchNeedsReview.value = true
                    aiState.value = AiState.Done
                },
                onFailure = { aiState.value = AiState.Error(it.message ?: "Request failed.") },
            )
        }

    // ---- Archive detail & search ----

    data class ArchiveDetail(
        val shift: Shift?,
        val jobs: List<Job>,
        val reviews: List<Review>,
        val rounds: List<WardRound>,
    )

    suspend fun archivedShiftDetail(shiftId: String): ArchiveDetail =
        ArchiveDetail(
            shift = repo.shiftDao.byId(shiftId),
            jobs = repo.jobDao.forShiftOnce(shiftId),
            reviews = repo.reviewDao.forShiftOnce(shiftId),
            rounds = repo.wardRoundDao.forShiftOnce(shiftId),
        )

    /** Free-text search across every archived shift's jobs and reviews. */
    suspend fun searchArchived(query: String): List<ArchiveSearchHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val shifts = archivedShifts.value
        val allJobs = repo.jobDao.allOnce().groupBy { it.shiftId }
        val allReviews = repo.reviewDao.allOnce().groupBy { it.shiftId }
        val allRounds = repo.wardRoundDao.allOnce().groupBy { it.shiftId }
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
            allRounds[shift.id].orEmpty().forEach { r ->
                listOf(
                    r.bed, r.patientName, r.mrn, r.dxOp, r.overnight, r.exam, r.results, r.plan,
                ).forEach { f ->
                    if (f.lowercase().contains(q)) {
                        hits += "Round ${r.patientName.ifBlank { r.bed }}: ${f.take(80)}"
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

    // ---- Quick capture ----

    /** Which tab the shift screen is showing (0 = Jobs). Held here so other
     *  surfaces — a ward round card, say — can send the user to it. */
    val activeTab = MutableStateFlow(0)

    fun selectTab(index: Int) {
        activeTab.value = index
    }

    /** Text pushed into the capture bar; "" simply focuses it. */
    val captureSeed = MutableStateFlow<String?>(null)

    fun clearCaptureSeed() {
        captureSeed.value = null
    }

    /**
     * The bed currently open on the Jobs tab. Opening a bed IS the target for
     * the fast bar — there is no separate mode to set, because a mode you have
     * to remember is a mode you will get wrong at speed.
     */
    val openBedId = MutableStateFlow<String?>(null)

    fun openBed(bedId: String?) {
        openBedId.value = bedId
        if (bedId != null) captureSeed.value = ""
    }

    fun addBed(label: String) =
        viewModelScope.launch {
            val shift = activeShift.value ?: return@launch
            if (label.isBlank()) return@launch
            val bed = repo.addBed(shift.id, label)
            openBedId.value = bed.id
        }

    fun updateBed(bed: Bed) = viewModelScope.launch { repo.updateBed(bed) }

    fun deleteBedWithUndo(bed: Bed) =
        viewModelScope.launch {
            val orphaned = repo.deleteBed(bed)
            if (openBedId.value == bed.id) openBedId.value = null
            undoSnackbar("Bed deleted — its jobs kept") { repo.restoreBed(bed, orphaned) }
        }

    /** From a ward round card: open (creating if needed) that bed on Jobs. */
    fun startJobForBed(label: String) =
        viewModelScope.launch {
            val shift = activeShift.value ?: return@launch
            val trimmed = label.trim()
            val existing = beds.value.firstOrNull { it.label.equals(trimmed, ignoreCase = true) }
            val bed = existing ?: if (trimmed.isBlank()) null else repo.addBed(shift.id, trimmed)
            openBedId.value = bed?.id
            activeTab.value = 0
            captureSeed.value = ""
        }

    /**
     * One line in, structured job out — or several, since writing up a round
     * comes in clumps. Newlines and semicolons split into separate jobs, so
     * "chase bloods; order CT; call family" is three jobs, not one.
     *
     * Jobs land on whichever bed is open. A line naming its own bed still wins.
     */
    fun captureJob(raw: String) =
        viewModelScope.launch {
            val shift = activeShift.value ?: return@launch
            val current = beds.value.firstOrNull { it.id == openBedId.value }
            val lines = raw.split('\n', ';').map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines) {
                val parsed = parseCapture(line)
                if (parsed.isEmpty) continue
                // A bed typed into the line takes precedence, creating it if new.
                val target =
                    if (parsed.bed.isNotBlank()) {
                        beds.value.firstOrNull { it.label.equals(parsed.bed, ignoreCase = true) }
                            ?: repo.addBed(shift.id, parsed.bed)
                    } else {
                        current
                    }
                val job = repo.addJob(shift.id)
                val withDetail =
                    job.copy(
                        text = parsed.text,
                        bedId = target?.id,
                        bed = target?.label.orEmpty(),
                        priority = parsed.priority,
                    )
                repo.updateJob(withDetail)
                // A clock time ("at 0400") and a countdown ("30m") are the same
                // thing underneath: an absolute deadline with an alarm on it.
                val due =
                    parsed.dueAt
                        ?: parsed.timerMinutes?.let { System.currentTimeMillis() + it * 60_000L }
                if (due != null) repo.setJobTimer(withDetail, due)
            }
        }

    /** Move a single job to another bed (or off the beds entirely). */
    fun moveJobToBed(job: Job, bed: Bed?) =
        viewModelScope.launch {
            repo.updateJob(job.copy(bedId = bed?.id, bed = bed?.label.orEmpty()))
        }

    // ---- Wellbeing ----

    fun recordBreak() =
        viewModelScope.launch {
            activeShift.value?.let {
                repo.recordBreak(it)
                snackbarHostState.showSnackbar("Break logged. Good.", duration = SnackbarDuration.Short)
            }
        }

    // ---- Handover ----

    val handoverText = MutableStateFlow("")

    fun openHandover() {
        val shift = activeShift.value ?: return
        handoverText.value = buildHandover(shift, jobs.value, reviews.value, rounds.value)
        screen.value = Screen.Handover
    }

    fun editHandoverText(text: String) {
        handoverText.value = text
    }

    fun regenerateHandover() {
        val shift = activeShift.value ?: return
        handoverText.value = buildHandover(shift, jobs.value, reviews.value, rounds.value)
    }

    fun setHandoverNote(note: String) =
        viewModelScope.launch {
            activeShift.value?.let { repo.updateShift(it.copy(handoverNote = note)) }
        }

    fun openEndShift() {
        screen.value = Screen.EndShift
    }

    // ---- Escalation (time-stamped, for the documentation trail) ----

    /** An alarm on a review — "chase the gas at 04:00" is the common case. */
    fun setReviewReminder(review: Review, at: Long?) =
        viewModelScope.launch { repo.setReviewReminder(review, at) }

    fun recordEscalation(review: Review, to: String) =
        viewModelScope.launch {
            repo.updateReview(
                review.copy(
                    escalatedTo = to,
                    escalatedAt = System.currentTimeMillis(),
                    registrarNotified = true,
                ),
            )
        }

    fun clearEscalation(review: Review) =
        viewModelScope.launch {
            repo.updateReview(review.copy(escalatedTo = "", escalatedAt = null, registrarNotified = false))
        }

    // ---- Procedure logbook ----

    val procedures: StateFlow<List<ProcedureLog>> =
        repo.procedureDao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun logProcedure(
        name: String,
        supervision: String,
        outcome: String,
        notes: String,
    ) = viewModelScope.launch {
        repo.logProcedure(name, supervision, outcome, notes, activeShift.value?.id)
        snackbarHostState.showSnackbar("Logged: $name", duration = SnackbarDuration.Short)
    }

    fun deleteProcedureWithUndo(entry: ProcedureLog) =
        viewModelScope.launch {
            repo.deleteProcedure(entry)
            undoSnackbar("Logbook entry deleted") { repo.restoreProcedure(entry) }
        }

    // ---- Learning questions ----

    val learning: StateFlow<List<LearningItem>> =
        repo.learningDao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addQuestion(question: String, context: String = "") =
        viewModelScope.launch {
            if (question.isBlank()) return@launch
            repo.addLearning(question.trim(), context, activeShift.value?.id)
            snackbarHostState.showSnackbar("Saved to look up later", duration = SnackbarDuration.Short)
        }

    fun answerQuestion(item: LearningItem, answer: String) =
        viewModelScope.launch {
            repo.updateLearning(
                item.copy(
                    answer = answer,
                    answeredAt = if (answer.isBlank()) null else (item.answeredAt ?: System.currentTimeMillis()),
                ),
            )
        }

    fun toggleQuestionStar(item: LearningItem) =
        viewModelScope.launch { repo.updateLearning(item.copy(starred = !item.starred)) }

    fun deleteQuestionWithUndo(item: LearningItem) =
        viewModelScope.launch {
            repo.deleteLearning(item)
            undoSnackbar("Question deleted") { repo.restoreLearning(item) }
        }

    fun deleteArchivedShiftWithUndo(shift: Shift) =
        viewModelScope.launch {
            val snapshot = repo.deleteShiftCascade(shift)
            if (screen.value is Screen.ArchiveDetail) screen.value = Screen.Home
            undoSnackbar("Shift deleted") { repo.restoreShiftCascade(snapshot) }
        }

    // ---- Ward rounds ----

    fun addRound() = viewModelScope.launch { activeShift.value?.let { repo.addRound(it.id) } }

    fun updateRound(round: WardRound) = viewModelScope.launch { repo.updateRound(round) }

    fun markRoundSeen(round: WardRound) = viewModelScope.launch { repo.updateRound(round.copy(seen = true)) }

    fun reopenRound(round: WardRound) = viewModelScope.launch { repo.updateRound(round.copy(seen = false)) }

    fun deleteRoundWithUndo(round: WardRound) =
        viewModelScope.launch {
            repo.deleteRound(round)
            undoSnackbar("Round entry deleted") { repo.restoreRound(round) }
        }

    // ---- Shared feedback ----

    fun noteCopied() =
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                "Note copied — paste into the record",
                duration = SnackbarDuration.Short,
            )
        }

    fun bumpGeneration() {
        dataGeneration.value += 1
    }

    /**
     * 6-second undo window. Compose's SnackbarHost places the snackbar in the
     * normal composition with real hit-testing, so the action is reliably
     * tappable — the bug that made undo useless in the old web app.
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
}
