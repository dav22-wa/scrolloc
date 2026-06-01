package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppLimit::class,
        ScrollSession::class,
        EmergencyOverrideLog::class,
        FocusModeState::class,
        LockSchedule::class,
        DisciplineStreak::class,
        UsageHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun scrollSessionDao(): ScrollSessionDao
    abstract fun emergencyOverrideLogDao(): EmergencyOverrideLogDao
    abstract fun focusModeStateDao(): FocusModeStateDao
    abstract fun lockScheduleDao(): LockScheduleDao
    abstract fun disciplineStreakDao(): DisciplineStreakDao
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scrolllock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
