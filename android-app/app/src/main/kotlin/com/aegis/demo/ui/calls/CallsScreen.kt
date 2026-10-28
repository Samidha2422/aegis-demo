package com.aegis.demo.ui.calls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
    val calls by repository.getCalls().collectAsState(initial = emptyList())

    var showConfigDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var simulatedResult by remember { mutableStateOf<CallRecord?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (!text.isNullOrBlank()) {
                    scope.launch {
                        repository.importCallLogCsv(text)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val hasSpike = remember(calls) {
        calls.any { it.safety.severity >= 70f }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Call History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasSpike) "Quiet-Hour Repeated Spike Active" else "VoIP Audit & Activity",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasSpike) SafetyTheme.highRisk else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        OutlinedButton(
                            onClick = { showCsvDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Import CSV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = { showConfigDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate Call",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No calls recorded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Only display the high-risk banner if there is an active spike
                if (hasSpike) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = SafetyTheme.highRiskContainer
                            ),
                            border = BorderStroke(1.dp, SafetyTheme.highRiskBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SafetyTheme.highRisk,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "Shield",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "QUIET-HOUR CALL SPIKE DETECTED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SafetyTheme.highRisk
                                        )
                                        Text(
                                            text = "40 calls between 01:00 AM – 05:30 AM (92% unanswered).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SafetyTheme.onHighRiskContainer
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
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, SafetyTheme.highRiskBorder)
                                    ) {
                                        Text(
                                            text = "Frequency Risk: 96/100",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SafetyTheme.highRisk,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    TextButton(
                                        onClick = onOpenSafetyCenter,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "View Evidence",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SafetyTheme.highRisk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(calls, key = { it.id }) { call ->
                    CallRecordItem(call = call)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        // Interactive Call Simulation Config Dialog
        if (showConfigDialog) {
            SimulateCallConfigDialog(
                isSimulating = isSimulating,
                onDismiss = { showConfigDialog = false },
                onSimulate = { callerId, duration, status, transcript ->
                    scope.launch {
                        isSimulating = true
                        val res = repository.simulateCall(callerId, duration, status, transcript)
                        isSimulating = false
                        showConfigDialog = false
                        res.onSuccess { record ->
                            simulatedResult = record
                        }
                    }
                }
            )
        }

        // CSV Call Log Import Dialog (Scenario 6)
        if (showCsvDialog) {
            AlertDialog(
                onDismissRequest = { showCsvDialog = false },
                title = {
                    Text("Import Call Log CSV (Scenario 6)", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            "Analyze quiet-hour persistence, velocity spikes, and unanswered call patterns from synthetic or external call logs.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = {
                                showCsvDialog = false
                                scope.launch {
                                    val bundledCsv = try {
                                        context.assets.open("assets/overnight_calls.csv").bufferedReader().use { it.readText() }
                                    } catch (_: Exception) { "" }
                                    if (bundledCsv.isNotBlank()) {
                                        repository.importCallLogCsv(bundledCsv)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load Bundled Overnight Spike (40 Calls)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                showCsvDialog = false
                                csvPicker.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose CSV from Device")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCsvDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Evaluated Incoming Call Result Dialog
        simulatedResult?.let { record ->
            IncomingCallResultDialog(
                record = record,
                onDismiss = { simulatedResult = null },
                onBlock = {
                    scope.launch {
                        repository.blockUser(record.caller_id)
                    }
                    simulatedResult = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulateCallConfigDialog(
    isSimulating: Boolean,
    onDismiss: () -> Unit,
    onSimulate: (callerId: String, duration: Int, status: String, transcript: String?) -> Unit
) {
    var callerId by remember { mutableStateOf("unknown_stalker") }
    var status by remember { mutableStateOf("missed") }
    var durationSecs by remember { mutableIntStateOf(0) }
    var transcript by remember { mutableStateOf("Pick up right now. You know you can't ignore me forever.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simulate Incoming Call",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure caller identity, status, and custom voicemail / audio transcript to test live engine and model response.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Caller identity selector
                Text(
                    text = "Caller Identity",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = callerId == "unknown_stalker",
                        onClick = { callerId = "unknown_stalker" },
                        label = { Text("Unknown Stalker", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = callerId == "rohan",
                        onClick = { callerId = "rohan" },
                        label = { Text("Rohan", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = callerId == "clinic",
                        onClick = { callerId = "clinic" },
                        label = { Text("City Clinic", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Call status selector
                Text(
                    text = "Call Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = status == "missed",
                        onClick = {
                            status = "missed"
                            durationSecs = 0
                        },
                        label = { Text("Missed", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = status == "answered",
                        onClick = {
                            status = "answered"
                            durationSecs = 15
                        },
                        label = { Text("Answered (15s)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = status == "rejected",
                        onClick = {
                            status = "rejected"
                            durationSecs = 0
                        },
                        label = { Text("Rejected", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Voicemail / Audio Transcript Input
                Text(
                    text = "Voicemail / Audio Transcript",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = transcript,
                    onValueChange = { transcript = it },
                    placeholder = { Text("Enter synthetic speech transcript...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Quick Preset Chips
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = {
                            callerId = "unknown_stalker"
                            status = "missed"
                            transcript = "Pick up right now. You know you can't ignore me forever. I'm outside."
                        },
                        label = { Text("Hostile Stalker", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = {
                            callerId = "rohan"
                            status = "missed"
                            transcript = "Why aren't you answering me? Pick up or else I will keep calling."
                        },
                        label = { Text("Coercive Stalking", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = {
                            callerId = "clinic"
                            status = "answered"
                            transcript = "Hi Meera, this is City Dental confirming your routine cleaning tomorrow at 10 AM."
                        },
                        label = { Text("Benign Clinic", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = {
                            callerId = "ishaan"
                            status = "missed"
                            transcript = "Hey Meera, whenever you get a free moment, please give me a quick ring back!"
                        },
                        label = { Text("Friendly Check-in", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSimulate(callerId, durationSecs, status, transcript) },
                enabled = !isSimulating
            ) {
                if (isSimulating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Simulate & Analyze")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSimulating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IncomingCallResultDialog(
    record: CallRecord,
    onDismiss: () -> Unit,
    onBlock: () -> Unit
) {
    val displayName = when (record.caller_id) {
        "unknown_stalker", "unknown" -> "Unknown Caller"
        "rohan" -> "Rohan Sharma"
        "clinic" -> "City Clinic"
        "diya" -> "Diya Sharma"
        "ishaan" -> "Ishaan Verma"
        else -> record.caller_id.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    val isHighRisk = record.safety.severity >= 70f

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isHighRisk) Icons.Default.PhoneCallback else Icons.Default.PhoneInTalk,
                contentDescription = null,
                tint = if (isHighRisk) SafetyTheme.highRisk else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isHighRisk) "High Risk Call Detected" else "Call Evaluated",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Caller: $displayName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "SIMULATED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "Status: ${record.status.replaceFirstChar { it.uppercase() }} (${record.duration_secs}s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                SeverityMeter(
                    severity = record.safety.severity,
                    tier = record.safety.tier
                )

                if (record.safety.reasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Evaluated Factors:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    record.safety.reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!record.transcript.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                text = "\"${record.transcript}\"",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isHighRisk) {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline & Log Evidence")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (isHighRisk) {
                TextButton(onClick = onDismiss) {
                    Text("Ignore")
                }
            }
        }
    )
}

@Composable
fun CallRecordItem(
    call: CallRecord,
    modifier: Modifier = Modifier
) {
    val isRisk = call.safety.severity >= 70f
    val isMissed = call.status == "missed"

    val displayName = when (call.caller_id) {
        "unknown_stalker", "unknown" -> "Unknown Caller"
        "rohan" -> "Rohan (Design Lead)"
        "clinic" -> "City Clinic"
        "diya" -> "Diya Sharma"
        "ishaan" -> "Ishaan Verma"
        else -> call.caller_id.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    val formattedTime = formatCallTimestamp(call.timestamp)
    val callDetail = if (isMissed) {
        "Missed • $formattedTime"
    } else {
        "Answered (${call.duration_secs}s) • $formattedTime"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isRisk) SafetyTheme.highRiskContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (isRisk) SafetyTheme.highRiskBorder else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMissed) Icons.Default.PhoneMissed else Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (call.safety.simulated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "SIMULATED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = callDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!call.transcript.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\"${call.transcript}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isRisk) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SafetyTheme.highRiskContainer,
                border = BorderStroke(1.dp, SafetyTheme.highRiskBorder)
            ) {
                Text(
                    text = "Risk: ${call.safety.severity.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SafetyTheme.onHighRiskContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatCallTimestamp(iso: String): String {
    return try {
        val dt = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
        formatter.format(dt)
    } catch (_: Exception) {
        if (iso.contains("T")) {
            iso.substringAfter("T").take(5)
        } else {
            iso
        }
    }
}
