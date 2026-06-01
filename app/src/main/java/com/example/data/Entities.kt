package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val usedMinutesToday: Double,
    val isBlocked: Boolean = false,
    val category: String = "Social",
    val isEnabled: Boolean = true
)

@Entity(tableName = "scroll_sessions")
data class ScrollSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val scrollDurationMinutes: Int,
    val lastScrollTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_override_logs")
data class EmergencyOverrideLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_mode_state")
data class FocusModeState(
    @PrimaryKey val id: Int = 1,
    val isActive: Boolean = false,
    val endTimestamp: Long = 0,
    val blockedApps: String = "" // comma-separated packageNames
)

@Entity(tableName = "lock_schedules")
data class LockSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean = true
)

@Entity(tableName = "discipline_streaks")
data class DisciplineStreak(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val lastActiveDate: String = "",
    val totalHoursSaved: Double = 0.0,
    val weeklyDisciplineScore: Int = 0
)

@Entity(tableName = "usage_history")
data class UsageHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val date: String, // YYYY-MM-DD
    val minutesUsed: Double
)
