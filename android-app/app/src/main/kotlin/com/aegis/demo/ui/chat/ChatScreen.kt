package com.aegis.demo.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.ChatParticipant
import com.aegis.demo.data.model.ChatThread
import com.aegis.demo.data.model.SafeMessage
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.components.ComposerFloatingToolbar
import com.aegis.demo.ui.components.SafeMessageBubble
import com.aegis.demo.ui.components.SafetyReasonsBottomSheet
import com.aegis.demo.ui.theme.SafetyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenSafetyCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { ServiceLocator.getRepository() }
    val scope = rememberCoroutineScope()

    val chats by repository.getChats().collectAsState(initial = emptyList())
    val messages by remember(repository, chatId) { repository.getMessages(chatId) }.collectAsState(initial = emptyList())
    val activePersona by repository.getActivePersona().collectAsState()

    val currentChat = chats.find { it.id == chatId } ?: ChatThread(
        id = chatId,
        participant = ChatParticipant(
            id = chatId.removePrefix("chat_"),
            name = chatId.removePrefix("chat_").replaceFirstChar { it.uppercase() },
            avatar_url = ""
        )
    )

    var inspectedMessage by remember { mutableStateOf<SafeMessage?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(messages.size, isImeVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Only show high-risk alert banner when there is a genuinely high-severity risk threshold breach (>= 70)
    val hasHighRiskBreach = remember(currentChat, messages) {
        currentChat.max_tier in listOf("high", "urgent") && messages.any { it.safety.severity >= 70f }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentChat.participant.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentChat.participant.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (currentChat.participant.is_blocked) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = "Blocked contact",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (currentChat.participant.is_blocked) "Blocked • Shielding active" else "Active now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentChat.participant.is_blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                    },
                    actions = {
                        if (hasHighRiskBreach) {
                            IconButton(onClick = onOpenSafetyCenter) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Safety Center",
                                    tint = SafetyTheme.highRisk
                                )
                            }
                        } else {
                            IconButton(onClick = onOpenSafetyCenter) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = "Safety Center",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (currentChat.participant.is_blocked) {
                                DropdownMenuItem(
                                    text = { Text("Unblock Contact") },
                                    onClick = {
                                        scope.launch { repository.unblockUser(currentChat.participant.id) }
                                        showMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Block Contact") },
                                    onClick = {
                                        scope.launch { repository.blockUser(currentChat.participant.id) }
                                        showMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Mute Notifications") },
                                onClick = {
                                    scope.launch { repository.muteUser(currentChat.participant.id) }
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Conversation") },
                                onClick = {
                                    scope.launch { repository.submitReport(null, currentChat.participant.id, "harassment") }
                                    showMenu = false
                                }
                            )
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        },
        bottomBar = {
            ComposerFloatingToolbar(
                onSendMessage = { text ->
                    scope.launch {
                        repository.sendMessage(chatId, text)
                    }
                },
                onSendImage = { bytes, filename ->
                    scope.launch {
                        repository.sendImage(chatId, bytes, filename)
                    }
                },
                onSendVoice = { bytes, filename ->
                    scope.launch {
                        repository.sendVoiceNote(chatId, bytes, filename)
                    }
                },
                onCheckNudge = { draft ->
                    repository.checkNudge(draft, chatId)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Refined M3 Expressive Risk Status Banner (Clean Card & LinearWavyProgressIndicator)
            if (hasHighRiskBreach) {
                val maxSeverity = messages.maxOfOrNull { it.safety.severity } ?: 88f
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTheme.highRiskBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SafetyTheme.highRiskContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = SafetyTheme.highRisk,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "High Harassment Risk Detected",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Threat severity: ${maxSeverity.toInt()}/100 • Automated shielding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenSafetyCenter,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTheme.highRisk.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyTheme.highRisk
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearWavyProgressIndicator(
                            progress = { (maxSeverity / 100f).coerceIn(0.1f, 1f) },
                            color = SafetyTheme.highRisk,
                            trackColor = SafetyTheme.highRiskContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Discreet simulated date divider
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isSelf = msg.sender_id == (activePersona?.id ?: "meera")
                    SafeMessageBubble(
                        message = msg,
                        isSelf = isSelf,
                        onInspectSafety = { inspectedMessage = it }
                    )
                }
            }
        }

        // Bottom sheet for SHAP safety explanations
        inspectedMessage?.let { msg ->
            SafetyReasonsBottomSheet(
                message = msg,
                onDismiss = { inspectedMessage = null },
                onBlockUser = { userId ->
                    scope.launch { repository.blockUser(userId) }
                },
                onReport = { msgId ->
                    scope.launch { repository.submitReport(msgId, msg.sender_id, "harassment") }
                }
            )
        }
    }
}
