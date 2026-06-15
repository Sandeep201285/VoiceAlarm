package com.echocare.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echocare.app.domain.model.GroupMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    groupId: String,
    viewModel: GroupViewModel,
    onBack: () -> Unit
) {
    val membersFlow = remember(groupId) { viewModel.getMembers(groupId) }
    val members by membersFlow.collectAsState(initial = emptyList())
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Group Members",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Loading members...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Members list and invitation status. Tap PENDING to simulate accept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(members, key = { it.userId }) { member ->
                MemberItem(
                    member = member,
                    onStatusClick = {
                        if (member.status == "PENDING") {
                            viewModel.updateStatus(groupId, member.userId, "ACCEPTED")
                        } else {
                            viewModel.updateStatus(groupId, member.userId, "PENDING")
                        }
                    }
                )
            }
        }

        // Add Member Dialog
        if (showAddMemberDialog) {
            AlertDialog(
                onDismissRequest = { showAddMemberDialog = false },
                title = { Text("Invite Member") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            label = { Text("Name") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newMemberPhone,
                            onValueChange = { newMemberPhone = it },
                            label = { Text("Phone") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newMemberName.isNotBlank() && newMemberPhone.isNotBlank()) {
                                viewModel.createGroup(
                                    name = "", // Create direct handles
                                    guestMembers = listOf(Pair(newMemberName.trim(), newMemberPhone.trim()))
                                )
                                newMemberName = ""
                                newMemberPhone = ""
                                showAddMemberDialog = false
                            }
                        }
                    ) {
                        Text("Invite")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemberDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MemberItem(
    member: GroupMember,
    onStatusClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = if (member.role == "OWNER") MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.secondary
                    ) {
                        Text(member.role, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                if (member.phone.isNotBlank()) {
                    Text(
                        text = member.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clickable status tag to simulate accept/decline
            val statusColor = if (member.status == "ACCEPTED") MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.errorContainer
            val statusTextColor = if (member.status == "ACCEPTED") MaterialTheme.colorScheme.onPrimaryContainer
                                  else MaterialTheme.colorScheme.onErrorContainer

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor)
                    .clickable(onClick = onStatusClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = member.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor
                )
            }
        }
    }
}
