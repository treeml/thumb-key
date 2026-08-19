package com.nightshift.tracker.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a full-database JSON snapshot after every committed save.
 *
 * Bursts of writes (typing) coalesce into one snapshot ~1.5 s after the last
 * write — the Room commit itself is still immediate and per-keystroke; only
 * the redundant re-serialisation is coalesced. Snapshots are written
 * atomically (temp file + rename) to both filesDir/backups and the
 * app-specific external dir, with the latest plus 10 rotating copies.
 * Neither location is subject to cache eviction.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope,
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val kick = Channel<Unit>(Channel.CONFLATED)
    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    init {
        scope.launch(Dispatchers.IO) {
            for (unused in kick) {
                delay(1500) // coalesce bursts; CONFLATED channel merges kicks
                runCatching { writeSnapshot() }
            }
        }
    }

    /** Call after every successful mutation. Never blocks the caller. */
    fun scheduleBackup() {
        kick.trySend(Unit)
    }

    suspend fun currentJson(): String {
        val payload =
            BackupPayload(
                exportedAt = System.currentTimeMillis(),
                shifts = db.shiftDao().allOnce(),
                jobs = db.jobDao().allOnce(),
                reviews = db.reviewDao().allOnce(),
                rounds = db.wardRoundDao().allOnce(),
                beds = db.bedDao().allOnce(),
                procedures = db.procedureDao().allOnce(),
                learning = db.learningDao().allOnce(),
            )
        return gson.toJson(payload)
    }

    private suspend fun writeSnapshot() {
        val json = currentJson()
        for (dir in backupDirs()) {
            atomicWrite(File(dir, "nightshift-backup-latest.json"), json)
            atomicWrite(File(dir, "nightshift-backup-${stamp.format(Date())}.json"), json)
            rotate(dir)
        }
    }

    private fun backupDirs(): List<File> =
        buildList {
            add(File(context.filesDir, "backups").apply { mkdirs() })
            context.getExternalFilesDir(null)?.let {
                add(File(it, "backups").apply { mkdirs() })
            }
        }

    private fun atomicWrite(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            // Rename across a stale target can fail on some filesystems.
            target.delete()
            tmp.renameTo(target)
        }
    }

    private fun rotate(dir: File) {
        val dated =
            dir
                .listFiles { f -> f.name.startsWith("nightshift-backup-2") && f.extension == "json" }
                ?.sortedByDescending { it.name }
                .orEmpty()
        dated.drop(10).forEach { it.delete() }
    }

    /** Manual export to a user-chosen SAF location (Downloads, Drive folder, SD). */
    suspend fun exportTo(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = currentJson()
                context.contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open destination")
            }
        }

    /**
     * Import a backup file, replacing current data. A safety snapshot of the
     * pre-import state is written first, so a bad import is itself undoable
     * from the backups folder.
     */
    suspend fun importFrom(uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text =
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Could not read file")
                val payload = gson.fromJson(text, BackupPayload::class.java)
                require(payload != null && payload.app == "nightshift-tracker") {
                    "Not a Nightshift Tracker backup file"
                }
                // Gson can leave declared-non-null lists null on malformed input.
                @Suppress("USELESS_ELVIS")
                val shifts: List<Shift> = payload.shifts ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val jobs: List<Job> = payload.jobs ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val reviews: List<Review> = payload.reviews ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val rounds: List<WardRound> = payload.rounds ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val beds: List<Bed> = payload.beds ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val procedures: List<ProcedureLog> = payload.procedures ?: emptyList()

                @Suppress("USELESS_ELVIS")
                val learning: List<LearningItem> = payload.learning ?: emptyList()

                // Safety net before we touch anything.
                for (dir in backupDirs()) {
                    atomicWrite(
                        File(dir, "nightshift-preimport-${stamp.format(Date())}.json"),
                        currentJson(),
                    )
                }

                db.runInTransaction {
                    // runInTransaction is not suspend-friendly; use raw deletes.
                    db.openHelper.writableDatabase.execSQL("DELETE FROM learning_items")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM procedures")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM beds")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM ward_rounds")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM reviews")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM jobs")
                    db.openHelper.writableDatabase.execSQL("DELETE FROM shifts")
                }
                shifts.forEach { db.shiftDao().upsert(it) }
                jobs.forEach { db.jobDao().upsert(it) }
                reviews.forEach { db.reviewDao().upsert(it) }
                beds.forEach { db.bedDao().upsert(it) }
                rounds.forEach { db.wardRoundDao().upsert(it) }
                procedures.forEach { db.procedureDao().upsert(it) }
                learning.forEach { db.learningDao().upsert(it) }
                scheduleBackup()
                "Restored ${shifts.size} shifts, ${jobs.size} jobs, ${reviews.size} reviews, " +
                    "${rounds.size} rounds, ${procedures.size} procedures, ${learning.size} questions"
            }
        }
}
