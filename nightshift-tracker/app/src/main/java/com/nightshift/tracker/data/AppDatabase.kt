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
        Bed::class,
        ProcedureLog::class,
        LearningItem::class,
    ],
    version = 6,
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

    abstract fun bedDao(): BedDao

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

        // v5: beds become real rows rather than a text label on each job, so a
        // bed can be created before it has any jobs. Existing jobs keep their
        // label and are back-filled onto generated bed rows — nothing is lost.
        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `beds` (" +
                            "`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                            "`patientName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_beds_shiftId` ON `beds` (`shiftId`)")
                    db.execSQL("ALTER TABLE jobs ADD COLUMN bedId TEXT")
                    db.execSQL(
                        "INSERT INTO beds (id, shiftId, label, patientName, createdAt) " +
                            "SELECT DISTINCT shiftId || '::' || TRIM(bed), shiftId, TRIM(bed), '', " +
                            "MIN(createdAt) FROM jobs WHERE TRIM(bed) <> '' GROUP BY shiftId, TRIM(bed)",
                    )
                    db.execSQL(
                        "UPDATE jobs SET bedId = shiftId || '::' || TRIM(bed) WHERE TRIM(bed) <> ''",
                    )
                }
            }

        // v6: reviews can carry their own alarm.
        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE reviews ADD COLUMN remindAt INTEGER")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // No destructive fallback — an app update must never wipe data.
                    .build()
                    .also { instance = it }
            }
    }
}
