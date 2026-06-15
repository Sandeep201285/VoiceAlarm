package com.echocare.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echocare.app.util.EchoCareTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(1) } // 0 = Monthly, 1 = Yearly, 2 = Lifetime
    
    val plans = listOf(
        PlanOption("Monthly Care", "₹199", "per month"),
        PlanOption("Yearly Care", "₹999", "per year (Save 60%)", isBestValue = true),
        PlanOption("Lifetime Care", "₹2,999", "one-time purchase")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0B24),
                        Color(0xFF1B0F3A),
                        Color(0xFF0F0B24)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Exit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Crown / Star Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Title
            Text(
                text = "EchoCare Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFBBF24),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Keep your loved ones safe with advanced care features",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Checklist Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumFeatureRow(
                    title = "AI Voice Cloning",
                    desc = "Speak alerts in your actual recorded voice so Papa recognizes it instantly."
                )
                PremiumFeatureRow(
                    title = "Caregiver Groups (Up to 10)",
                    desc = "Broadcast spoken reminders to up to 10 family devices simultaneously."
                )
                PremiumFeatureRow(
                    title = "Smart Home Integrations",
                    desc = "Directly link schedules to Alexa, Google Home, and Wear OS watches."
                )
                PremiumFeatureRow(
                    title = "Cloud Backups",
                    desc = "Never lose your reminders. Safe database replication and restore."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan Selector List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                plans.forEachIndexed { index, plan ->
                    val isSelected = selectedPlan == index
                    val borderColor = if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.1f)
                    val bgAlpha = if (isSelected) 0.08f else 0.03f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = bgAlpha))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlan = index }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plan.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (plan.isBestValue) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFD97706))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "BEST VALUE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = plan.sub,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = plan.price,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color(0xFFFBBF24) else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    EchoCareTelemetry.logEvent("paywall_purchase_completed", mapOf("plan_index" to selectedPlan))
                    Toast.makeText(context, "Premium Subscription Unlocked!", Toast.LENGTH_SHORT).show()
                    onSubscribed()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBBF24),
                    contentColor = Color(0xFF1B0F3A)
                )
            ) {
                Text(
                    text = "Unlock Premium Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer / Restore purchases
            Text(
                text = "Restore Purchases",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Simulated purchase restored.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun PremiumFeatureRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}

data class PlanOption(
    val title: String,
    val price: String,
    val sub: String,
    val isBestValue: Boolean = false
)
