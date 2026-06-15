package com.echocare.app.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onSelectTemplate: (String) -> Unit,
    onBack: () -> Unit
) {
    val templates = listOf(
        ReminderTemplate(
            emoji = "💊",
            title = "BP Medicine for Papa",
            phrase = "Tell Papa to take his BP medicine every day at 8 PM",
            desc = "Schedules a daily alarm at 8:00 PM for Papa's phone."
        ),
        ReminderTemplate(
            emoji = "💧",
            title = "Drink Water (Self)",
            phrase = "Remind me to drink water every 2 hours",
            desc = "Schedules a custom hourly reminder recurring every 2 hours."
        ),
        ReminderTemplate(
            emoji = "🏃",
            title = "Stretch & Walk Break",
            phrase = "Remind me to walk for 15 minutes in 1 hour",
            desc = "Schedules a one-time reminder in 1 hour."
        ),
        ReminderTemplate(
            emoji = "🩺",
            title = "Doctor Appointment (Papa)",
            phrase = "Tell Papa doctor checkup tomorrow at 11 AM",
            desc = "Schedules a one-time alarm for Papa's phone tomorrow at 11:00 AM."
        ),
        ReminderTemplate(
            emoji = "📝",
            title = "Weekly Timesheet Submissions",
            phrase = "Remind me to submit timesheet every Friday at 4 PM",
            desc = "Schedules a weekly reminder recurring every Friday at 4:00 PM."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quick Templates",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Tap a template to parse and configure it instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTemplate(template.phrase) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(template.emoji, fontSize = 28.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"${template.phrase}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ReminderTemplate(
    val emoji: String,
    val title: String,
    val phrase: String,
    val desc: String
)
