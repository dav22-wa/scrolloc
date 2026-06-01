package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScrollRepository(val db: AppDatabase) {

    val allLimits: Flow<List<AppLimit>> = db.appLimitDao().getAllLimitsFlow()
    val allSessions: Flow<List<ScrollSession>> = db.scrollSessionDao().getSessionsFlow()
    val allLogs: Flow<List<EmergencyOverrideLog>> = db.emergencyOverrideLogDao().getAllLogsFlow()
    val focusState: Flow<FocusModeState?> = db.focusModeStateDao().getFocusStateFlow()
    val allSchedules: Flow<List<LockSchedule>> = db.lockScheduleDao().getAllSchedulesFlow()
    val streak: Flow<DisciplineStreak?> = db.disciplineStreakDao().getStreakFlow()
    val usageHistory: Flow<List<UsageHistory>> = db.usageHistoryDao().getHistoryFlow()

    suspend fun getLimitByPackage(packageName: String): AppLimit? {
        return db.appLimitDao().getLimitByPackage(packageName)
    }

    suspend fun updateUsedTimeAndBlock(packageName: String, usedMinutes: Double, isBlocked: Boolean) {
        db.appLimitDao().updateUsedTimeAndBlock(packageName, usedMinutes, isBlocked)
    }

    suspend fun updateUsedTime(packageName: String, usedMinutes: Double) {
        db.appLimitDao().updateUsedTime(packageName, usedMinutes)
    }

    suspend fun setBlocked(packageName: String, isBlocked: Boolean) {
        db.appLimitDao().setBlocked(packageName, isBlocked)
    }

    suspend fun updateLimitSettings(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        db.appLimitDao().updateLimitSettings(packageName, limitMinutes, isEnabled)
    }

    suspend fun insertLimit(limit: AppLimit) {
        db.appLimitDao().insertLimit(limit)
    }

    suspend fun deleteLimitByPackage(packageName: String) {
        db.appLimitDao().deleteLimit(packageName)
    }

    suspend fun insertSession(session: ScrollSession) {
        db.scrollSessionDao().insertSession(session)
    }

    suspend fun clearSessions() {
        db.scrollSessionDao().clearSessions()
    }

    suspend fun insertOverrideLog(log: EmergencyOverrideLog) {
        db.emergencyOverrideLogDao().insertLog(log)
    }

    suspend fun getFocusState(): FocusModeState? {
        return db.focusModeStateDao().getFocusState()
    }

    suspend fun saveFocusState(state: FocusModeState) {
        db.focusModeStateDao().insertFocusState(state)
    }

    suspend fun insertSchedule(schedule: LockSchedule) {
        db.lockScheduleDao().insertSchedule(schedule)
    }

    suspend fun updateScheduleEnabled(id: Int, isEnabled: Boolean) {
        db.lockScheduleDao().updateScheduleEnabled(id, isEnabled)
    }

    suspend fun deleteSchedule(id: Int) {
        db.lockScheduleDao().deleteSchedule(id)
    }

    suspend fun saveStreak(disciplineStreak: DisciplineStreak) {
        db.disciplineStreakDao().insertStreak(disciplineStreak)
    }

    suspend fun insertHistory(history: UsageHistory) {
        db.usageHistoryDao().insertHistory(history)
    }

    suspend fun seedMockDataIfEmpty() {
        val currentLimits = db.appLimitDao().getAllLimits()
        if (currentLimits.isNotEmpty()) {
            return
        }

        // Seed Limits
        val limits = listOf(
            AppLimit("com.zhiliaoapp.musically", "TikTok", 30, 25.0, false, "Social"),
            AppLimit("com.instagram.android", "Instagram", 20, 18.0, false, "Social"),
            AppLimit("com.facebook.katana", "Facebook", 15, 10.0, false, "Social"),
            AppLimit("com.twitter.android", "X (Twitter)", 20, 8.0, false, "Social"),
            AppLimit("com.google.android.youtube", "YouTube", 35, 30.0, false, "Entertainment"),
            AppLimit("com.google.android.youtube.shorts", "YouTube Shorts", 25, 23.0, false, "Entertainment"),
            AppLimit("com.snapchat.android", "Snapchat", 30, 12.0, false, "Social"),
            AppLimit("com.reddit.frontpage", "Reddit", 20, 5.0, false, "Social"),
            AppLimit("com.linkedin.android", "LinkedIn", 15, 2.0, false, "Business"),
            AppLimit("com.instagram.barcelona", "Threads", 15, 0.0, false, "Social")
        )
        db.appLimitDao().insertLimits(limits)

        // Seed Streak
        db.disciplineStreakDao().insertStreak(
            DisciplineStreak(1, currentStreak = 5, lastActiveDate = "2026-05-31", totalHoursSaved = 12.5, weeklyDisciplineScore = 88)
        )

        // Seed Focus State
        db.focusModeStateDao().insertFocusState(
            FocusModeState(1, isActive = false, endTimestamp = 0, blockedApps = "")
        )

        // Seed Schedules
        db.lockScheduleDao().insertSchedule(LockSchedule(0, "School Hours", 8, 0, 16, 0, true))
        db.lockScheduleDao().insertSchedule(LockSchedule(0, "Sleep Hours", 22, 0, 6, 0, true))

        // Seed history for past 7 days to make charts look awesome
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val histories = mutableListOf<UsageHistory>()

        val apps = listOf(
            Pair("com.zhiliaoapp.musically", "TikTok"),
            Pair("com.instagram.android", "Instagram"),
            Pair("com.facebook.katana", "Facebook"),
            Pair("com.google.android.youtube", "YouTube")
        )

        // Generate historic data
        for (i in 6 downTo 1) {
            cal.time = java.util.Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)

            // Random but structured usages
            val factor = 1.0 - (i * 0.05) // usage decreases as user gets more disciplined
            histories.add(UsageHistory(0, apps[0].first, apps[0].second, dateStr, (40 + (Math.random() * 20)) * factor))
            histories.add(UsageHistory(0, apps[1].first, apps[1].second, dateStr, (30 + (Math.random() * 15)) * factor))
            histories.add(UsageHistory(0, apps[2].first, apps[2].second, dateStr, (20 + (Math.random() * 10)) * factor))
            histories.add(UsageHistory(0, apps[3].first, apps[3].second, dateStr, (50 + (Math.random() * 30)) * factor))
        }

        db.usageHistoryDao().insertHistories(histories)
    }
}
