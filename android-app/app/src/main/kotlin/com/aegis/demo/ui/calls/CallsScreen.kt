package com.aegis.demo.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.CallRecord
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.components.SeverityMeter
import com.aegis.demo.ui.theme.SafetyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onOpenSafetyCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember { ServiceLocator.getRepository() }
    val scope = rememberCoroutineScope()
    val calls by repository.getCalls().collectAsState(initial = emptyList())

    var showSimulateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Call Records",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Scenario 6: Overnight Call Spillover",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                repository.simulateCall("unknown_stalker")
                            }
                            showSimulateDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulate Call",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Scenario 6 behavioral warning header
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SafetyTheme.criticalRiskContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = SafetyTheme.criticalRisk
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "QUIET-HOUR CALL SPIKE DETECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyTheme.criticalRisk
                                )
                                Text(
                                    text = "40 calls between 01:00 AM – 05:30 AM (92% unanswered).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SafetyTheme.onCriticalRiskContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "Risk: 96/100 (Critical)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyTheme.criticalRisk,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            TextButton(
                                onClick = onOpenSafetyCenter,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "View in Safety Center",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafetyTheme.criticalRisk
                                )
                            }
                        }
                    }
                }
            }

            items(calls, key = { it.id }) { call ->
                CallRecordItem(call = call)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Live Simulated Call Dialog
        if (showSimulateDialog) {
            AlertDialog(
                onDismissRequest = { showSimulateDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = null,
                        tint = SafetyTheme.highRisk
                    )
                },
                title = {
                    Text(
                        text = "Incoming Call (Quiet Hours)",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Caller: Unknown Contact (SIMULATED)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Timestamp: 03:14 AM | 40 prior attempts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SeverityMeter(
                            severity = 88f,
                            tier = "high"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "SYNTHETIC AUDIO TRANSCRIPT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"Pick up right now. You know you can't ignore me forever.\"",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.blockUser("unknown_stalker")
                            }
                            showSimulateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Decline & Log Evidence")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSimulateDialog = false }) {
                        Text("Ignore")
                    }
                }
            )
        }
    }
}

@Composable
fun CallRecordItem(
    call: CallRecord,
    modifier: Modifier = Modifier
) {
    val isRisk = call.safety.severity >= 60f
    val (badgeBg, badgeFg) = if (isRisk) {
        Pair(SafetyTheme.highRiskContainer, SafetyTheme.onHighRiskContainer)
    } else {
        Pair(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isRisk) SafetyTheme.highRiskContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (call.status == "missed") Icons.Default.PhoneMissed else Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = if (isRisk) SafetyTheme.highRisk else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.caller_id,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${call.timestamp} • ${call.duration_secs}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (call.safety.simulated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• SIMULATED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (isRisk) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeBg
            ) {
                Text(
                    text = "Risk: ${call.safety.severity.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
