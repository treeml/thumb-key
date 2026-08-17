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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: Review)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM reviews")
    suspend fun allOnce(): List<Review>
}
