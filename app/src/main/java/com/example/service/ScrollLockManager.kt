package com.example.service

import com.example.data.AppLimit
import com.example.data.EmergencyOverrideLog
import com.example.data.ScrollRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

object ScrollLockManager {
    private const val TAG = "ScrollLockManager"

    // Simulation & Live State
    private val _foregroundApp = MutableStateFlow<String?>(null)
    val foregroundApp = _foregroundApp.asStateFlow()

    private val _lockOverlayApp = MutableStateFlow<AppLimit?>(null)
    val lockOverlayApp = _lockOverlayApp.asStateFlow()

    private val _isLockOverlayActive = MutableStateFlow(false)
    val isLockOverlayActive = _isLockOverlayActive.asStateFlow()

    private val _continuousScrollMinutes = MutableStateFlow(0.0)
    val continuousScrollMinutes = _continuousScrollMinutes.asStateFlow()

    private val _isScrolling = MutableStateFlow(false)
    val isScrolling = _isScrolling.asStateFlow()

    private val _interventionMessage = MutableStateFlow<String?>(null)
    val interventionMessage = _interventionMessage.asStateFlow()

    // Notification Feed
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications = _notifications.asStateFlow()

    fun addNotification(message: String) {
        _notifications.value = (listOf(message) + _notifications.value).take(10)
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // Call this under a timer (e.g., every 1 second representing 1 minute in "Fast Simulation Mode" or 1 real second in Standard Mode)
    suspend fun tickUsage(repository: ScrollRepository, elapsedMinutes: Double, isFastSimulation: Boolean = false) {
        val appPkg = _foregroundApp.value ?: return
        val limitObj = repository.getLimitByPackage(appPkg) ?: return

        if (!limitObj.isEnabled) return

        // 1. Check Schedules Custom Blocks
        if (checkActiveSchedules(repository)) {
            triggerLockOverlay(limitObj.copy(usedMinutesToday = limitObj.usedMinutesToday + elapsedMinutes, isBlocked = true))
            return
        }

        // 2. Check Focus Mode Block
        val focus = repository.getFocusState()
        if (focus != null && focus.isActive) {
            val blockedPkgs = focus.blockedApps.split(",")
            if (focus.endTimestamp > System.currentTimeMillis() && blockedPkgs.contains(appPkg)) {
                triggerLockOverlay(limitObj.copy(usedMinutesToday = limitObj.usedMinutesToday + elapsedMinutes, isBlocked = true))
                return
            }
        }

        // Increment time
        val newTime = limitObj.usedMinutesToday + elapsedMinutes
        val limitReached = newTime >= limitObj.limitMinutes

        repository.updateUsedTimeAndBlock(appPkg, newTime, limitReached)

        // Notification thresholds
        val remaining = limitObj.limitMinutes - newTime
        if (remaining in 4.9..5.1 || (remaining < 5.0 && remaining + elapsedMinutes >= 5.0)) {
            addNotification("⚠️ 5 minutes remaining. ${limitObj.appName} will lock soon!")
        } else if (remaining in 0.9..1.1 || (remaining < 1.0 && remaining + elapsedMinutes >= 1.0)) {
            addNotification("⚠️ 1 minute remaining. ${limitObj.appName} is close to locking!")
        }

        if (limitReached) {
            repository.setBlocked(appPkg, true)
            addNotification("🔒 ${limitObj.appName} has been locked for today.")
            triggerLockOverlay(limitObj.copy(usedMinutesToday = newTime, isBlocked = true))
        } else {
            // Track scrolling
            if (_isScrolling.value) {
                val newScroll = _continuousScrollMinutes.value + elapsedMinutes
                _continuousScrollMinutes.value = newScroll

                // Check intervention intervals (20, 30, 45 or simplified mock scale)
                val checkInterval = if (isFastSimulation) 5.0 else 10.0 // faster warnings in simulator
                if (newScroll >= checkInterval && (newScroll - elapsedMinutes) < checkInterval) {
                    _interventionMessage.value = "⚠️ Mindful Scroll Notice!\nYou have been scrolling Instagram/TikTok continuously for ${String.format("%.1f", newScroll)} minutes. Would you like to take a break?"
                    addNotification("🧘 Mindful Intervene: Scroll warning shown for ${limitObj.appName}.")
                }
            }
        }
    }

    private suspend fun checkActiveSchedules(repository: ScrollRepository): Boolean {
        val schedules = repository.db.lockScheduleDao().getAllSchedulesFlow()
        // Check active lock blocks based on current hour/minute
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        var isScheduleActive = false
        repository.allSchedules.collect { list ->
            for (sched in list) {
                if (sched.isEnabled) {
                    val startMin = sched.startHour * 60 + sched.startMinute
                    val endMin = sched.endHour * 60 + sched.endMinute
                    val nowMin = hour * 60 + minute
                    if (startMin < endMin) {
                        if (nowMin in startMin..endMin) {
                            isScheduleActive = true
                            break
                        }
                    } else {
                        // Spans midnight (e.g. 22:00 to 06:00)
                        if (nowMin >= startMin || nowMin <= endMin) {
                            isScheduleActive = true
                            break
                        }
                    }
                }
            }
        }
        return isScheduleActive
    }

    fun startSimulateApp(packageName: String, appName: String) {
        _foregroundApp.value = packageName
        _continuousScrollMinutes.value = 0.0
        _isScrolling.value = false
        _interventionMessage.value = null
        addNotification("📱 Launched app: $appName")
    }

    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
        if (!scrolling) {
            _continuousScrollMinutes.value = 0.0
        }
    }

    fun stopSimulateApp() {
        _foregroundApp.value = null
        _continuousScrollMinutes.value = 0.0
        _isScrolling.value = false
        _interventionMessage.value = null
        addNotification("🏠 Returned to home screen.")
    }

    fun dismissIntervention() {
        _interventionMessage.value = null
    }

    private fun triggerLockOverlay(appLimit: AppLimit) {
        _lockOverlayApp.value = appLimit
        _isLockOverlayActive.value = true
        // Minimize active simulation
        _foregroundApp.value = null
        _isScrolling.value = false
        _continuousScrollMinutes.value = 0.0
    }

    fun dismissLockOverlay() {
        _isLockOverlayActive.value = false
        _lockOverlayApp.value = null
    }

    suspend fun performEmergencyUnlock(
        repository: ScrollRepository,
        packageName: String,
        appName: String,
        reason: String
    ): Boolean {
        // Find existing limit
        val limit = repository.getLimitByPackage(packageName) ?: return false
        
        // Log override
        repository.insertOverrideLog(
            EmergencyOverrideLog(0, packageName, appName, reason, System.currentTimeMillis())
        )

        // Reset usage today, or extend limit by 15 mins
        val extendedLimit = limit.limitMinutes + 15
        repository.updateLimitSettings(packageName, extendedLimit, true)
        repository.updateUsedTimeAndBlock(packageName, limit.usedMinutesToday, false)
        
        addNotification("🔓 Emergency Unlock applied for $appName. Limit extended by 15 mins.")
        dismissLockOverlay()
        return true
    }
}
