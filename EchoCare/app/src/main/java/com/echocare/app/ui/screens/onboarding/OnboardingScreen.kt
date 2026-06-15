package com.echocare.app.ui.screens.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.echocare.app.util.EchoCareTelemetry
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Define slides
    val pages = listOf(
        OnboardingPage.VoiceAlarms,
        OnboardingPage.FamilyCare,
        OnboardingPage.Permissions
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    // Permissions states
    var hasMicPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasExactAlarmPermission by remember { mutableStateOf(false) }
    var isBatteryOptimizationsIgnored by remember { mutableStateOf(false) }

    // Helper to refresh permission statuses
    val checkPermissions = {
        hasMicPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptimizationsIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    // Check permissions initially and when resuming
    LaunchedEffect(Unit) {
        checkPermissions()
        EchoCareTelemetry.logEvent("onboarding_entered")
    }

    // Permission launcher for Mic and Notifications
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkPermissions()
        results.forEach { (perm, granted) ->
            EchoCareTelemetry.logEvent("permission_requested", mapOf("permission" to perm, "granted" to granted))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIdx ->
                when (val page = pages[pageIdx]) {
                    is OnboardingPage.SimplePage -> {
                        SimplePageContent(page = page)
                    }
                    is OnboardingPage.Permissions -> {
                        PermissionsPageContent(
                            hasMic = hasMicPermission,
                            hasNotif = hasNotificationPermission,
                            hasAlarm = hasExactAlarmPermission,
                            hasBattery = isBatteryOptimizationsIgnored,
                            onRequestPermissions = {
                                val permissionsNeeded = mutableListOf<String>()
                                if (!hasMicPermission) {
                                    permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
                                }
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                if (permissionsNeeded.isNotEmpty()) {
                                    requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
                                }

                                // Handle system intents for alarm/battery optimization
                                if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                }

                                if (!isBatteryOptimizationsIgnored && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Action Button
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("NEXT →", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            EchoCareTelemetry.logEvent("onboarding_completed")
                            onFinished()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("GET STARTED")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplePageContent(page: OnboardingPage.SimplePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(page.emoji, fontSize = 56.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun PermissionsPageContent(
    hasMic: Boolean,
    hasNotif: Boolean,
    hasAlarm: Boolean,
    hasBattery: Boolean,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "System Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "EchoCare requires a few settings configured to deliver voice alerts reliably.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Checklist Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PermissionCheckRow(
                title = "Microphone Access",
                description = "Used to record your reminder voice commands.",
                granted = hasMic
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionCheckRow(
                title = "Notifications Delivery",
                description = "Required to display full-screen alarm popups.",
                granted = hasNotif
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionCheckRow(
                title = "Exact Alarm Scheduling",
                description = "Allows triggering spoken reminders at the precise second.",
                granted = hasAlarm
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionCheckRow(
                title = "Battery Whitelist",
                description = "Prevents system from shutting down reminder schedulers.",
                granted = hasBattery
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        val allGranted = hasMic && hasNotif && hasAlarm && hasBattery

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (allGranted) "All Permissions Granted ✓" else "Configure System Permissions",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionCheckRow(
    title: String,
    description: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (granted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Pending Setup",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

sealed class OnboardingPage {
    abstract val key: String
    
    sealed class SimplePage(
        override val key: String,
        val emoji: String,
        val title: String,
        val description: String
    ) : OnboardingPage()
    
    object VoiceAlarms : SimplePage(
        key = "voice_alarms",
        emoji = "🗣️",
        title = "Voice-First Reminders",
        description = "Simply speak naturally, like: 'Tell Papa to take medicine every day at 8 PM'. The app interprets your command and configures the exact alarm automatically."
    )
    
    object FamilyCare : SimplePage(
        key = "family_care",
        emoji = "❤️",
        title = "Care For Your Family",
        description = "Create family groups and share reminders remotely. Spoken alarms trigger directly on recipient devices, even if they are offline or the app is closed."
    )
    
    object Permissions : OnboardingPage() {
        override val key = "permissions"
    }
}
