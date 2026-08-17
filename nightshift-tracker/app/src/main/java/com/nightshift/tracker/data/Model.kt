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
)

@Entity(
    tableName = "jobs",
    indices = [Index("shiftId")],
)
data class Job(
    @PrimaryKey val id: String,
    val shiftId: String,
    val text: String = "",
    val bed: String = "",
    val priority: Int = 2,
    val status: Int = 0,
    val createdAt: Long,
    // Absolute epoch-millis deadline; null = no timer. Stored, not in-memory,
    // so timers survive process death and are re-armed after reboot.
    val timerEndAt: Long? = null,
    // S Pen ink, serialized as JSON [[{x,y},...],...]; null = typed-only job.
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
    // Done = hidden from the active list, parked in the Completed drawer.
    val done: Boolean = false,
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

/** Whole-database snapshot used for JSON auto-backup, export and import. */
data class BackupPayload(
    val app: String = "nightshift-tracker",
    val schemaVersion: Int = 2,
    val exportedAt: Long,
    val shifts: List<Shift>,
    val jobs: List<Job>,
    val reviews: List<Review>,
    val rounds: List<WardRound> = emptyList(),
)

/** Full contents of one shift, used for cascade delete/undo and archive view. */
data class ShiftSnapshot(
    val shift: Shift,
    val jobs: List<Job>,
    val reviews: List<Review>,
    val rounds: List<WardRound>,
)
