package com.aegis.demo.ui.director

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.BaselineComparison
import com.aegis.demo.data.model.ScenarioProgress
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.theme.SafetyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoDirectorScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember { ServiceLocator.getRepository() }
    val scope = rememberCoroutineScope()

    var selectedScenario by remember { mutableIntStateOf(1) }
    var speed by remember { mutableFloatStateOf(60f) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf<ScenarioProgress?>(null) }
    var baselineComparison by remember { mutableStateOf<BaselineComparison?>(null) }

    LaunchedEffect(Unit) {
        baselineComparison = repository.getBaselineComparison("chat_rohan")
    }

    val scenarioList = remember {
        listOf(
            ScenarioItem(1, "Polite Stalking", "80 polite messages over 3 hours with 0 replies. Demonstrates high frequency without toxic keywords.", "rohan", "high"),
            ScenarioItem(2, "Coordinated Dogpiling", "Targeted harassment spike with 24 comments from throwaway accounts. Graph modularity intervention.", "cluster", "high"),
            ScenarioItem(3, "Cyberflashing Image", "Unsolicited explicit media from a non-contact. Blur media with victim reveal discretion.", "stranger_77", "high"),
            ScenarioItem(4, "OCR & Meme Toxicity", "Harassing text embedded in a screenshot meme. OCR signal combined with behavioral context.", "rohan", "high"),
            ScenarioItem(5, "Contact After Block", "Stalker creates throwaway persona after being blocked. Post-block persistent contact detection.", "rohan_alt", "urgent"),
            ScenarioItem(6, "Overnight Voice Calls", "40 calls between 2 AM – 4 AM (92% unanswered). Synthetic audio transcript & quiet hour throttle.", "unknown", "high"),
            ScenarioItem(7, "Pre-Send Nudge", "Real-time draft check nudges user before sending a hurtful message, de-escalating harm.", "meera", "review"),
            ScenarioItem(8, "Benign Disagreement", "Heated argument between mutual contacts. Context engine correctly classifies as low risk (no false positive).", "diya", "low"),
            ScenarioItem(9, "Emergency SOS", "Explicit threat triggering immediate one-tap police contact and evidence preservation.", "stalker", "urgent")
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Demo Director",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Interactive Scenario Controller",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Scenario Picker Carousel
            item {
                Text(
                    text = "Select Behavioral Scenario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(scenarioList) { sc ->
                        val isSelected = sc.id == selectedScenario
                        Card(
                            onClick = { selectedScenario = sc.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scenario ${sc.id}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (sc.expectTier == "low") SafetyTheme.lowRiskContainer else SafetyTheme.highRiskContainer,
                                        border = BorderStroke(0.5.dp, if (sc.expectTier == "low") SafetyTheme.lowRiskBorder else SafetyTheme.highRiskBorder),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = sc.expectTier.uppercase(),
                                            color = if (sc.expectTier == "low") SafetyTheme.lowRisk else SafetyTheme.highRisk,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sc.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sc.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }

            // Playback Speed Slider & Controls
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulation Speed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "${speed.toInt()}x Multiplier",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 1f..60f,
                            steps = 4
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isPlaying = true
                                    scope.launch {
                                        repository.playScenario(selectedScenario, speed.toInt()).collect { progress ->
                                            currentProgress = progress
                                            if (progress.step_index >= progress.total_steps) {
                                                isPlaying = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "Playing..." else "Run Scenario $selectedScenario")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.resetDemo()
                                        currentProgress = null
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // Step Progress Banner
            currentProgress?.let { prog ->
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { prog.step_index.toFloat() / prog.total_steps.toFloat() },
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Step ${prog.step_index} of ${prog.total_steps} (${prog.actor})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = prog.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // C1 Baseline Keyword Comparison Panel
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Engine vs Legacy Filter (Comparator C1)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Side-by-side evaluation of subtle harassment patterns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Legacy Keyword Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "KEYWORD FILTER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "0 Flags",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Missed polite harassment. No bad words detected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Aegis Engine Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafetyTheme.highRiskContainer,
                                border = BorderStroke(1.dp, SafetyTheme.highRiskBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AEGIS AI ENGINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyTheme.highRisk
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "88/100",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyTheme.highRisk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Escalated to HIGH. Velocity & 0-reply pattern flagged.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SafetyTheme.onHighRiskContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class ScenarioItem(
    val id: Int,
    val title: String,
    val desc: String,
    val actor: String,
    val expectTier: String
)
