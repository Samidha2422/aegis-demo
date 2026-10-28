package com.aegis.demo.ui.director

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.demo.data.model.BaselineComparison
import com.aegis.demo.data.model.ScenarioProgress
import com.aegis.demo.di.ServiceLocator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoDirectorScreen(
    modifier: Modifier = Modifier
) {
    val repository = remember { ServiceLocator.getRepository() }
    val scope = rememberCoroutineScope()

    var selectedScenario by remember { mutableIntStateOf(1) }
    var speed by remember { mutableFloatStateOf(60f) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf<ScenarioProgress?>(null) }
    var baselineComparison by remember { mutableStateOf<BaselineComparison?>(null) }
    var showRawJson by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Demo Director & Scenarios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hacksploit 2026 PS5 Orchestrator",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Scenario Picker Carousel
            item {
                Text(
                    text = "Select Behavioral Scenario (1 – 9)",
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
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Scenario ${sc.id}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = if (sc.expectTier == "low") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = sc.expectTier.uppercase(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sc.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
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
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playback Compression Speed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${speed.toInt()}x (Demo Speed)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 1f..60f,
                            steps = 5
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
                                Text(if (isPlaying) "Playing..." else "Play Scenario $selectedScenario")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.resetDemo()
                                        currentProgress = null
                                    }
                                }
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
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
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
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Executing Step ${prog.step_index} of ${prog.total_steps} (${prog.actor})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = prog.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // C1 Baseline Keyword vs Aegis Risk Engine Comparison Card
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "C1: Keyword Filter vs Aegis Behavioral Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Keyword Baseline Side
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "KEYWORD FILTER (C1)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "0 / 80 Flagged",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "All messages were polite: \"Are you free?\", \"Hope you're well!\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(90.dp)
                                    .padding(horizontal = 12.dp)
                            )

                            // Aegis Engine Side
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AEGIS RISK ENGINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "TIER HIGH (Score: 84)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Flagged: 80 msgs in 3h, 0 replies, persistent after block.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Insight: Simple keyword filters fail completely on polite stalking. Aegis analyzes conversation frequency, reciprocity, and post-block persistence to stop harassment before physical harm occurs.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Raw Verdict JSON Viewer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inspect Engine JSON Verdict",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = showRawJson,
                        onCheckedChange = { showRawJson = it }
                    )
                }

                AnimatedVisibility(visible = showRawJson) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = """
                                {
                                  "target_user_id": "rohan",
                                  "harassment_score": 84.2,
                                  "risk_tier": "high",
                                  "shap_reasons": [
                                    "80 messages in 3 hours, 0 replies",
                                    "Contact persisted after user block"
                                  ],
                                  "signals": {
                                    "text_toxicity": 0.08,
                                    "frequency_zscore": 4.82,
                                    "post_block_recidivism": 1.0
                                  },
                                  "recommended_action": "throttle"
                                }
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
