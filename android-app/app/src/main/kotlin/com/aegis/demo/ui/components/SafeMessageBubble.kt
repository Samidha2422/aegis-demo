package com.aegis.demo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.SafeMessage
import com.aegis.demo.ui.theme.SafetyTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SafeMessageBubble(
    message: SafeMessage,
    isSelf: Boolean,
    onInspectSafety: (SafeMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBlurred by remember { mutableStateOf(message.safety.state == "Blurred") }
    var isHeldExpanded by remember { mutableStateOf(false) }
    var isVoicePlaying by remember { mutableStateOf(false) }

    val safety = message.safety
    val state = safety.state

    val bubbleAlignment = if (isSelf) Alignment.End else Alignment.Start
    val bubbleShape = if (isSelf) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    val bubbleColor = when {
        isSelf -> MaterialTheme.colorScheme.primaryContainer
        state == "Held" || state == "Hidden" -> MaterialTheme.colorScheme.surfaceVariant
        state == "Warned" -> SafetyTheme.reviewRiskContainer
        state == "Escalated" -> SafetyTheme.highRiskContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isSelf -> MaterialTheme.colorScheme.onPrimaryContainer
        state == "Warned" -> SafetyTheme.onReviewRiskContainer
        state == "Escalated" -> SafetyTheme.onHighRiskContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            tonalElevation = if (isSelf) 0.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
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
                                text = "Evaluating behavioral signals...",
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Message Held for Safety",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Automated quarantine based on behavioral risk pattern.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHeldExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isHeldExpanded = !isHeldExpanded },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isHeldExpanded) "Hide" else "Reveal (Discretion)",
                                    style = MaterialTheme.typography.labelSmall
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
                                style = MaterialTheme.typography.labelSmall,
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = "Demo Image",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "DEMO PROXY IMAGE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (message.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        } else if (message.kind == "audio") {
                            // Voice Note Waveform representation
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "SYNTHETIC AUDIO (16kHz)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { isVoicePlaying = !isVoicePlaying },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isVoicePlaying) "Pause" else "Play",
                                            tint = contentColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LinearWavyProgressIndicator(
                                        progress = { if (isVoicePlaying) 0.6f else 0.1f },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "0:03",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (message.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor
                                    )
                                }
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

                // C1 Baseline Keyword Comparison indicator
                if (safety.severity >= 60f && !isSelf) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SafetyTheme.keywordMissedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Keyword: 0 Flags",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafetyTheme.keywordMissed
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "AI Risk: ${safety.severity.toInt()}/100",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafetyTheme.engineCaught
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
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SIMULATED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onInspect: () -> Unit
) {
    val (icon, label, badgeBg, badgeFg) = when (state) {
        "Warned" -> Quadruple(Icons.Default.Warning, "FLAGGED BY AEGIS", SafetyTheme.reviewRiskContainer, SafetyTheme.onReviewRiskContainer)
        "Blurred" -> Quadruple(Icons.Default.VisibilityOff, "SENSITIVE MEDIA BLURRED", SafetyTheme.highRiskContainer, SafetyTheme.onHighRiskContainer)
        "Held" -> Quadruple(Icons.Default.Shield, "HELD IN QUARANTINE", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "Escalated" -> Quadruple(Icons.Default.Security, "CRITICAL RISK", SafetyTheme.criticalRiskContainer, SafetyTheme.onCriticalRiskContainer)
        "EngineUnavailable" -> Quadruple(Icons.Default.CloudOff, "RULE ENGINE FALLBACK", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Quadruple(Icons.Default.Info, state.uppercase(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = badgeFg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (severity > 0) {
                Text(
                    text = "${severity.toInt()}/100",
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
