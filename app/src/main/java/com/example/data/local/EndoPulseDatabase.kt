package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyLogEntity::class,
        CustomHabitEntity::class,
        CustomHabitCompletionEntity::class,
        MilestoneRewardEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EndoPulseDatabase : RoomDatabase() {
    abstract fun endoPulseDao(): EndoPulseDao

    companion object {
        @Volatile
        private var INSTANCE: EndoPulseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN bedtimeMinutes INTEGER NOT NULL DEFAULT 1380")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN wakeTimeMinutes INTEGER NOT NULL DEFAULT 420")
                db.execSQL("ALTER TABLE custom_habits ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE custom_habits ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE custom_habits ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN consistentWindDown INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN morningLightWithin60Mins INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN hasCaffeine INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN lastCaffeineMinutes INTEGER NOT NULL DEFAULT 600")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN dopamineFrictionRatio REAL NOT NULL DEFAULT 2.0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN overtrainingFlag INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN coinsEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN rewardMultiplierLabel TEXT NOT NULL DEFAULT '1.0x Base'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS milestone_rewards (
                        milestoneId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        coinsAwarded INTEGER NOT NULL,
                        unlockedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): EndoPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EndoPulseDatabase::class.java,
                    "endopulse_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
