package com.nightshift.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Shift::class, Job::class, Review::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    abstract fun jobDao(): JobDao

    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "nightshift.db",
                    )
                    // WAL: atomic, crash-safe commits; readers never block the writer.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    // No destructive fallback — an app update must never wipe data.
                    // Future schema changes get explicit Migration objects here.
                    .build()
                    .also { instance = it }
            }
    }
}
