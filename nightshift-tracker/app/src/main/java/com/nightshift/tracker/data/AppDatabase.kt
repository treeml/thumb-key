package com.nightshift.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Shift::class, Job::class, Review::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    abstract fun jobDao(): JobDao

    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // v2: reviews gain a 'done' flag (Completed drawer). Additive only —
        // existing rows default to not-done, nothing is dropped.
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE reviews ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
                }
            }

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
                    .addMigrations(MIGRATION_1_2)
                    // No destructive fallback — an app update must never wipe data.
                    // Future schema changes get explicit Migration objects here.
                    .build()
                    .also { instance = it }
            }
    }
}
