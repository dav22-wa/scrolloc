package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.ScrollRepository
import com.example.service.ScrollLockManager
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScrollViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: ScrollViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ScrollViewModel::class.java]
        
        handleIntent(intent)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val triggerPackage = intent.getStringExtra("TRIGGER_LOCK_OVERLAY")
        if (triggerPackage != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val repo = ScrollRepository(db)
                val limit = repo.getLimitByPackage(triggerPackage)
                if (limit != null) {
                    // Force display overlay
                    ScrollLockManager.startSimulateApp(limit.packageName, limit.appName)
                    // trigger overlay
                    viewModel.updateLimit(limit.packageName, limit.limitMinutes, true)
                }
            }
        }
    }
}
