package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ScrollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScrollAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: ScrollRepository

    companion object {
        private const val TAG = "ScrollAccessibility"
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = ScrollRepository(db)
        Log.d(TAG, "ScrollAccessibilityService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Detect app launch or window state changed
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d(TAG, "Window changed to: $packageName")
            
            // Only capture standard third party targets we monitor
            val targetPackages = listOf(
                "com.zhiliaoapp.musically", // TikTok
                "com.instagram.android",    // Instagram
                "com.facebook.katana",      // Facebook
                "com.twitter.android",      // X Twitter
                "com.google.android.youtube", // YouTube
                "com.snapchat.android",     // Snapchat
                "com.reddit.frontpage",     // Reddit
                "com.linkedin.android",     // LinkedIn
                "com.instagram.barcelona"   // Threads
            )

            if (targetPackages.contains(packageName)) {
                serviceScope.launch {
                    val limit = repository.getLimitByPackage(packageName)
                    if (limit != null && limit.isEnabled) {
                        // If app exceeds limit, immediately force block
                        if (limit.usedMinutesToday >= limit.limitMinutes || limit.isBlocked) {
                            Log.d(TAG, "App locked! Forcing close: $packageName")
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            
                            // Pop up our lock screen
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("TRIGGER_LOCK_OVERLAY", packageName)
                            }
                            startActivity(intent)
                        } else {
                            // Track in ScrollLockManager
                            ScrollLockManager.startSimulateApp(packageName, limit.appName)
                        }
                    }
                }
            } else {
                // If exiting monitored app or returning home, stop simulation
                if (packageName == "com.android.launcher" || packageName == "com.google.android.apps.nexuslauncher") {
                    ScrollLockManager.stopSimulateApp()
                }
            }
        }

        // 2. Continuous stream / Scroll gestures tracking (e.g., event scroll)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val packageName = event.packageName?.toString() ?: return
            if (ScrollLockManager.foregroundApp.value == packageName) {
                ScrollLockManager.setScrolling(true)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "ScrollAccessibilityService interrupted")
    }
}
