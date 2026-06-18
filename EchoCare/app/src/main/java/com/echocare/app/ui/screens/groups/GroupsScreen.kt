package com.echocare.app.ui.screens.groups

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
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
import com.echocare.app.ui.screens.home.HomeViewModel
import com.echocare.app.ui.screens.settings.SettingsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBackClick: () -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val groupState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Form inputs
    var groupName by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var memberPhone by remember { mutableStateOf("") }

    // Shared remote reminder inputs
    var selectedMemberId by remember { mutableStateOf("") }
    var sharedMessage by remember { mutableStateOf("") }
    var sharedHour by remember { mutableStateOf(20) }
    var sharedMinute by remember { mutableStateOf(0) }
    var sharedTimeSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Family Groups",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9FAFC)
                )
            )
        },
        containerColor = Color(0xFFF9FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ─── IF NO GROUPS EXIST: CREATION CARD ────────────────────────
            if (groupState.groups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Add Group",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Create Your Family Group",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        Text(
                            text = "Add your parents, kids, or friends to send them voice reminders remotely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name (e.g. Caregivers)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = memberName,
                            onValueChange = { memberName = it },
                            label = { Text("Member Nickname (e.g. Papa)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = memberPhone,
                            onValueChange = { memberPhone = it },
                            label = { Text("Member Phone Number") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (groupName.isBlank() || memberName.isBlank() || memberPhone.isBlank()) {
                                    Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.createGroup(groupName, memberPhone, memberName)
                                    groupName = ""
                                    memberName = ""
                                    memberPhone = ""
                                    Toast.makeText(context, "Group Created & Invite Sent!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Create Group & Invite", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ─── ACTIVE GROUP & MEMBERS LIST ──────────────────────────
                val currentGroup = groupState.groups.first()
                val members = groupState.groupMembers[currentGroup.id] ?: emptyList()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Group",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentGroup.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = "Active Care Group",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        Divider(color = Color(0xFFF3F4F6))

                        Text(
                            text = "Group Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )

                        members.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF9FAFC))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = m.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        text = m.phone,
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (m.status == "ACCEPTED") Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = m.status,
                                        color = if (m.status == "ACCEPTED") Color(0xFF065F46) else Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── DISPATCH REMINDER REMOTE CARD ────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Schedule Remote Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        Text(
                            text = "Select a group member to send an exact voice reminder directly to their phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )

                        Text("Recipient:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            members.forEach { m ->
                                val isSelected = selectedMemberId == m.userId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMemberId = m.userId },
                                    label = { Text(m.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFECFDF5),
                                        selectedLabelColor = Color(0xFF065F46)
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = sharedMessage,
                            onValueChange = { sharedMessage = it },
                            label = { Text("Spoken Voice Message (e.g. Take medicine)") },
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
                                            sharedHour = h
                                            sharedMinute = m
                                            sharedTimeSelected = true
                                        },
                                        sharedHour,
                                        sharedMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151))
                            ) {
                                Text(
                                    if (sharedTimeSelected)
                                        String.format("Time: %02d:%02d", sharedHour, sharedMinute)
                                    else "Set Trigger Time"
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedMemberId.isBlank() || sharedMessage.isBlank() || !sharedTimeSelected) {
                                        Toast.makeText(context, "Fill in recipient, message & time", Toast.LENGTH_SHORT).show()
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
                                        homeViewModel.updateParsedReminder(reminder)
                                        homeViewModel.confirmParsedReminder()

                                        sharedMessage = ""
                                        sharedTimeSelected = false
                                        Toast.makeText(context, "Remote Alarm Sent!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Send Alarm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
