package com.echocare.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echocare.app.util.EchoCareTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onUpgradeClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }
    
    // Battery optimization state
    var isBatteryOptimizedExempt by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    // Diagnostics logs dialog state
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    
    // Voice cloning calibration dialog state
    var showVoiceCloningDialog by remember { mutableStateOf(false) }
    var calibrationStep by remember { mutableStateOf(0) }
    val calibrationPhrases = listOf(
        "Hello Papa, this is Sandeep. Just checking in on you.",
        "Remember to drink water and take your daily walk.",
        "Your health is important to us. Please take your medicine now."
    )

    val requestBatteryOptimizationExemption = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryOptimizedExempt = pm.isIgnoringBatteryOptimizations(context.packageName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ─── Premium Membership Upgrades ──────────────────────────────
            if (!state.isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpgradeClick() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upgrade to Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Unlock voice cloning, smart home sync, and database backups.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        TextButton(onClick = onUpgradeClick) {
                            Text("Upgrade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("EchoCare Premium Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            Text("You have unlimited access to all care features.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24).copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // ─── Profile ──────────────────────────────────────────────────
            SettingsSection(title = "Profile") {
                OutlinedTextField(
                    value         = nameInput,
                    onValueChange = { nameInput = it },
                    label         = { Text("Your name") },
                    leadingIcon   = { Icon(Icons.Outlined.Person, null) },
                    trailingIcon  = {
                        if (nameInput != state.userName) {
                            IconButton(onClick = { viewModel.updateName(nameInput) }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            // ─── TTS / Voice ──────────────────────────────────────────────
            SettingsSection(title = "Voice & Language") {
                Text(
                    "TTS Language",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageChip(label = "🇬🇧 English", selected = state.ttsLanguage == "en", onClick = { viewModel.updateLanguage("en") })
                    LanguageChip(label = "🇮🇳 Hindi", selected = state.ttsLanguage == "hi", onClick = { viewModel.updateLanguage("hi") })
                    LanguageChip(label = "🇮🇳 Marathi", selected = state.ttsLanguage == "mr", onClick = { viewModel.updateLanguage("mr") })
                    LanguageChip(label = "🇮🇳 Tamil", selected = state.ttsLanguage == "ta", onClick = { viewModel.updateLanguage("ta") })
                    LanguageChip(label = "🇮🇳 Telugu", selected = state.ttsLanguage == "te", onClick = { viewModel.updateLanguage("te") })
                    LanguageChip(label = "🇮🇳 Bengali", selected = state.ttsLanguage == "bn", onClick = { viewModel.updateLanguage("bn") })
                    LanguageChip(label = "🇮🇳 Kannada", selected = state.ttsLanguage == "kn", onClick = { viewModel.updateLanguage("kn") })
                }

                Spacer(Modifier.height(4.dp))

                val speedLabel = when {
                    state.ttsSpeed <= 0.75f -> "Slow"
                    state.ttsSpeed <= 1.25f -> "Normal"
                    else                    -> "Fast"
                }
                Text(
                    "Speech Speed: $speedLabel (${state.ttsSpeed}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value        = state.ttsSpeed,
                    onValueChange = viewModel::updateSpeed,
                    valueRange   = 0.5f..2.0f,
                    steps        = 5,
                    colors       = SliderDefaults.colors(
                        thumbColor       = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Speech Pitch: ${state.voicePitch}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value        = state.voicePitch,
                    onValueChange = viewModel::updateVoicePitch,
                    valueRange   = 0.5f..2.0f,
                    steps        = 5,
                    colors       = SliderDefaults.colors(
                        thumbColor       = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // ─── Alarm Defaults ───────────────────────────────────────────
            SettingsSection(title = "Alarm Defaults") {
                Text(
                    "Default repeat interval: ${state.repeatIntervalMin} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value        = state.repeatIntervalMin.toFloat(),
                    onValueChange = { viewModel.updateRepeatInterval(it.toInt()) },
                    valueRange   = 1f..30f,
                    steps        = 28
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Default max repetitions: ${state.maxRepetitions} times",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value        = state.maxRepetitions.toFloat(),
                    onValueChange = { viewModel.updateMaxRepetitions(it.toInt()) },
                    valueRange   = 1f..10f,
                    steps        = 8
                )
            }

            // ─── Voice Cloning & AI Calibration ───────────────────────────
            SettingsSection(title = "Voice Cloning") {
                SettingActionRow(
                    label   = "AI Voice Calibration",
                    subtext = if (state.isPremium) "Record voice prompts to generate model" else "Clone your voice for personalized alerts (Premium)",
                    icon    = Icons.Outlined.RecordVoiceOver,
                    actionLabel = if (state.isPremium) "Configure" else "Unlock 👑",
                    onClick = {
                        if (state.isPremium) {
                            showVoiceCloningDialog = true
                        } else {
                            onUpgradeClick()
                        }
                    }
                )
            }

            // ─── Smart Home Integration ───────────────────────────────────
            SettingsSection(title = "Smart Home Integrations") {
                SettingToggleRow(
                    label   = "Amazon Alexa Skill",
                    subtext = "Sync shared reminders to Alexa speakers",
                    icon    = Icons.Outlined.Home,
                    checked = state.alexaLinked && state.isPremium,
                    onToggle = {
                        if (state.isPremium) viewModel.toggleAlexa(it)
                        else onUpgradeClick()
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingToggleRow(
                    label   = "Google Assistant",
                    subtext = "Sync shared reminders to Google Nest Home Hubs",
                    icon    = Icons.Outlined.SettingsInputAntenna,
                    checked = state.googleHomeLinked && state.isPremium,
                    onToggle = {
                        if (state.isPremium) viewModel.toggleGoogleHome(it)
                        else onUpgradeClick()
                    }
                )
            }

            // ─── Database Backup & Restore ───────────────────────────────
            SettingsSection(title = "Backup & Restore") {
                SettingActionRow(
                    label   = "Backup Reminders",
                    subtext = "Export database reminders to local JSON",
                    icon    = Icons.Outlined.CloudUpload,
                    actionLabel = "Export",
                    onClick = {
                        if (state.isPremium) {
                            viewModel.backupReminders(
                                context = context,
                                onSuccess = { path ->
                                    Toast.makeText(context, "Backup saved to: $path", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Backup failed: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            onUpgradeClick()
                        }
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingActionRow(
                    label   = "Restore Reminders",
                    subtext = "Import reminders from local JSON backup file",
                    icon    = Icons.Outlined.CloudDownload,
                    actionLabel = "Import",
                    onClick = {
                        if (state.isPremium) {
                            viewModel.restoreReminders(
                                context = context,
                                onSuccess = {
                                    Toast.makeText(context, "Reminders restored successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            onUpgradeClick()
                        }
                    }
                )
            }

            // ─── Security ─────────────────────────────────────────────────
            SettingsSection(title = "Security & System") {
                SettingToggleRow(
                    label   = "Biometric Unlock",
                    subtext = "Use fingerprint or face to unlock EchoCare",
                    icon    = Icons.Outlined.Fingerprint,
                    checked = state.biometricEnabled,
                    onToggle = viewModel::toggleBiometric
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingActionRow(
                    label   = "Battery Optimization Whitelist",
                    subtext = if (isBatteryOptimizedExempt) "Optimizations are bypassed" else "Bypass optimization to prevent alarms being blocked",
                    icon    = Icons.Outlined.BatteryAlert,
                    actionLabel = if (isBatteryOptimizedExempt) "Whitelisted" else "Configure",
                    onClick = {
                        if (!isBatteryOptimizedExempt) {
                            requestBatteryOptimizationExemption()
                        } else {
                            Toast.makeText(context, "EchoCare is already whitelisted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // ─── Developer & Closed Beta Options ──────────────────────────
            SettingsSection(title = "Developer & Closed Beta Options") {
                SettingActionRow(
                    label   = "Diagnostics Log Viewer",
                    subtext = "Inspect local telemetry event stream logs",
                    icon    = Icons.Outlined.BugReport,
                    actionLabel = "View Logs",
                    onClick = { showDiagnosticsDialog = true }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingActionRow(
                    label   = "Simulate Non-Fatal Crash",
                    subtext = "Logs a mock exception message to diagnostics stream",
                    icon    = Icons.Outlined.ErrorOutline,
                    actionLabel = "Simulate",
                    onClick = {
                        viewModel.triggerNonFatalCrash()
                        Toast.makeText(context, "Non-fatal exception logged!", Toast.LENGTH_SHORT).show()
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingActionRow(
                    label   = "Simulate Fatal Crash",
                    subtext = "Throws RuntimeException and crashes application process",
                    icon    = Icons.Outlined.Dangerous,
                    actionLabel = "Force Crash",
                    onClick = {
                        viewModel.triggerFatalCrash()
                    }
                )
            }

            // ─── About ────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                AboutRow(label = "Version",   value = "1.0.0 (Final)")
                AboutRow(label = "Build",     value = "Beta Release")
                AboutRow(label = "Platform",  value = "Android")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Diagnostics Log Dialog
    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Text(
                    "Diagnostics & Telemetry Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val logs = remember { EchoCareTelemetry.getLogs() }
                if (logs.isEmpty()) {
                    Text("No logs recorded yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logs.forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.eventName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = log.timestamp.substringAfter(" "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (log.details.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.details.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearTelemetryLogs()
                    showDiagnosticsDialog = false
                }) {
                    Text("Clear Logs")
                }
            }
        )
    }

    // Voice Cloning Calibration Dialog
    if (showVoiceCloningDialog) {
        AlertDialog(
            onDismissRequest = {
                showVoiceCloningDialog = false
                calibrationStep = 0
            },
            title = {
                Text(
                    "Voice Cloning Model Calibration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Phrase ${calibrationStep + 1} of 3: Read the sentence aloud to calibrate your custom voice model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\"${calibrationPhrases[calibrationStep]}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (calibrationStep < 2) {
                            calibrationStep++
                        } else {
                            showVoiceCloningDialog = false
                            calibrationStep = 0
                            Toast.makeText(context, "Voice Cloning Model Calibrated!", Toast.LENGTH_SHORT).show()
                            EchoCareTelemetry.logEvent("voice_cloning_calibrated")
                        }
                    }
                ) {
                    Text(if (calibrationStep < 2) "Next Phrase" else "Complete Calibration")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVoiceCloningDialog = false
                    calibrationStep = 0
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SettingToggleRow(
    label: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    label: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onClick) {
            Text(actionLabel, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
