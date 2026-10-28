package com.aegis.demo.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.Comment
import com.aegis.demo.data.model.Post
import com.aegis.demo.data.model.SafetyObject
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.theme.SafetyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenDirector: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember { ServiceLocator.getRepository() }
    val scope = rememberCoroutineScope()

    val posts by repository.getPosts().collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    var showQuarantinedComments by remember { mutableStateOf(false) }

    val post = posts.firstOrNull() ?: Post(
        id = "post_01",
        author_id = "meera",
        content = "Excited to announce our design team's research paper on protective digital spaces was accepted! #DesignLeadership",
        timestamp = "3 hours ago",
        comments_count = 24,
        is_protected = true
    )

    // Demo coordinated comment pile-on (Scenario 2)
    val demoComments = remember {
        listOf(
            Comment("c1", post.id, "throwaway_99", "Nobody asked for your opinion. Delete your account.", "2h ago",
                SafetyObject(78f, "high", listOf("Coordinated cluster account (Age < 2 days)"), state = "Held")),
            Comment("c2", post.id, "bot_alpha", "Overrated trash. Everyone in the office hates you.", "2h ago",
                SafetyObject(82f, "high", listOf("Targeted harassment pattern", "Graph cluster: Dogpiling mob"), state = "Held")),
            Comment("c3", post.id, "ishaan", "So proud of you Meera! Deserved recognition.", "1h ago",
                SafetyObject(0f, "low", emptyList(), state = "Delivered")),
            Comment("c4", post.id, "diya", "Congratulations!! Celebrating together this weekend!", "1h ago",
                SafetyObject(0f, "low", emptyList(), state = "Delivered"))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Public Feed",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Scenario 2: Dogpiling & Mob Defense",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDirector) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = "Demo Scenarios"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                text = "Add a comment...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                scope.launch {
                                    repository.addComment(post.id, commentText)
                                    commentText = ""
                                }
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Post Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "M",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Meera Patel",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = post.timestamp,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Aegis Mob Shield Intervention Banner (Scenario 2)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SafetyTheme.highRiskContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Mob Shield",
                                tint = SafetyTheme.highRisk
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "COORDINATED DOGPILING MITIGATED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyTheme.highRisk
                                )
                                Text(
                                    text = "24 comments from temporary cluster accounts quarantined.",
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
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "Modularity: 0.84",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            TextButton(
                                onClick = { showQuarantinedComments = !showQuarantinedComments },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showQuarantinedComments) "Hide Quarantined" else "View 24 Quarantined",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafetyTheme.highRisk
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Comments (${demoComments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(demoComments) { comment ->
                val isQuarantined = comment.safety.state == "Held"
                if (!isQuarantined || showQuarantinedComments) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isQuarantined) SafetyTheme.highRiskContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${comment.author_id}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (isQuarantined) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "QUARANTINED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = comment.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = comment.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
