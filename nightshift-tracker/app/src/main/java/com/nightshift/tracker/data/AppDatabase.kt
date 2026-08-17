package com.nightshift.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Shift::class, Job::class, Review::class, WardRound::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    abstract fun jobDao(): JobDao

    abstract fun reviewDao(): ReviewDao

    abstract fun wardRoundDao(): WardRoundDao

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

        // v3: ward_rounds table (UroDay Rounds tab). Purely additive.
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `ward_rounds` (" +
                            "`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `bed` TEXT NOT NULL, " +
                            "`patientName` TEXT NOT NULL, `mrn` TEXT NOT NULL, `dxOp` TEXT NOT NULL, " +
                            "`overnight` TEXT NOT NULL, `exam` TEXT NOT NULL, `results` TEXT NOT NULL, " +
                            "`plan` TEXT NOT NULL, `priority` INTEGER NOT NULL, `seen` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_ward_rounds_shiftId` ON `ward_rounds` (`shiftId`)",
                    )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // No destructive fallback — an app update must never wipe data.
                    // Future schema changes get explicit Migration objects here.
                    .build()
                    .also { instance = it }
            }
    }
}
