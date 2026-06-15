package com.echocare.app.ui.screens.home

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echocare.app.domain.model.RecurrenceType
import com.echocare.app.domain.model.Reminder
import com.echocare.app.ui.components.ReminderCard
import com.echocare.app.ui.screens.groups.GroupViewModel
import com.echocare.app.ui.screens.settings.SettingsViewModel
import com.echocare.app.util.BackupHelper
import com.echocare.app.util.EchoCareTelemetry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupState by groupViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Personal Alarms, 1 = Group Alarms
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Inline inputs for Personal alarm form
    var personalTitle by remember { mutableStateOf("") }
    var personalMessage by remember { mutableStateOf("") }
    var personalHour by remember { mutableStateOf(8) }
    var personalMinute by remember { mutableStateOf(0) }
    var personalTimeSelected by remember { mutableStateOf(false) }

    // Inline inputs for Group creation form
    var groupName by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var memberPhone by remember { mutableStateOf("") }

    // Inline inputs for Shared alarm form
    var selectedMemberId by remember { mutableStateOf("") }
    var sharedMessage by remember { mutableStateOf("") }
    var sharedHour by remember { mutableStateOf(20) }
    var sharedMinute by remember { mutableStateOf(0) }
    var sharedTimeSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── SECTION 1: PERSONAL VOICE ALARMS ───────────────────
            Text(
                text = "Your Personal Voice Alarms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Filter only personal reminders (recipientId is null or empty)
            val personalReminders = uiState.upcomingReminders.filter { it.recipientId.isNullOrEmpty() }

            if (personalReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No personal alarms scheduled yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    personalReminders.forEach { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onActiveToggle = { viewModel.toggleActive(reminder) },
                            onCardClick = {}
                        )
                    }
                }
            }

            // Inline Creator Form for Personal Alarms
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Schedule Personal Alarm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = personalTitle,
                        onValueChange = { personalTitle = it },
                        label = { Text("Alarm Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = personalMessage,
                        onValueChange = { personalMessage = it },
                        label = { Text("Spoken Voice Message") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        personalHour = h
                                        personalMinute = m
                                        personalTimeSelected = true
                                    },
                                    personalHour,
                                    personalMinute,
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (personalTimeSelected)
                                    String.format("Time: %02d:%02d", personalHour, personalMinute)
                                else "Choose Alarm Time"
                            )
                        }

                        Button(
                            onClick = {
                                if (personalTitle.trim().isEmpty() || !personalTimeSelected) {
                                    Toast.makeText(context, "Please set a title and time", Toast.LENGTH_SHORT).show()
                                } else {
                                    val calendar = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, personalHour)
                                        set(Calendar.MINUTE, personalMinute)
                                        set(Calendar.SECOND, 0)
                                        if (timeInMillis <= System.currentTimeMillis()) {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                    }
                                    val reminder = Reminder(
                                        title = personalTitle,
                                        message = personalMessage,
                                        triggerTime = calendar.timeInMillis,
                                        recurrenceType = RecurrenceType.DAILY,
                                        recurrenceIntervalMs = 0L,
                                        repeatIntervalMinutes = settingsState.repeatIntervalMin,
                                        maxRepetitions = settingsState.maxRepetitions,
                                        isActive = true,
                                        creatorId = "local_user",
                                        syncStatus = "SYNCED"
                                    )
                                    viewModel.processTranscript("") // Reset voice dialog overlays
                                    viewModel.updateParsedReminder(reminder)
                                    viewModel.confirmParsedReminder()
                                    
                                    // Clear inputs
                                    personalTitle = ""
                                    personalMessage = ""
                                    personalTimeSelected = false
                                    Toast.makeText(context, "Personal Alarm scheduled!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Save Alarm")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // ─── SECTION 2: FAMILY GROUP & REMOTE SHARING ───────────
            Text(
                text = "Family Group & Remote Sharing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // If no family groups exist, show creation form
            if (groupState.groups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Create Your Family Group", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name (e.g. Parents)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = memberName,
                            onValueChange = { memberName = it },
                            label = { Text("Member Nickname (e.g. Papa)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = memberPhone,
                            onValueChange = { memberPhone = it },
                            label = { Text("Member Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                if (groupName.trim().isEmpty() || memberName.trim().isEmpty() || memberPhone.trim().isEmpty()) {
                                    Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    groupViewModel.createGroup(groupName, memberPhone, memberName)
                                    groupName = ""
                                    memberName = ""
                                    memberPhone = ""
                                    Toast.makeText(context, "Group Created & Invite Sent!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Group & Invite Member")
                        }
                    }
                }
            } else {
                // Display active group and members list
                val currentGroup = groupState.groups.first()
                val members = groupState.groupMembers[currentGroup.id] ?: emptyList()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Group: ${currentGroup.name}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        members.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${m.name} (${m.phone})")
                                Text(
                                    text = m.status,
                                    color = if (m.status == "ACCEPTED") Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shared Remote Reminder setup
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Schedule Alarm for Group Members", fontWeight = FontWeight.Bold)
                        
                        // Simple choice list representing members
                        Text("Deliver to Member:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.forEach { m ->
                                val isSelected = selectedMemberId == m.userId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMemberId = m.userId },
                                    label = { Text(m.name) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = sharedMessage,
                            onValueChange = { sharedMessage = it },
                            label = { Text("Reminder Message") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            sharedHour = h
                                            sharedMinute = m
                                            sharedTimeSelected = true
                                        },
                                        sharedHour,
                                        sharedMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (sharedTimeSelected)
                                        String.format("Time: %02d:%02d", sharedHour, sharedMinute)
                                    else "Choose Time"
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedMemberId.isEmpty() || sharedMessage.trim().isEmpty() || !sharedTimeSelected) {
                                        Toast.makeText(context, "Select recipient, type message & time", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val calendar = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, sharedHour)
                                            set(Calendar.MINUTE, sharedMinute)
                                            set(Calendar.SECOND, 0)
                                            if (timeInMillis <= System.currentTimeMillis()) {
                                                add(Calendar.DAY_OF_YEAR, 1)
                                            }
                                        }
                                        val targetMemberName = members.find { it.userId == selectedMemberId }?.name ?: "Papa"
                                        val reminder = Reminder(
                                            title = "Care reminder for $targetMemberName",
                                            message = sharedMessage,
                                            triggerTime = calendar.timeInMillis,
                                            recurrenceType = RecurrenceType.DAILY,
                                            recurrenceIntervalMs = 0L,
                                            repeatIntervalMinutes = settingsState.repeatIntervalMin,
                                            maxRepetitions = settingsState.maxRepetitions,
                                            isActive = true,
                                            creatorId = "local_user",
                                            recipientId = selectedMemberId,
                                            syncStatus = "PENDING_SYNC"
                                        )
                                        viewModel.processTranscript("")
                                        viewModel.updateParsedReminder(reminder)
                                        viewModel.confirmParsedReminder()

                                        // Reset shared forms
                                        sharedMessage = ""
                                        sharedTimeSelected = false
                                        Toast.makeText(context, "Shared Alarm scheduled remotely!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Send Alarm")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Settings Configuration Dialog (Houses all preferences & diagnostics)
    if (showSettingsDialog) {
        var diagnosticsViewerOpen by remember { mutableStateOf(false) }
        var showVoiceCloningDialog by remember { mutableStateOf(false) }
        var calibrationStep by remember { mutableStateOf(0) }
        val calibrationPhrases = listOf(
            "Hello Papa, this is Sandeep. Just checking in on you.",
            "Remember to drink water and take your daily walk.",
            "Your health is important to us. Please take your medicine now."
        )

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    "Settings & Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Premium Section ──
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsViewModel.updatePremium(!settingsState.isPremium)
                                Toast
                                    .makeText(
                                        context,
                                        if (!settingsState.isPremium) "Premium Member Active!" else "Premium status reset.",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (settingsState.isPremium) Color(0xFFD97706).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (settingsState.isPremium) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (settingsState.isPremium) "👑" else "★", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (settingsState.isPremium) "EchoCare Premium Active" else "Try Premium Features",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tap to toggle mock billing subscription.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // ── Languages ──
                    Text("TTS Language Selection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settingsState.ttsLanguage == "en",
                            onClick = { settingsViewModel.updateLanguage("en") },
                            label = { Text("🇬🇧 EN") }
                        )
                        FilterChip(
                            selected = settingsState.ttsLanguage == "hi",
                            onClick = { settingsViewModel.updateLanguage("hi") },
                            label = { Text("🇮🇳 HI") }
                        )
                    }

                    // ── Speech Speeds ──
                    Text("Voice customization settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Column {
                        Text("Speech Speed: ${settingsState.ttsSpeed}x", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = settingsState.ttsSpeed,
                            onValueChange = settingsViewModel::updateSpeed,
                            valueRange = 0.5f..2.0f
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Speech Pitch: ${settingsState.voicePitch}x", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = settingsState.voicePitch,
                            onValueChange = settingsViewModel::updateVoicePitch,
                            valueRange = 0.5f..2.0f
                        )
                    }

                    // ── Voice Cloning ──
                    Button(
                        onClick = {
                            if (settingsState.isPremium) showVoiceCloningDialog = true
                            else Toast.makeText(context, "Requires Premium plan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Voice Cloning 🎙️")
                    }

                    // ── Backup Restore ──
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (settingsState.isPremium) {
                                    settingsViewModel.backupReminders(
                                        context,
                                        onSuccess = { Toast.makeText(context, "Backup exported!", Toast.LENGTH_SHORT).show() },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    Toast.makeText(context, "Requires Premium!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Backup")
                        }
                        Button(
                            onClick = {
                                if (settingsState.isPremium) {
                                    settingsViewModel.restoreReminders(
                                        context,
                                        onSuccess = { Toast.makeText(context, "Reminders imported!", Toast.LENGTH_SHORT).show() },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    Toast.makeText(context, "Requires Premium!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Backup")
                        }
                    }

                    // ── Security features ──
                    Text("Security configurations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Biometric App Lock")
                        Switch(
                            checked = settingsState.biometricEnabled,
                            onCheckedChange = settingsViewModel::toggleBiometric
                        )
                    }

                    // ── Diagnostics LogViewer ──
                    Button(
                        onClick = { diagnosticsViewerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Open Logcat Diagnostics")
                    }

                    // ── Mock Crash Simulator ──
                    Button(
                        onClick = { settingsViewModel.triggerFatalCrash() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Simulate Fatal Crash")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )

        // Sub-dialogs inside settings
        if (diagnosticsViewerOpen) {
            AlertDialog(
                onDismissRequest = { diagnosticsViewerOpen = false },
                title = { Text("Diagnostics Telemetry logs") },
                text = {
                    val logs = remember { EchoCareTelemetry.getLogs() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logs.forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.eventName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(log.timestamp.substringAfter(" "), fontSize = 10.sp)
                                }
                                if (log.details.isNotEmpty()) {
                                    Text(log.details.toString(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { diagnosticsViewerOpen = false }) { Text("Close") }
                }
            )
        }

        if (showVoiceCloningDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceCloningDialog = false },
                title = { Text("Voice Cloning calibration prompt") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Phrase ${calibrationStep + 1} of 3: Read aloud.")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                "\"${calibrationPhrases[calibrationStep]}\"",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (calibrationStep < 2) calibrationStep++
                            else {
                                showVoiceCloningDialog = false
                                calibrationStep = 0
                                Toast.makeText(context, "Calibrated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(if (calibrationStep < 2) "Next" else "Finish")
                    }
                }
            )
        }
    }
}
