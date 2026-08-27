package com.nightshift.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Priority: 1 = URGENT (red), 2 = SOON (yellow), 3 = ROUTINE (green)
// Status:   0 = not started, 1 = in progress, 2 = done

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: String,
    val label: String,
    val startedAt: Long,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    /** Last time the user tapped "had a break" — drives the wellbeing chip. */
    val lastBreakAt: Long? = null,
    /** Free-text "watch out for" carried into the handover. */
    val handoverNote: String = "",
)

/**
 * A bed on this shift. Beds are created explicitly — you add them one by one
 * at the start, then open one and put jobs under it — so a bed exists even
 * before it has any jobs, which a bed-as-text-label model cannot represent.
 */
@Entity(
    tableName = "beds",
    indices = [Index("shiftId")],
)
data class Bed(
    @PrimaryKey val id: String,
    val shiftId: String,
    val label: String,
    /** Initials or a name — whatever came out of the capture bar. */
    val patientName: String = "",
    val mrn: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "jobs",
    indices = [Index("shiftId")],
)
data class Job(
    @PrimaryKey val id: String,
    val shiftId: String,
    val text: String = "",
    /** Owning bed, when the job belongs to one. Null = unassigned. */
    val bedId: String? = null,
    /** Bed label copied for notes, handover and backups. */
    val bed: String = "",
    val priority: Int = 2,
    val status: Int = 0,
    val createdAt: Long,
    // Absolute epoch-millis deadline; null = no timer. Stored, not in-memory,
    // so timers survive process death and are re-armed after reboot.
    val timerEndAt: Long? = null,
    /**
     * Retired: handwriting capture was removed in 3.2.1 — it went unused.
     *
     * The column stays. Dropping one in SQLite before 3.35 (which minSdk 26
     * predates) means rebuilding the whole jobs table, and risking every job
     * on the device to delete a field nobody reads is a bad trade. Any ink
     * already captured also survives in the backups this way.
     */
    val inkJson: String? = null,
)

@Entity(
    tableName = "reviews",
    indices = [Index("shiftId")],
)
data class Review(
    @PrimaryKey val id: String,
    val shiftId: String,
    val bed: String = "",
    val patientName: String = "",
    val mrn: String = "",
    val reason: String = "",
    val priority: Int = 2,
    val a: String = "",
    val b: String = "",
    val c: String = "",
    val d: String = "",
    val e: String = "",
    val investigations: String = "",
    val impression: String = "",
    val plan: String = "",
    val registrarNotified: Boolean = false,
    // Time-stamped escalation: who, and when. Protects the documentation trail.
    val escalatedTo: String = "",
    val escalatedAt: Long? = null,
    // Done = hidden from the active list, parked in the Completed drawer.
    val done: Boolean = false,
    /** Optional alarm for this patient, e.g. recheck at 04:00. */
    val remindAt: Long? = null,
    /**
     * Which presentation this review was started from, so the card can show
     * that template's differential and workup prompts. The label, not the text:
     * the guidance is app content, not the user's words, and improving it later
     * should improve every review already started from it.
     */
    val templateKey: String = "",
    val createdAt: Long,
)

/**
 * Ward round entry (UroDay flavor's Rounds tab; table exists in both flavors
 * so backups stay interchangeable).
 */
@Entity(
    tableName = "ward_rounds",
    indices = [Index("shiftId")],
)
data class WardRound(
    @PrimaryKey val id: String,
    val shiftId: String,
    val bed: String = "",
    val patientName: String = "",
    val mrn: String = "",
    // Diagnosis / operation and post-op day, e.g. "TURP (POD 1)".
    val dxOp: String = "",
    val overnight: String = "",
    val exam: String = "",
    val results: String = "",
    val plan: String = "",
    val priority: Int = 3,
    // Seen on today's round = parked in the Seen drawer.
    val seen: Boolean = false,
    val createdAt: Long,
)

/**
 * Procedure logbook entry. Deliberately NOT tied to a shift's lifetime —
 * these accumulate across the whole term for the user's portfolio/CPD.
 */
@Entity(tableName = "procedures")
data class ProcedureLog(
    @PrimaryKey val id: String,
    val name: String,
    val supervision: String = SUPERVISION_LEVELS[3],
    val outcome: String = OUTCOMES[0],
    val notes: String = "",
    val shiftId: String? = null,
    val performedAt: Long,
)

val SUPERVISION_LEVELS = listOf("Observed", "Assisted", "Supervised", "Independent", "Taught someone")
val OUTCOMES = listOf("Success", "Difficult", "Failed", "Complication")

/** Common procedures offered as one-tap chips. Free text is always allowed. */
val COMMON_PROCEDURES =
    listOf(
        "IDC insertion (male)",
        "IDC insertion (female)",
        "Bladder washout",
        "3-way catheter",
        "Cannula (IV)",
        "Venepuncture",
        "ABG / VBG",
        "Blood cultures",
        "NG tube",
        "Catheter removal / TOV",
        "Suturing",
        "Wound review / dressing",
        "Lumbar puncture",
        "Ascitic tap",
        "Death certification",
        "Family discussion",
    )

/**
 * A question the user hit on shift and wants to close the loop on later.
 * Answered items become their own flashcards.
 */
@Entity(tableName = "learning_items")
data class LearningItem(
    @PrimaryKey val id: String,
    val question: String,
    val answer: String = "",
    val context: String = "",
    val shiftId: String? = null,
    val createdAt: Long,
    val answeredAt: Long? = null,
    val starred: Boolean = false,
)

/** Whole-database snapshot used for JSON auto-backup, export and import. */
data class BackupPayload(
    val app: String = "nightshift-tracker",
    val schemaVersion: Int = 3,
    val exportedAt: Long,
    val shifts: List<Shift>,
    val jobs: List<Job>,
    val reviews: List<Review>,
    val rounds: List<WardRound> = emptyList(),
    val beds: List<Bed> = emptyList(),
    val procedures: List<ProcedureLog> = emptyList(),
    val learning: List<LearningItem> = emptyList(),
)

/** Full contents of one shift, used for cascade delete/undo and archive view. */
data class ShiftSnapshot(
    val shift: Shift,
    val jobs: List<Job>,
    val reviews: List<Review>,
    val rounds: List<WardRound>,
    val beds: List<Bed> = emptyList(),
)
