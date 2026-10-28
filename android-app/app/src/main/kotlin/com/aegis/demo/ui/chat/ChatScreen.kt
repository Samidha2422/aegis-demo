package com.aegis.demo.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.demo.data.model.ChatThread
import com.aegis.demo.data.model.SafeMessage
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.components.ComposerFloatingToolbar
import com.aegis.demo.ui.components.SafeMessageBubble
import com.aegis.demo.ui.components.SafetyReasonsBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        participant = com.aegis.demo.data.model.ChatParticipant(
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentChat.participant.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentChat.participant.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (currentChat.participant.is_blocked) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "BLOCKED",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety Center",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (currentChat.participant.is_blocked) {
                            DropdownMenuItem(
                                text = { Text("Unblock User") },
                                onClick = {
                                    scope.launch { repository.unblockUser(currentChat.participant.id) }
                                    showMenu = false
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Block User") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Honesty & simulation disclaimer header
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Aegis Demo Mode: Behavioral analysis active. Simulated events are labeled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
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
}
