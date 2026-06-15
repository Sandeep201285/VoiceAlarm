package com.echocare.app.ui.screens.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echocare.app.domain.model.RecurrenceType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    viewModel: AddReminderViewModel = hiltViewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigate back on successful save
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Reminder" else "New Reminder",
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
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ─── Error Banner ─────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ─── Title ────────────────────────────────────────────────────
            SectionLabel(text = "Reminder Details")
            OutlinedTextField(
                value         = uiState.title,
                onValueChange = viewModel::updateTitle,
                label         = { Text("Title") },
                placeholder   = { Text("e.g. Morning Medicine") },
                leadingIcon   = { Icon(Icons.Outlined.Label, null) },
                shape         = RoundedCornerShape(14.dp),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            OutlinedTextField(
                value         = uiState.message,
                onValueChange = viewModel::updateMessage,
                label         = { Text("Spoken Message") },
                placeholder   = { Text("What should be announced?") },
                leadingIcon   = { Icon(Icons.Outlined.RecordVoiceOver, null) },
                shape         = RoundedCornerShape(14.dp),
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4
            )

            // ─── Date & Time ──────────────────────────────────────────────
            SectionLabel(text = "When")

            val calendar = Calendar.getInstance().also {
                it.timeInMillis = uiState.triggerTimeMs
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date picker
                PickerChip(
                    label = "📅  ${SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(calendar.time)}",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(year, month, day)
                                viewModel.updateTriggerTime(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).also {
                            it.datePicker.minDate = System.currentTimeMillis()
                        }.show()
                    }
                )
                // Time picker
                PickerChip(
                    label = "🕐  ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)}",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                viewModel.updateTriggerTime(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                )
            }

            // ─── Recurrence ───────────────────────────────────────────────
            SectionLabel(text = "Repeat Schedule")

            val recurrenceOptions = RecurrenceType.values()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recurrenceOptions.forEach { type ->
                    RecurrenceOption(
                        type       = type,
                        isSelected = uiState.recurrenceType == type,
                        onClick    = { viewModel.updateRecurrenceType(type) }
                    )
                }
            }

            // Custom interval (only when CUSTOM selected)
            AnimatedVisibility(visible = uiState.recurrenceType == RecurrenceType.CUSTOM) {
                Column {
                    Text(
                        "Repeat every",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value        = uiState.recurrenceIntervalHours.toFloat(),
                        onValueChange = { viewModel.updateRecurrenceInterval(it.toInt()) },
                        valueRange   = 1f..24f,
                        steps        = 22,
                        colors       = SliderDefaults.colors(
                            thumbColor       = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        "${uiState.recurrenceIntervalHours} hour(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── Repeat Until Acknowledged ────────────────────────────────
            SectionLabel(text = "Repeat Until Acknowledged")

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Interval: ${uiState.repeatIntervalMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.repeatIntervalMinutes.toFloat(),
                        onValueChange = { viewModel.updateRepeatInterval(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Max times: ${if (uiState.maxRepetitions == 0) "∞" else "${uiState.maxRepetitions}"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.maxRepetitions.toFloat(),
                        onValueChange = { viewModel.updateMaxRepetitions(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Save Button ──────────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEditMode) "Update Reminder" else "Set Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RecurrenceOption(
    type: RecurrenceType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (emoji) = when (type) {
        RecurrenceType.ONCE    -> listOf("1️⃣")
        RecurrenceType.DAILY   -> listOf("🔄")
        RecurrenceType.WEEKLY  -> listOf("📅")
        RecurrenceType.MONTHLY -> listOf("🗓️")
        RecurrenceType.CUSTOM  -> listOf("⚙️")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text = type.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PickerChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
