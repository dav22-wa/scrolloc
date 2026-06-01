package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppLimit
import com.example.data.DisciplineStreak
import com.example.data.FocusModeState
import com.example.data.GeminiService
import com.example.data.LockSchedule
import com.example.data.ScrollRepository
import com.example.service.ScrollLockManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollViewModel(application: Application) : AndroidViewModel(application) {

    val repository: ScrollRepository
    
    // Core states retrieved from Repository Flows
    val appLimits: StateFlow<List<AppLimit>>
    val schedules: StateFlow<List<LockSchedule>>
    val overrideLogs: StateFlow<List<com.example.data.EmergencyOverrideLog>>
    val focusState: StateFlow<FocusModeState?>
    val streak: StateFlow<DisciplineStreak?>
    val history: StateFlow<List<com.example.data.UsageHistory>>

    // UI state parameters
    private val _currentTab = MutableStateFlow("dashboard")
    val currentTab = _currentTab.asStateFlow()

    private val _isFastSimulation = MutableStateFlow(true) // default true for immediate testing
    val isFastSimulation = _isFastSimulation.asStateFlow()

    private val _isPremium = MutableStateFlow(false) // default false, toggleable for full feature testing
    val isPremium = _isPremium.asStateFlow()

    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights = _aiInsights.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // Tracking variables for timer
    private var tickerJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ScrollRepository(db)

        appLimits = repository.allLimits.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        schedules = repository.allSchedules.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        overrideLogs = repository.allLogs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        focusState = repository.focusState.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        streak = repository.streak.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        history = repository.usageHistory.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Seed default limits and data
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
        }

        // Start background simulated ticker
        startTimer()
    }

    private fun startTimer() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                // If fast simulation is on, 1 second = 1.0 minute usage increments. Otherwise, 1 second = 1/60th of a minute.
                val elapsed = if (_isFastSimulation.value) 1.0 else (1.0 / 60.0)
                ScrollLockManager.tickUsage(repository, elapsed, _isFastSimulation.value)
            }
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun toggleFastSimulation(enabled: Boolean) {
        _isFastSimulation.value = enabled
    }

    fun togglePremium(enabled: Boolean) {
        _isPremium.value = enabled
    }

    // Direct interface to Simulator State
    fun simulateLaunchApp(packageName: String, appName: String) {
        ScrollLockManager.startSimulateApp(packageName, appName)
    }

    fun simulateExitApp() {
        ScrollLockManager.stopSimulateApp()
    }

    fun simulateSetScrolling(scrolling: Boolean) {
        ScrollLockManager.setScrolling(scrolling)
    }

    fun dismissIntervention() {
        ScrollLockManager.dismissIntervention()
    }

    fun dismissLockOverlay() {
        ScrollLockManager.dismissLockOverlay()
    }

    fun triggerEmergencyUnlock(packageName: String, appName: String, reason: String) {
        viewModelScope.launch {
            ScrollLockManager.performEmergencyUnlock(repository, packageName, appName, reason)
        }
    }

    // Database actions
    fun updateLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateLimitSettings(packageName, limitMinutes, isEnabled)
        }
    }

    fun addCustomLimit(packageName: String, appName: String, limitMinutes: Int, category: String) {
        viewModelScope.launch {
            val limit = AppLimit(packageName, appName, limitMinutes, 0.0, false, category, true)
            repository.insertLimit(limit)
        }
    }

    fun deleteLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimitByPackage(packageName)
        }
    }

    fun activateFocusMode(durationHours: Int, selectedApps: List<String>) {
        viewModelScope.launch {
            val endTimestamp = System.currentTimeMillis() + (durationHours * 3600 * 1000)
            val appsCommaString = selectedApps.joinToString(",")
            val state = FocusModeState(1, isActive = true, endTimestamp = endTimestamp, blockedApps = appsCommaString)
            repository.saveFocusState(state)
            
            // Add notification alert
            ScrollLockManager.addNotification("🎯 Focus Mode activated for $durationHours hours on ${selectedApps.size} apps.")
        }
    }

    fun deactivateFocusMode() {
        viewModelScope.launch {
            val state = FocusModeState(1, isActive = false, endTimestamp = 0, blockedApps = "")
            repository.saveFocusState(state)
            ScrollLockManager.addNotification("🕊️ Focus Mode deactivated.")
        }
    }

    fun addSchedules(name: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            val sched = LockSchedule(0, name, startHour, startMinute, endHour, endMinute, true)
            repository.insertSchedule(sched)
            ScrollLockManager.addNotification("📅 Schedule '$name' added ($startHour:$startMinute to $endHour:$endMinute).")
        }
    }

    fun toggleSchedule(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateScheduleEnabled(id, enabled)
        }
    }

    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    fun resetDailyUsage() {
        viewModelScope.launch {
            val limits = appLimits.value
            for (limit in limits) {
                repository.updateUsedTimeAndBlock(limit.packageName, 0.0, false)
            }
            ScrollLockManager.addNotification("♻️ Daily limits usage reset successfully.")
        }
    }

    fun fetchAIInsights() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiInsights.value = null
            try {
                val limitsList = appLimits.value
                val streakVal = streak.value
                val overridesVal = overrideLogs.value
                val res = GeminiService.getAdditionInsights(limitsList, streakVal, overridesVal)
                _aiInsights.value = res
            } catch (e: Exception) {
                _aiInsights.value = "Error generating insights: ${e.localizedMessage}. Please double check network state and key settings."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
