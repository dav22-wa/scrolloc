package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLimit
import com.example.data.FocusModeState
import com.example.data.LockSchedule
import com.example.data.UsageHistory
import androidx.compose.ui.draw.shadow
import com.example.service.ScrollLockManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScrollViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(
    viewModel: ScrollViewModel,
    modifier: Modifier = Modifier
) {
    // Observe state from ViewModel
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val appLimits by viewModel.appLimits.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val overrideLogs by viewModel.overrideLogs.collectAsStateWithLifecycle()
    val focusState by viewModel.focusState.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val isFastSim by viewModel.isFastSimulation.collectAsStateWithLifecycle()
    val isPremiumActive by viewModel.isPremium.collectAsStateWithLifecycle()
    val aiInsightsText by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    // Observe active overlays in real time
    val simulatedForegroundApp by ScrollLockManager.foregroundApp.collectAsStateWithLifecycle()
    val lockOverlayApp by ScrollLockManager.lockOverlayApp.collectAsStateWithLifecycle()
    val isLockActive by ScrollLockManager.isLockOverlayActive.collectAsStateWithLifecycle()
    val isScrolling by ScrollLockManager.isScrolling.collectAsStateWithLifecycle()
    val scrollMinutes by ScrollLockManager.continuousScrollMinutes.collectAsStateWithLifecycle()
    val interventionMsg by ScrollLockManager.interventionMessage.collectAsStateWithLifecycle()
    val notificationFeed by ScrollLockManager.notifications.collectAsStateWithLifecycle()

    // Local state for UI forms
    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var showEmergencyUnlockReasonDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Standard Navigation Bar (Material 3)
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                listOf(
                    Triple("dashboard", Icons.Outlined.Dashboard, "Dashboard"),
                    Triple("limits", Icons.Outlined.HourglassEmpty, "Limits"),
                    Triple("focus", Icons.Outlined.OfflineBolt, "Focus"),
                    Triple("schedules", Icons.Outlined.CalendarMonth, "Schedules"),
                    Triple("analytics", Icons.Outlined.BarChart, "Analytics"),
                    Triple("ai_advisor", Icons.Outlined.Psychology, "AI Coach"),
                    Triple("simulator", Icons.Outlined.PhonelinkSetup, "Simulator")
                ).forEach { (tabId, icon, label) ->
                    val selected = currentTab == tabId
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tabId) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) TechBlue else TextGray
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) IceWhite else TextGray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = TechBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_$tabId")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views routing
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "tab_animation"
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardView(
                        viewModel = viewModel,
                        limits = appLimits,
                        streak = streak,
                        logs = overrideLogs,
                        notifications = notificationFeed,
                        onReset = { viewModel.resetDailyUsage() }
                    )
                    "limits" -> LimitsView(
                        limits = appLimits,
                        isPremium = isPremiumActive,
                        onUpdate = { pkg, min, enabled -> viewModel.updateLimit(pkg, min, enabled) },
                        onDelete = { pkg -> viewModel.deleteLimit(pkg) },
                        onAddCustomClick = { showAddCustomAppDialog = true },
                        onUpgradeClick = { viewModel.togglePremium(true) }
                    )
                    "focus" -> FocusModeView(
                        limits = appLimits,
                        focusState = focusState,
                        onActivate = { duration, apps -> viewModel.activateFocusMode(duration, apps) },
                        onDeactivate = { viewModel.deactivateFocusMode() }
                    )
                    "schedules" -> SchedulesView(
                        schedules = schedules,
                        isPremium = isPremiumActive,
                        onAdd = { name, sh, sm, eh, em -> viewModel.addSchedules(name, sh, sm, eh, em) },
                        onToggle = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                        onDelete = { id -> viewModel.deleteSchedule(id) },
                        onUpgradeClick = { viewModel.togglePremium(true) }
                    )
                    "analytics" -> AnalyticsView(
                        history = history,
                        limits = appLimits,
                        streak = streak
                    )
                    "ai_advisor" -> AiAdvisorView(
                        insights = aiInsightsText,
                        isLoading = isAiLoading,
                        isPremium = isPremiumActive,
                        onGenerate = { viewModel.fetchAIInsights() },
                        onUpgradeClick = { viewModel.togglePremium(true) }
                    )
                    "simulator" -> SimulatorConsoleView(
                        viewModel = viewModel,
                        limits = appLimits,
                        isFastSim = isFastSim,
                        simulatedApp = simulatedForegroundApp,
                        isScrolling = isScrolling,
                        scrollMinutes = scrollMinutes,
                        onToggleFast = { viewModel.toggleFastSimulation(it) },
                        onTogglePremium = { viewModel.togglePremium(it) },
                        onLaunch = { pkg, name -> viewModel.simulateLaunchApp(pkg, name) },
                        onExit = { viewModel.simulateExitApp() },
                        onSetScroll = { viewModel.simulateSetScrolling(it) }
                    )
                }
            }

            // 1. GLOBAL INTERVENTION POPUP OVERLAY
            interventionMsg?.let { msg ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    elevation = CardDefaults.cardElevation(16.dp),
                    border = BorderStroke(1.5.dp, AlertAmber.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(AlertAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Intervention Warning",
                                tint = AlertAmber,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SMART SCROLL INTERRUPTION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertAmber,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = msg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = IceWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.dismissIntervention() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IceWhite),
                                border = BorderStroke(1.dp, TextGray.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("intervention_continue_button")
                            ) {
                                Text("Continue")
                            }

                            Button(
                                onClick = { 
                                    viewModel.dismissIntervention()
                                    viewModel.simulateExitApp()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("intervention_break_button")
                            ) {
                                Text("Take a Break", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. GLOBAL SYSTEM LOCK SCREEN OVERLAY
            if (isLockActive && lockOverlayApp != null) {
                val lockedObj = lockOverlayApp!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateBackground.copy(alpha = 0.98f))
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(BlockCrimson.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked Notification",
                                tint = BlockCrimson,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "SCROLLLOCK ENGAGED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BlockCrimson,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your daily limit for ${lockedObj.appName} has been reached.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TODAY'S USAGE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", lockedObj.usedMinutesToday)} minutes spend today",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertAmber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "(Allotted: ${lockedObj.limitMinutes} minutes)",
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Come back tomorrow or use an emergency unlock to claim an extra 15 minutes.",
                            fontSize = 14.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { 
                                    viewModel.dismissLockOverlay()
                                    viewModel.simulateExitApp()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, TextGray.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("exit_lock_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.ExitToApp, contentDescription = "Exit", tint = IceWhite)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exit", color = IceWhite, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { showEmergencyUnlockReasonDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BlockCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("emergency_unlock_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.OfflineBolt, contentDescription = "Unlock", tint = IceWhite)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Emergency", color = IceWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. DIALOG: NEW APP MONITOR CREATION
    if (showAddCustomAppDialog) {
        var appNameInput by remember { mutableStateOf("") }
        var appPkgInput by remember { mutableStateOf("") }
        var appLimitInput by remember { mutableStateOf("20") }
        var alertMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCustomAppDialog = false },
            title = { Text("Monitor New Application", color = IceWhite, fontWeight = FontWeight.Bold) },
            containerColor = ObsidianSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Register a custom application on your phone to track and lock scroll feeds.", color = TextGray, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        label = { Text("App Name (e.g. Pinterest)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedLabelColor = TextGray,
                            focusedLabelColor = TechBlue,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_app_name_input")
                    )

                    OutlinedTextField(
                        value = appPkgInput,
                        onValueChange = { appPkgInput = it },
                        label = { Text("Package (e.g. com.pinterest)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedLabelColor = TextGray,
                            focusedLabelColor = TechBlue,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_app_package_input")
                    )

                    OutlinedTextField(
                        value = appLimitInput,
                        onValueChange = { appLimitInput = it },
                        label = { Text("Daily Limit (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedLabelColor = TextGray,
                            focusedLabelColor = TechBlue,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_app_limit_input")
                    )

                    alertMsg?.let {
                        Text(it, color = BlockCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitInt = appLimitInput.toIntOrNull()
                        if (appNameInput.isBlank() || appPkgInput.isBlank() || limitInt == null || limitInt <= 0) {
                            alertMsg = "Please verify all fields are valid."
                        } else {
                            viewModel.addCustomLimit(appPkgInput, appNameInput, limitInt, "Custom")
                            showAddCustomAppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    modifier = Modifier.testTag("confirm_add_app_button")
                ) {
                    Text("Add App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomAppDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // 4. DIALOG: EMERGENCY UNLOCK ACCOUNTABILITY REASON PROVIDER
    if (showEmergencyUnlockReasonDialog && lockOverlayApp != null) {
        val lockedObj = lockOverlayApp!!
        val reasonsList = listOf("Work", "School", "Business", "Emergency")
        var selectedReason by remember { mutableStateOf("Emergency") }
        var customReasonText by remember { mutableStateOf("") }
        val overridesThisWeek = overrideLogs.filter { it.packageName == lockedObj.packageName }.size

        AlertDialog(
            onDismissRequest = { showEmergencyUnlockReasonDialog = false },
            title = { Text("Accountability Verification", color = IceWhite, fontWeight = FontWeight.Bold) },
            containerColor = ObsidianSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("To prevent easy bypassing, bypasses are logged. You have used $overridesThisWeek emergency unlock(s) for this app this week (limit: 3 per week).", color = TextGray, fontSize = 14.sp)
                    
                    Text("Select a valid unlock context:", color = IceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reasonsList.forEach { reason ->
                            val isSelected = selectedReason == reason
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TechBlue else DeepGrayIndicator)
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) IceWhite else TextGray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customReasonText,
                        onValueChange = { customReasonText = it },
                        label = { Text("Details (Reason explanation)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        singleLine = true,
                        placeholder = { Text("Needs to retrieve vital address / lecture...") },
                        modifier = Modifier.fillMaxWidth().testTag("emergency_reason_input")
                    )

                    if (overridesThisWeek >= 3) {
                        Text(
                            "⛔ Limit reached! You have exeeded the weekly allowance of 3 emergency unlocks for this application.",
                            color = BlockCrimson,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = "$selectedReason: ${customReasonText.ifBlank { "Unspecified explanation" }}"
                        viewModel.triggerEmergencyUnlock(lockedObj.packageName, lockedObj.appName, finalReason)
                        showEmergencyUnlockReasonDialog = false
                    },
                    enabled = overridesThisWeek < 3,
                    colors = ButtonDefaults.buttonColors(containerColor = BlockCrimson),
                    modifier = Modifier.testTag("confirm_emergency_unlock_button")
                ) {
                    Text("Perform Breakout (15m)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyUnlockReasonDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}

// ==========================================
// VIEW 1: DASHBOARD
// ==========================================
@Composable
fun DashboardView(
    viewModel: ScrollViewModel,
    limits: List<AppLimit>,
    streak: com.example.data.DisciplineStreak?,
    logs: List<com.example.data.EmergencyOverrideLog>,
    notifications: List<String>,
    onReset: () -> Unit
) {
    val totalTime = limits.sumOf { it.usedMinutesToday }
    val blockedTodayCount = limits.count { it.isBlocked }

    // Hours saved metric logic template
    val avgSocialBaseLineTime = 180.0 // average user spends 180 mins/day on social
    val savedMins = maxOf(0.0, avgSocialBaseLineTime - totalTime)
    val savedHoursText = String.format("%.1f hrs", savedMins / 60.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SCROLLLOCK",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = IceWhite,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Time saved. Control regained.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(TechBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Shield Verified",
                        tint = TechBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Streak Progress Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, TechBlue.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(AlertAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Fire Streak", tint = AlertAmber, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${streak?.currentStreak ?: 0} Day Active Streak",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceWhite
                            )
                            Text(
                                text = "Weekly Score: ${streak?.weeklyDisciplineScore ?: 0} / 100",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "STREAK LEVEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertAmber
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AlertAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Consistency Pro", fontSize = 10.sp, color = AlertAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main Stat Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Scroll Time",
                    value = "${String.format("%.1f", totalTime)}m",
                    subtext = "Today's index",
                    icon = Icons.Outlined.HourglassEmpty,
                    accent = TechBlue,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Discipline Saved",
                    value = savedHoursText,
                    subtext = "Vs normal day",
                    icon = Icons.Outlined.CheckCircle,
                    accent = SafeMint,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Bypassed / Locks",
                    value = "${logs.size} / $blockedTodayCount",
                    subtext = "Overrides logging",
                    icon = Icons.Outlined.Block,
                    accent = BlockCrimson,
                    modifier = Modifier.weight(1f)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simulate Controls", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        
                        Button(
                            onClick = onReset,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGrayIndicator),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_usage_button")
                        ) {
                            Text("Reset Usage (Today)", fontSize = 11.sp, color = IceWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live App Feed Alerts Stream
        if (notifications.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "REAL-TIME WELLBEING NOTIFICATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 1.sp
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            notifications.take(3).forEach { feedMsg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(AlertAmber, CircleShape)
                                    )
                                    Text(
                                        text = feedMsg,
                                        fontSize = 13.sp,
                                        color = IceWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE SOCIAL LIMITS TODAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${limits.count { it.isEnabled }} tracked",
                    fontSize = 11.sp,
                    color = TechBlue
                )
            }
        }

        // Mini status bar items
        val enabledLimits = limits.filter { it.isEnabled }
        if (enabledLimits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No limits configured. Go to Limits tab to start!", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            items(enabledLimits) { limitItem ->
                DashboardAppLimitRow(limitItem)
            }
        }
    }
}

@Composable
fun DashboardAppLimitRow(limit: AppLimit) {
    val progress = minOf(1.0f, (limit.usedMinutesToday / limit.limitMinutes).toFloat())
    val tintColor = when {
        limit.isBlocked -> BlockCrimson
        progress > 0.85f -> AlertAmber
        else -> TechBlue
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(tintColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = limit.appName.take(1),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tintColor
                        )
                    }
                    Column {
                        Text(
                            text = limit.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IceWhite
                        )
                        Text(
                            text = limit.category,
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", limit.usedMinutesToday)}m / ${limit.limitMinutes}m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                    Text(
                        text = if (limit.isBlocked) "Locked 🔒" else "${String.format("%.0f", (1.0f - progress) * limit.limitMinutes)}m left",
                        fontSize = 11.sp,
                        color = if (limit.isBlocked) BlockCrimson else TextGray,
                        fontWeight = if (limit.isBlocked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = tintColor,
                trackColor = DeepGrayIndicator
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Icon(imageVector = icon, contentDescription = title, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
            Column {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = IceWhite
                )
                Text(
                    text = subtext,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }
    }
}

// ==========================================
// VIEW 2: LIMITS CONFIGURATION
// ==========================================
@Composable
fun LimitsView(
    limits: List<AppLimit>,
    isPremium: Boolean,
    onUpdate: (String, Int, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAddCustomClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "APP USAGE LIMITS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Text(
                text = "Set daily limits. When limits are hit, ScrollLock automatically blocks access.",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        // premium upsell card if not premium
        if (!isPremium) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, AlertAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Stars, contentDescription = "Premium Crown", tint = AlertAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Unlock AI addiction feedback report, custom hard scheduling blocks, and unlimited application limits tracking (currently restricted to 3 apps default). Only ${'$'}2.99 / month.", color = TextGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onUpgradeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("limits_upgrade_now_button")
                        ) {
                            Text("Unlock ScrollLock Premium", fontWeight = FontWeight.Bold, color = SlateBackground)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, SafeMint.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = "Active Premium Logo", tint = SafeMint, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ScrollLock Premium Active • All Locks Enabled", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeMint)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED APPLICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = onAddCustomClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_app_trigger")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Icon", tint = IceWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom", fontSize = 12.sp, color = IceWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Display configured limits
        val shownLimits = if (isPremium) limits else limits.take(3) // restricted to 3 active locks on free tier
        items(shownLimits) { limit ->
            LimitControlCard(limit, onUpdate, onDelete)
        }

        if (!isPremium && limits.size > 3) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(ObsidianSurface, RoundedCornerShape(12.dp))
                        .clickable { onUpgradeClick() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ ${limits.size - 3} more apps locked under Premium configuration. Tap to unlock.", color = AlertAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LimitControlCard(
    limit: AppLimit,
    onUpdate: (String, Int, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(TechBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(limit.appName.take(1), color = TechBlue, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(limit.appName, fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 15.sp)
                        Text(limit.packageName, color = TextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = limit.isEnabled,
                        onCheckedChange = { onUpdate(limit.packageName, limit.limitMinutes, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafeMint,
                            checkedTrackColor = SafeMint.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = DeepGrayIndicator
                        ),
                        modifier = Modifier.testTag("switch_limit_${limit.appName}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onDelete(limit.packageName) },
                        modifier = Modifier.testTag("delete_limit_${limit.appName}")
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Limit", tint = BlockCrimson.copy(alpha = 0.8f))
                    }
                }
            }

            if (limit.isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Restriction Hourglass:", color = TextGray, fontSize = 12.sp)
                        Text("${limit.limitMinutes} minutes allotted", color = TechBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = limit.limitMinutes.toFloat(),
                        onValueChange = { onUpdate(limit.packageName, it.toInt(), limit.isEnabled) },
                        valueRange = 5f..120f,
                        steps = 22, // intervals of 5 mins
                        colors = SliderDefaults.colors(
                            thumbColor = TechBlue,
                            activeTrackColor = TechBlue,
                            inactiveTrackColor = DeepGrayIndicator
                        ),
                        modifier = Modifier.testTag("slider_limit_${limit.appName}")
                    )
                }
            } else {
                Text("Lock status currently inactive. Social access is unlimited.", color = TextGray, fontSize = 12.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            }
        }
    }
}

// ==========================================
// VIEW 3: FOCUS MODE
// ==========================================
@Composable
fun FocusModeView(
    limits: List<AppLimit>,
    focusState: FocusModeState?,
    onActivate: (Int, List<String>) -> Unit,
    onDeactivate: () -> Unit
) {
    val isFocusActive = focusState?.isActive == true
    
    // Select state for multi selection of apps
    val selectedApps = remember { mutableStateListOf<String>() }
    var selectedDurationHours by remember { mutableStateOf(1) }

    // Initialize list with some limits packages if empty to secure standard block targets
    LaunchedEffect(limits) {
        if (selectedApps.isEmpty() && limits.isNotEmpty()) {
            selectedApps.addAll(limits.take(3).map { it.packageName })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TARGET FOCUS BLOCK",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceWhite
                )
                Text(
                    text = "Instantly sever connection list to highly addictive applications. Selected apps remain strictly blocked until the duration is complete.",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
        }

        if (isFocusActive) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(2.dp, SafeMint.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(SafeMint.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.OfflineBolt, contentDescription = "Focus Mode On", tint = SafeMint, modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "FOCUS BLOCK ACTIVE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceWhite
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // remaining duration simulation format
                        val remainingMins = maxOf(0, ((focusState!!.endTimestamp - System.currentTimeMillis()) / 60000).toInt())
                        Text(
                            text = if (remainingMins > 0) "$remainingMins minutes remaining" else "Active until scheduled completion",
                            fontSize = 14.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Blocked Applications:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = focusState!!.blockedApps.split(",").joinToString(", ") { pkg ->
                                limits.find { it.packageName == pkg }?.appName ?: pkg.substringAfterLast(".")
                            },
                            color = AlertAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDeactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = BlockCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .testTag("deactivate_focus_mode_button")
                        ) {
                            Text("Abandon Focus (End Blocks)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Configuration Setup UI
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("1. Focus Session Interval:", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 14.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair(1, "1 Hour"),
                                Pair(2, "2 Hours"),
                                Pair(4, "4 Hours"),
                                Pair(24, "Tomorrow")
                            ).forEach { (hrs, label) ->
                                val isSelected = selectedDurationHours == hrs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) TechBlue else DeepGrayIndicator)
                                        .clickable { selectedDurationHours = hrs }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) IceWhite else TextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Select Apps under Restrict Alert:", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (limits.isEmpty()) {
                            Text("No configured apps. Please set limits first.", color = TextGray)
                        } else {
                            limits.forEach { appObj ->
                                val isChecked = selectedApps.contains(appObj.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                selectedApps.remove(appObj.packageName)
                                            } else {
                                                selectedApps.add(appObj.packageName)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(TechBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(appObj.appName.take(1), fontWeight = FontWeight.Bold, color = TechBlue)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(appObj.appName, color = IceWhite, fontSize = 14.sp)
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            if (check == true && !isChecked) selectedApps.add(appObj.packageName)
                                            if (check == false && isChecked) selectedApps.remove(appObj.packageName)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = TechBlue,
                                            uncheckedColor = TextGray
                                        ),
                                        modifier = Modifier.testTag("focus_checkbox_${appObj.appName}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onActivate(selectedDurationHours, selectedApps.toList()) },
                    enabled = selectedApps.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeMint),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp)
                        .testTag("activate_focus_mode_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = "Launch active lock", tint = SlateBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE FOCUS SESSION", color = SlateBackground, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 4: SCHEDULED LOCKS
// ==========================================
@Composable
fun SchedulesView(
    schedules: List<LockSchedule>,
    isPremium: Boolean,
    onAdd: (String, Int, Int, Int, Int) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onUpgradeClick: () -> Unit
) {
    // Form variables
    var showAddScheduleForm by remember { mutableStateOf(false) }
    var scheduleName by remember { mutableStateOf("") }
    var startH by remember { mutableStateOf("09") }
    var startM by remember { mutableStateOf("00") }
    var endH by remember { mutableStateOf("17") }
    var endM by remember { mutableStateOf("00") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "SCHEDULED SOCIAL BLOCKS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Text(
                text = "Automatically lock all social services during designated work, sleep, or study windows.",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        if (!isPremium) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, AlertAmber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔐 Premium Schedule Slots Required", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlertAmber)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Free accounts have standard mock slots. Custom scheduled lock blocks require a Premium Subscription active.", fontSize = 12.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onUpgradeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Unlock Scheduling Controls", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Button(
                    onClick = { showAddScheduleForm = !showAddScheduleForm },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_add_schedule_form_button")
                ) {
                    Text(if (showAddScheduleForm) "Close Form" else "Add New Schedule Slot")
                }
            }

            if (showAddScheduleForm) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, TechBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("SLOT CONFIGURATOR", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 14.sp)
                            
                            OutlinedTextField(
                                value = scheduleName,
                                onValueChange = { scheduleName = it },
                                label = { Text("Schedule Label (e.g. Study Time)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = IceWhite,
                                    unfocusedTextColor = IceWhite
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("schedule_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = startH,
                                    onValueChange = { startH = it },
                                    label = { Text("Start Hr (0-23)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = IceWhite, unfocusedTextColor = IceWhite),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = startM,
                                    onValueChange = { startM = it },
                                    label = { Text("Start Min") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = IceWhite, unfocusedTextColor = IceWhite),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("to", color = IceWhite)
                                OutlinedTextField(
                                    value = endH,
                                    onValueChange = { endH = it },
                                    label = { Text("End Hr") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = IceWhite, unfocusedTextColor = IceWhite),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endM,
                                    onValueChange = { endM = it },
                                    label = { Text("End Min") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = IceWhite, unfocusedTextColor = IceWhite),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    val sHour = startH.toIntOrNull() ?: 0
                                    val sMin = startM.toIntOrNull() ?: 0
                                    val eHour = endH.toIntOrNull() ?: 0
                                    val eMin = endM.toIntOrNull() ?: 0
                                    
                                    if (scheduleName.isNotBlank()) {
                                        onAdd(scheduleName, sHour, sMin, eHour, eMin)
                                        scheduleName = ""
                                        showAddScheduleForm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeMint),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_schedule_button")
                            ) {
                                Text("Save New Lock Schedule", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        items(schedules) { sched ->
            ScheduleRowItem(sched, onToggle, onDelete, isPremium)
        }
    }
}

@Composable
fun ScheduleRowItem(
    schedule: LockSchedule,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    allowInteraction: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(TechBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar", tint = TechBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = schedule.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                    Text(
                        text = "${String.format("%02d", schedule.startHour)}:${String.format("%02d", schedule.startMinute)} — ${String.format("%02d", schedule.endHour)}:${String.format("%02d", schedule.endMinute)}",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { if (allowInteraction) onToggle(schedule.id, it) },
                    enabled = allowInteraction,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafeMint,
                        checkedTrackColor = SafeMint.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = DeepGrayIndicator
                    ),
                    modifier = Modifier.testTag("switch_schedule_${schedule.name}")
                )
                if (allowInteraction) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDelete(schedule.id) },
                        modifier = Modifier.testTag("delete_schedule_${schedule.name}")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Schedule", tint = BlockCrimson.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 5: ANALYTICS & STATS CHART
// ==========================================
@Composable
fun AnalyticsView(
    history: List<com.example.data.UsageHistory>,
    limits: List<AppLimit>,
    streak: com.example.data.DisciplineStreak?
) {
    // Generate simple custom analytics chart representing past 7 days usage
    val weekdayLabels = listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon")
    // Summarized total values today template or custom mock bar heights
    val barValues = listOf(145f, 130f, 95f, 110f, 75f, 60f, limits.sumOf { it.usedMinutesToday }.toFloat())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DISCIPLINE ANALYTICS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Text(
                text = "Historical review of scroll activity patterns, streaks, and focus metrics.",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        // Custom canvas draw bar chart (Beautiful visual bar chart as requested!)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SOCIAL MEDIA TIME INDEX (PAST 7 DAYS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Draw direct charts using Canvas standard dimensions!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 32.dp.toPx()
                            val spacing = (size.width - (7 * barWidth)) / 8
                            val maxVal = 200f // max minutes in range

                            for (i in 0..6) {
                                val valMins = barValues.getOrElse(i) { 0f }
                                val barHeight = (valMins / maxVal) * (size.height - 40.dp.toPx())
                                val curX = spacing + i * (barWidth + spacing)
                                val curY = size.height - 30.dp.toPx() - barHeight

                                // Draw bar gradient
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(TechBlue, TechBlue.copy(alpha = 0.4f))
                                    ),
                                    topLeft = Offset(curX, curY),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }

                        // Labels row placed floating or on canvas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            weekdayLabels.forEach { label ->
                                Text(label, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Addiction Insight highlights
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BEHAVIOR PATTERNS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Most Addictive App", color = IceWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Takes 42% of total social budget", color = TextGray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BlockCrimson.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("TikTok", color = BlockCrimson, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Divider(color = DeepGrayIndicator, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Discipline Level", color = IceWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Hours saved is inside top 12% globally!", color = TextGray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SafeMint.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Class-A Elite", color = SafeMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "HISTOGRAM EVENT STREAK SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cumulative hours saved active limits:", color = TextGray, fontSize = 13.sp)
                            Text("${streak?.totalHoursSaved ?: 0.0} hrs", color = IceWhite, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discipline level active consistency status:", color = TextGray, fontSize = 13.sp)
                            Text("Excellent consistency Index", color = SafeMint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 6: AI ADDICTION ADVISOR
// ==========================================
@Composable
fun AiAdvisorView(
    insights: String?,
    isLoading: Boolean,
    isPremium: Boolean,
    onGenerate: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SCROLLLOCK AI COACH",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IceWhite
            )
            Text(
                text = "Get diagnostic reports, custom behavioral hacks, and accountability guidelines generated instantly from real-time usage metrics by Gemini Flash.",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        if (!isPremium) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, AlertAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Psychology, contentDescription = "Active Coach", tint = AlertAmber, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Unlock Premium AI Reports", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Connect directly to Gemini 3.5 Flash for fully customized critiques, scrolling behavioral triage, and bespoke willpower exercises to boost discipline.", color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onUpgradeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertAmber)
                        ) {
                            Text("Upgrade Active Status", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GENERATE INSIGHTS REPORT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Will compile daily usage limits, Streaks, and emergency overrides reasons logs to send over secure REST.",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onGenerate,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_ai_critique_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = IceWhite, modifier = Modifier.size(20.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, "Sparkle", tint = IceWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze Usage with AI", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            insights?.let { text ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, SafeMint.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, "Sparkle Logo", tint = SafeMint, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Coach Critique Report", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 15.sp)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("EVALUATED", color = SafeMint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = text,
                                color = IceWhite,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("ai_report_textbox")
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 7: SIMULATOR CONSOLE
// ==========================================
@Composable
fun SimulatorConsoleView(
    viewModel: ScrollViewModel,
    limits: List<AppLimit>,
    isFastSim: Boolean,
    simulatedApp: String?,
    isScrolling: Boolean,
    scrollMinutes: Double,
    onToggleFast: (Boolean) -> Unit,
    onTogglePremium: (Boolean) -> Unit,
    onLaunch: (String, String) -> Unit,
    onExit: () -> Unit,
    onSetScroll: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SIMULATOR PLAYGROUND",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceWhite
                )
                Text(
                    text = "Launch mock third-party social feeds to test and trigger ScrollLock's live overlays and limit breakers instantly.",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
        }

        // Configuration Control parameters block
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SIMULATOR SETUP CONFIG:", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hyper-Speed Simulator", color = IceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("1 real sec = 1.0 minute (Fast Limits trigger)", color = TextGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isFastSim,
                            onCheckedChange = { onToggleFast(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TechBlue, checkedTrackColor = TechBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("simulator_fast_switch")
                        )
                    }

                    // Simulated premium toggle
                    val isPremLocal = viewModel.isPremium.collectAsState(false).value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Simulate Premium Status", color = IceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Toggle subscription flags for limits & schedules", color = TextGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isPremLocal,
                            onCheckedChange = { onTogglePremium(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AlertAmber, checkedTrackColor = AlertAmber.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("simulator_premium_switch")
                        )
                    }
                }
            }
        }

        // Active app state ticker if simulating
        if (simulatedApp != null) {
            val limitObj = limits.find { it.packageName == simulatedApp }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                    border = BorderStroke(2.dp, if (isScrolling) AlertAmber else TechBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isScrolling) AlertAmber else SafeMint, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SCREEN ACTIVE: ${limitObj?.appName ?: simulatedApp}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = IceWhite,
                                letterSpacing = 1.sp
                            )
                        }

                        // Usage info metrics
                        limitObj?.let { item ->
                            Text(
                                text = "${String.format("%.1f", item.usedMinutesToday)} / ${item.limitMinutes} min spend today",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = IceWhite
                            )

                            LinearProgressIndicator(
                                progress = { minOf(1.0f, (item.usedMinutesToday / item.limitMinutes).toFloat()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (isScrolling) AlertAmber else TechBlue,
                                trackColor = DeepGrayIndicator
                            )
                        }

                        // Smart scroll detection triggers description
                        Text(
                            text = if (isScrolling) "📈 Infinite Feed Scroll detected! Continuous block: ${String.format("%.1f", scrollMinutes)} minutes." 
                                   else "🧘 Idle reading mode. Tap 'Feed Scrolling' below to simulate infinite feed swipe and check interventions.",
                            fontSize = 12.sp,
                            color = if (isScrolling) AlertAmber else TextGray,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSetScroll(!isScrolling) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isScrolling) AlertAmber else IceWhite),
                                border = BorderStroke(1.dp, if (isScrolling) AlertAmber else TextGray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.2f).testTag("simulator_toggle_scroll")
                            ) {
                                Text(if (isScrolling) "Stop Scrolling" else "Feed Scrolling")
                            }

                            Button(
                                onClick = onExit,
                                colors = ButtonDefaults.buttonColors(containerColor = BlockCrimson),
                                modifier = Modifier.weight(1f).testTag("simulator_exit")
                            ) {
                                Text("Close App")
                            }
                        }
                    }
                }
            }
        } else {
            // Pick an App to Simulate launching!
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SELECT SOCIAL FEED TO LAUNCH:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 1.sp
                    )

                    limits.forEach { limit ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunch(limit.packageName, limit.appName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(TechBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(limit.appName.take(1), fontWeight = FontWeight.Bold, color = TechBlue)
                                    }
                                    Column {
                                        Text(limit.appName, fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 14.sp)
                                        Text(limit.packageName, color = TextGray, fontSize = 11.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${String.format("%.1f", limit.usedMinutesToday)}m / ${limit.limitMinutes}m",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Simulate Launch Link",
                                        tint = SafeMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Gray Indicator constant definitions
val DeepGrayIndicator = Color(0xFF202530)
val DeepGrayIndicatorLight = Color(0xFFE5E7EB)
