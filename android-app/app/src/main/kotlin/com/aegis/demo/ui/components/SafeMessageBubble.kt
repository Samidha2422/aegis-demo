package com.aegis.demo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aegis.demo.data.model.SafeMessage

@Composable
fun SafeMessageBubble(
    message: SafeMessage,
    isSelf: Boolean,
    onInspectSafety: (SafeMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBlurred by remember { mutableStateOf(message.safety.state == "Blurred") }
    var isHeldExpanded by remember { mutableStateOf(false) }

    val safety = message.safety
    val state = safety.state

    val bubbleAlignment = if (isSelf) Alignment.End else Alignment.Start
    val bubbleShape = if (isSelf) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    val bubbleColor = when {
        isSelf -> MaterialTheme.colorScheme.primaryContainer
        state == "Held" || state == "Hidden" -> MaterialTheme.colorScheme.surfaceVariant
        state == "Warned" -> Color(0xFFFFF8E1) // Soft amber
        state == "Escalated" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val contentColor = when {
        isSelf -> MaterialTheme.colorScheme.onPrimaryContainer
        state == "Escalated" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            tonalElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .then(
                    if (state == "Warned") Modifier.border(1.dp, Color(0xFFFFB300), bubbleShape)
                    else if (state == "Escalated") Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, bubbleShape)
                    else Modifier
                )
                .clickable {
                    if (safety.severity > 0 || safety.reasons.isNotEmpty()) {
                        onInspectSafety(message)
                    }
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Safety badge header if flagged or held
                if (state != "Delivered" && state != "TranscriptOnly") {
                    SafetyStateHeader(
                        state = state,
                        severity = safety.severity,
                        simulated = safety.simulated,
                        onInspect = { onInspectSafety(message) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (state) {
                    "Scanning" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aegis analyzing message risk...",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    "Held", "Hidden" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Held",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Message Held for Safety",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Automated quarantine based on behavioral risk pattern.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHeldExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isHeldExpanded = !isHeldExpanded },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isHeldExpanded) "Hide" else "Reveal (Victim Discretion)",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    "TranscriptOnly" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneCallback,
                                contentDescription = "Voice Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYNTHETIC AUDIO TRANSCRIPT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.transcript ?: message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }

                    else -> {
                        // Regular or Warned/Blurred message
                        if (message.kind == "image") {
                            BlurOverlay(
                                isBlurred = isBlurred,
                                onToggleBlur = { isBlurred = !isBlurred },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Demo Image",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "DEMO PROXY IMAGE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(6.dp)
                                    )
                                }
                            }
                            if (message.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        } else {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (safety.simulated) {
                        Surface(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SIMULATED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyStateHeader(
    state: String,
    severity: Float,
    simulated: Boolean,
    onInspect: () -> Unit
) {
    val (icon, label, color) = when (state) {
        "Warned" -> Triple(Icons.Default.Warning, "FLAGGED BY AEGIS", Color(0xFFF57C00))
        "Blurred" -> Triple(Icons.Default.VisibilityOff, "SENSITIVE MEDIA BLURRED", Color(0xFFD32F2F))
        "Held" -> Triple(Icons.Default.Shield, "HELD IN QUARANTINE", MaterialTheme.colorScheme.error)
        "Escalated" -> Triple(Icons.Default.Security, "CRITICAL HARASSMENT RISK", MaterialTheme.colorScheme.error)
        "EngineUnavailable" -> Triple(Icons.Default.CloudOff, "OFFLINE RULE ENGINE", Color.Gray)
        else -> Triple(Icons.Default.Info, state.uppercase(), MaterialTheme.colorScheme.primary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable { onInspect() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (severity > 0) {
            Text(
                text = "Score: ${severity.toInt()}",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
