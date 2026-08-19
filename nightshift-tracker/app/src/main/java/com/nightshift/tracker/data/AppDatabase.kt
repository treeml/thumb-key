package com.nightshift.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Shift::class,
        Job::class,
        Review::class,
        WardRound::class,
        ProcedureLog::class,
        LearningItem::class,
    ],
    version = 4,
    // Off deliberately: KSP args are global rather than per-flavor, so two
    // flavors exporting schemas in one build race on the same file. Room still
    // validates the hand-written migrations against the entities at runtime.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    abstract fun jobDao(): JobDao

    abstract fun reviewDao(): ReviewDao

    abstract fun wardRoundDao(): WardRoundDao

    abstract fun procedureDao(): ProcedureDao

    abstract fun learningDao(): LearningDao

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

        // v4: break/handover fields, time-stamped escalation, and the two
        // cross-shift tables (procedure logbook, learning questions).
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE shifts ADD COLUMN lastBreakAt INTEGER")
                    db.execSQL("ALTER TABLE shifts ADD COLUMN handoverNote TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE reviews ADD COLUMN escalatedTo TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE reviews ADD COLUMN escalatedAt INTEGER")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `procedures` (" +
                            "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `supervision` TEXT NOT NULL, " +
                            "`outcome` TEXT NOT NULL, `notes` TEXT NOT NULL, `shiftId` TEXT, " +
                            "`performedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `learning_items` (" +
                            "`id` TEXT NOT NULL, `question` TEXT NOT NULL, `answer` TEXT NOT NULL, " +
                            "`context` TEXT NOT NULL, `shiftId` TEXT, `createdAt` INTEGER NOT NULL, " +
                            "`answeredAt` INTEGER, `starred` INTEGER NOT NULL, PRIMARY KEY(`id`))",
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // No destructive fallback — an app update must never wipe data.
                    .build()
                    .also { instance = it }
            }
    }
}
