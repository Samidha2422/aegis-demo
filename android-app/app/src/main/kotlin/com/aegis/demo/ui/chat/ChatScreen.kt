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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val messages by repository.getMessages(chatId).collectAsState(initial = emptyList())
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

    LaunchedEffect(messages.size) {
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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentChat.participant.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (currentChat.participant.is_blocked) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = "BLOCKED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (currentChat.participant.is_blocked) "Blocked" else "Active now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                onCheckNudge = { draft ->
                    repository.checkNudge(draft)
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
            // Pinned Risk Status Banner: STRICTLY ONLY when high risk breach (>= 70)
            if (hasHighRiskBreach) {
                Surface(
                    color = SafetyTheme.highRiskContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTheme.highRiskBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = SafetyTheme.highRisk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "HARASSMENT THRESHOLD BREACHED (88/100)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyTheme.onHighRiskContainer
                                )
                            }
                            TextButton(
                                onClick = onOpenSafetyCenter,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafetyTheme.highRisk
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearWavyProgressIndicator(
                            progress = { 0.88f },
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
                                text = "Today • Simulated Demo Thread",
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
