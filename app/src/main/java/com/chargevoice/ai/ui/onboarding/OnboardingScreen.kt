package com.chargevoice.ai.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.chargevoice.ai.datastore.dataStore
import kotlinx.coroutines.launch

data class OnboardingPage(val title: String, val description: String, val buttonText: String, val icon: ImageVector)

val onboardingPages = listOf(
    OnboardingPage("Welcome to ChargeVoice AI", "Your smart voice charging assistant.", "Next", Icons.Default.BatteryChargingFull),
    OnboardingPage("Voice Charging Updates", "Hear your battery progress without checking your phone.", "Next", Icons.Default.RecordVoiceOver),
    OnboardingPage("Reliable Monitoring", "Monitoring automatically starts while charging and stops when charging ends.", "Next", Icons.Default.Shield),
    OnboardingPage("Almost Ready", "Please allow Notification permissions to see the monitoring banner.", "Allow", Icons.Default.NotificationsActive),
    OnboardingPage("Battery Optimization", "Exempt this app from battery optimization for reliable background tracking.", "Allow Unrestricted", Icons.Default.Shield),
    OnboardingPage("You're all set", "ChargeVoice AI is ready to use.", "Start Using ChargeVoice AI", Icons.Default.CheckCircle)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(page = onboardingPages[page])
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip Button
            if (pagerState.currentPage < onboardingPages.size - 1) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
                            context.dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = true }
                            onOnboardingFinished()
                        }
                    }
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }

            // Page Indicators
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(onboardingPages.size) { index ->
                    val color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val width = if (pagerState.currentPage == index) 24.dp else 8.dp
                    Box(modifier = Modifier
                        .padding(4.dp)
                        .size(width = width, height = 8.dp)
                        .background(color, shape = CircleShape)
                    )
                }
            }

            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            3 -> { // Request Notification Permission
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            }
                            4 -> { // Open Battery Optimization Request
                                val intent = Intent()
                                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                            onboardingPages.size - 1 -> { // Finish
                                coroutineScope.launch {
                                    val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
                                    context.dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = true }
                                    onOnboardingFinished()
                                }
                            }
                            else -> {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(onboardingPages[pagerState.currentPage].buttonText, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = page.title, 
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), 
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description, 
            style = MaterialTheme.typography.bodyLarge, 
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
