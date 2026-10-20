package com.nightshift.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE archived = 0 ORDER BY startedAt DESC LIMIT 1")
    fun activeShift(): Flow<Shift?>

    @Query("SELECT * FROM shifts WHERE archived = 1 ORDER BY archivedAt DESC")
    fun archivedShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun byId(id: String): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM shifts")
    suspend fun allOnce(): List<Shift>
}

@Dao
interface JobDao {
    // Auto-sort: priority first, then status (active before done), then age.
    @Query(
        "SELECT * FROM jobs WHERE shiftId = :shiftId " +
            "ORDER BY CASE WHEN status = 2 THEN 1 ELSE 0 END, priority, createdAt",
    )
    fun forShift(shiftId: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE shiftId = :shiftId ORDER BY priority, createdAt")
    suspend fun forShiftOnce(shiftId: String): List<Job>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun byId(id: String): Job?

    @Query("SELECT * FROM jobs WHERE bedId = :bedId")
    suspend fun forBedOnce(bedId: String): List<Job>

    @Query("SELECT * FROM jobs WHERE timerEndAt IS NOT NULL")
    suspend fun withTimers(): List<Job>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: Job)

    @Update
    suspend fun update(job: Job)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM jobs")
    suspend fun allOnce(): List<Job>

    /**
     * The phrases this user actually writes, most-used first.
     *
     * Grouped case-insensitively on the trimmed text so "Chase bloods" and
     * "chase bloods" are the same habit. The text returned is the most recent
     * spelling of it, which is the one they are used to seeing.
     */
    @Query(
        "SELECT text AS text, COUNT(*) AS uses FROM jobs " +
            "WHERE TRIM(text) <> '' " +
            "GROUP BY LOWER(TRIM(text)) " +
            "ORDER BY uses DESC, MAX(createdAt) DESC " +
            "LIMIT :limit",
    )
    fun topPhrases(limit: Int): Flow<List<PhraseUse>>
}

@Dao
interface BedDao {
    @Query("SELECT * FROM beds WHERE shiftId = :shiftId ORDER BY createdAt")
    fun forShift(shiftId: String): Flow<List<Bed>>

    @Query("SELECT * FROM beds WHERE shiftId = :shiftId ORDER BY createdAt")
    suspend fun forShiftOnce(shiftId: String): List<Bed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bed: Bed)

    @Query("DELETE FROM beds WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM beds")
    suspend fun allOnce(): List<Bed>
}

@Dao
interface WardRoundDao {
    @Query("SELECT * FROM ward_rounds WHERE shiftId = :shiftId ORDER BY priority, createdAt")
    fun forShift(shiftId: String): Flow<List<WardRound>>

    @Query("SELECT * FROM ward_rounds WHERE shiftId = :shiftId ORDER BY priority, createdAt")
    suspend fun forShiftOnce(shiftId: String): List<WardRound>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(round: WardRound)

    @Query("DELETE FROM ward_rounds WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM ward_rounds")
    suspend fun allOnce(): List<WardRound>
}

@Dao
interface ReviewDao {
    @Query(
        "SELECT * FROM reviews WHERE shiftId = :shiftId ORDER BY priority, createdAt",
    )
    fun forShift(shiftId: String): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE shiftId = :shiftId ORDER BY priority, createdAt")
    suspend fun forShiftOnce(shiftId: String): List<Review>

    @Query("SELECT * FROM reviews WHERE remindAt IS NOT NULL")
    suspend fun withReminders(): List<Review>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: Review)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM reviews")
    suspend fun allOnce(): List<Review>
}

@Dao
interface ProcedureDao {
    @Query("SELECT * FROM procedures ORDER BY performedAt DESC")
    fun all(): Flow<List<ProcedureLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(procedure: ProcedureLog)

    @Query("DELETE FROM procedures WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM procedures")
    suspend fun allOnce(): List<ProcedureLog>
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_items ORDER BY answeredAt IS NOT NULL, createdAt DESC")
    fun all(): Flow<List<LearningItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LearningItem)

    @Query("DELETE FROM learning_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM learning_items")
    suspend fun allOnce(): List<LearningItem>
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE ownerId = :ownerId ORDER BY createdAt")
    fun forOwner(ownerId: String): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE ownerId = :ownerId ORDER BY createdAt")
    suspend fun forOwnerOnce(ownerId: String): List<Photo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: Photo)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM photos")
    suspend fun allOnce(): List<Photo>

    /** Which rounds and reviews carry a photo, for the marker on the board. */
    @Query("SELECT DISTINCT ownerId FROM photos")
    fun ownersWithPhotos(): Flow<List<String>>
}
