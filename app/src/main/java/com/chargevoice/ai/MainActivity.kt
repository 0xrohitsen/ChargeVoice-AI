package com.chargevoice.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chargevoice.ai.theme.ChargeVoiceAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    val constraints = androidx.work.Constraints.Builder()
        .setRequiresCharging(true)
        .build()
        
    val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.chargevoice.ai.service.ChargingTriggerWorker>()
        .setConstraints(constraints)
        .build()
        
    androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
        com.chargevoice.ai.service.ChargingTriggerWorker.WORK_NAME,
        androidx.work.ExistingWorkPolicy.KEEP, // KEEP — don't replace if already pending
        workRequest
    )
    setContent {
      ChargeVoiceAITheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { com.chargevoice.ai.ui.navigation.AppNavigation() } }
    }
  }
}
