package com.echocare.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isPast: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val cardAlpha   = if (isPast || !reminder.isActive) 0.5f else 1f
    val accentColor = when {
        isPast              -> MaterialTheme.colorScheme.outline
        !reminder.isActive  -> MaterialTheme.colorScheme.outline
        else                -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // ─── Left accent bar ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Max)
                        .background(
                            color = accentColor,
                            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                        )
                        .defaultMinSize(minHeight = 80.dp)
                )

                // ─── Content ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Title row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Recurrence badge
                        if (!isPast) {
                            RecurrenceBadge(reminder.recurrenceType)
                        }
                    }

                    // Message preview
                    Text(
                        text = reminder.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Time row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatTriggerTime(reminder.triggerTime, isPast),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ─── Actions Column ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toggle active / inactive
                    if (!isPast) {
                        Switch(
                            checked = reminder.isActive,
                            onCheckedChange = { onToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(width = 48.dp, height = 24.dp)
                        )
                    }

                    // Edit button
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // ─── Delete Confirmation Dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reminder?") },
            text  = { Text("\"${reminder.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecurrenceBadge(type: RecurrenceType) {
    val (emoji, label) = when (type) {
        RecurrenceType.ONCE    -> "1️⃣" to "Once"
        RecurrenceType.DAILY   -> "🔄" to "Daily"
        RecurrenceType.WEEKLY  -> "📅" to "Weekly"
        RecurrenceType.MONTHLY -> "🗓️" to "Monthly"
        RecurrenceType.CUSTOM  -> "⚙️" to "Custom"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

private fun formatTriggerTime(epochMs: Long, isPast: Boolean): String {
    val now  = System.currentTimeMillis()
    val diff = epochMs - now
    return when {
        isPast -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(epochMs))
        diff < 60_000L       -> "In less than a minute"
        diff < 3_600_000L    -> "In ${diff / 60_000} min"
        diff < 86_400_000L   -> "Today at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(epochMs))}"
        diff < 172_800_000L  -> "Tomorrow at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(epochMs))}"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(epochMs))
    }
}
