package com.echocare.app.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.echocare.app.domain.model.Reminder
import com.echocare.app.ui.screens.settings.SettingsUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreationOverlay(
    onDismiss: () -> Unit,
    viewModel: HomeViewModel,
    settingsState: SettingsUiState
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Infinite transitions for the pulsing mic button rings and soundwave
    val infiniteTransition = rememberInfiniteTransition(label = "mic_rings")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_2"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    // Preset voice command options
    val suggestions = listOf(
        "Wake me up tomorrow at 5 AM",
        "Remind me to drink water every 2 hours",
        "Remind me every evening at 7 PM to walk"
    )

    Dialog(
        onDismissRequest = {
            viewModel.cancelListening()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF9FAFC)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // ─── DIALOG SCROLL CONTENT ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ─── TOP HEADER ROW ───────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.cancelListening()
                            onDismiss()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Text(
                            text = "Create Alarm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        
                        // Empty spacer for header alignment balancing
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    // ─── ANIMATED SOUNDWAVE VISUALIZER ───────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val midY = height / 2f
                            val amplitude = if (uiState.isListening) 25f else 5f
                            
                            val strokeWidth = 3.dp.toPx()
                            val step = 4f
                            
                            val wavePath = androidx.compose.ui.graphics.Path()
                            wavePath.moveTo(0f, midY)
                            
                            var x = 0f
                            while (x < width) {
                                // Sine wave equation combining offset animation and amplitude
                                val y = midY + sin((x / width * 4f * Math.PI.toFloat()) + waveOffset) * amplitude
                                wavePath.lineTo(x, y)
                                x += step
                            }
                            
                            drawPath(
                                path = wavePath,
                                color = Color(0xFF6366F1),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }

                    // ─── MICROPHONE BUTTON & PULSING RINGS ────────────────────
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isListening) {
                            // Ring 1
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(pulseScale1)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1).copy(alpha = pulseAlpha1))
                            )
                            // Ring 2
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(pulseScale2)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = pulseAlpha2))
                            )
                        }

                        // Main Button Circle
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                    )
                                )
                                .clickable {
                                    if (uiState.isListening) {
                                        viewModel.stopListening()
                                    } else {
                                        viewModel.startListening()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listen Button",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    // ─── STATUS TEXT ──────────────────────────────────────────
                    Text(
                        text = if (uiState.isListening) "Listening..." else "Tap and speak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Text(
                        text = if (uiState.isListening) 
                            (uiState.voiceTranscript.ifBlank { "Say something..." }) 
                        else 
                            "Example: \"Wake me up tomorrow at 5 AM\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── SUGGESTIONS PANEL ("Try saying something like") ───────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Try saying something like",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B5563),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        suggestions.forEach { command ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Auto-simulate preset tap parsing
                                        viewModel.processTranscript(command)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speaker",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = command,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                            }
                        }
                    }

                    // ─── LANGUAGE FOOTER ──────────────────────────────────────
                    Text(
                        text = "You can speak in English or Hindi",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // ─── CONFIRMATION CARD DIALOG ───────────────────────────────
                if (uiState.showParsedConfirmation && uiState.parsedReminder != null) {
                    val reminder = uiState.parsedReminder!!
                    
                    Dialog(
                        onDismissRequest = { viewModel.dismissConfirmation() },
                        properties = DialogProperties(dismissOnClickOutside = false)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Confirm Alarm",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Title:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6366F1),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = reminder.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                        
                                        if (!reminder.message.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Announced Speech:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6366F1),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = reminder.message ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF4B5563)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Trigger Time:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6366F1),
                                            fontWeight = FontWeight.Bold
                                        )
                                        val sdf = SimpleDateFormat("EEEE h:mm a", Locale.getDefault())
                                        val timeStr = sdf.format(Date(reminder.triggerTime))
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.dismissConfirmation() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.confirmParsedReminder()
                                            Toast.makeText(context, "Alarm created successfully!", Toast.LENGTH_SHORT).show()
                                            onDismiss() // Close voice overlay completely
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Text("Save Alarm")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
