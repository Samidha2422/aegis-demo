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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentChat.participant.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentChat.participant.name,
                                    style = MaterialTheme.typography.titleMedium,
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
                                text = if (currentChat.participant.is_contact) "Contact" else "Non-contact (SIMULATED)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSafetyCenter) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Safety Center",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                            text = { Text("Report for Harassment") },
                            onClick = {
                                scope.launch { repository.submitReport(null, currentChat.participant.id, "harassment") }
                                showMenu = false
                            }
                        )
                    }
                }
            )
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
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned Risk Status Banner if elevated
            if (currentChat.max_tier in listOf("high", "urgent", "review")) {
                val isUrgent = currentChat.max_tier in listOf("high", "urgent")
                Surface(
                    color = if (isUrgent) SafetyTheme.highRiskContainer else SafetyTheme.reviewRiskContainer,
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
                                    imageVector = if (isUrgent) Icons.Default.Shield else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isUrgent) SafetyTheme.highRisk else SafetyTheme.reviewRisk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUrgent) "ELEVATED HARASSMENT RISK (88/100)" else "UNRECIPROCATED BURST DETECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUrgent) SafetyTheme.onHighRiskContainer else SafetyTheme.onReviewRiskContainer
                                )
                            }
                            TextButton(
                                onClick = onOpenSafetyCenter,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearWavyProgressIndicator(
                            progress = { if (isUrgent) 0.88f else 0.52f },
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
                // Simulation tag header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "SIMULATED SCENARIO CONVERSATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
