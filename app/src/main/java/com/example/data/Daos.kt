package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimitsFlow(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    suspend fun getAllLimits(): List<AppLimit>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimitByPackage(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: AppLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimits(limits: List<AppLimit>)

    @Query("UPDATE app_limits SET usedMinutesToday = :usedMinutes, isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateUsedTimeAndBlock(packageName: String, usedMinutes: Double, isBlocked: Boolean)

    @Query("UPDATE app_limits SET usedMinutesToday = :usedMinutes WHERE packageName = :packageName")
    suspend fun updateUsedTime(packageName: String, usedMinutes: Double)

    @Query("UPDATE app_limits SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)

    @Query("UPDATE app_limits SET limitMinutes = :limitMinutes, isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateLimitSettings(packageName: String, limitMinutes: Int, isEnabled: Boolean)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)
}

@Dao
interface ScrollSessionDao {
    @Query("SELECT * FROM scroll_sessions ORDER BY lastScrollTimestamp DESC")
    fun getSessionsFlow(): Flow<List<ScrollSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScrollSession)

    @Query("DELETE FROM scroll_sessions")
    suspend fun clearSessions()
}

@Dao
interface EmergencyOverrideLogDao {
    @Query("SELECT * FROM emergency_override_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<EmergencyOverrideLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EmergencyOverrideLog)
}

@Dao
interface FocusModeStateDao {
    @Query("SELECT * FROM focus_mode_state WHERE id = 1 LIMIT 1")
    fun getFocusStateFlow(): Flow<FocusModeState?>

    @Query("SELECT * FROM focus_mode_state WHERE id = 1 LIMIT 1")
    suspend fun getFocusState(): FocusModeState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusState(state: FocusModeState)
}

@Dao
interface LockScheduleDao {
    @Query("SELECT * FROM lock_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedulesFlow(): Flow<List<LockSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: LockSchedule)

    @Query("UPDATE lock_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateScheduleEnabled(id: Int, isEnabled: Boolean)

    @Query("DELETE FROM lock_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Int)
}

@Dao
interface DisciplineStreakDao {
    @Query("SELECT * FROM discipline_streaks WHERE id = 1 LIMIT 1")
    fun getStreakFlow(): Flow<DisciplineStreak?>

    @Query("SELECT * FROM discipline_streaks WHERE id = 1 LIMIT 1")
    suspend fun getStreak(): DisciplineStreak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: DisciplineStreak)
}

@Dao
interface UsageHistoryDao {
    @Query("SELECT * FROM usage_history ORDER BY date DESC, minutesUsed DESC")
    fun getHistoryFlow(): Flow<List<UsageHistory>>

    @Query("SELECT * FROM usage_history WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getHistoryByDateRangeFlow(startDate: String, endDate: String): Flow<List<UsageHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: UsageHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<UsageHistory>)
}
