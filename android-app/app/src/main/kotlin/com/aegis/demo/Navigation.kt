package com.aegis.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.aegis.demo.ui.calls.CallsScreen
import com.aegis.demo.ui.chat.ChatScreen
import com.aegis.demo.ui.chats.ChatsListScreen
import com.aegis.demo.ui.director.DemoDirectorScreen
import com.aegis.demo.ui.feed.FeedScreen
import com.aegis.demo.ui.persona.PersonaPickerScreen
import com.aegis.demo.ui.safety.SafetyCenterScreen
import com.aegis.demo.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(ChatsListKey)
    val currentKey = backStack.lastOrNull() ?: ChatsListKey

    val showBottomBar = currentKey !is ChatDetailKey && currentKey !is PersonaPickerKey

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentKey is ChatsListKey,
                        onClick = {
                            if (currentKey !is ChatsListKey) {
                                backStack.clear()
                                backStack.add(ChatsListKey)
                            }
                        },
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                        label = { Text("Chats") }
                    )
                    NavigationBarItem(
                        selected = currentKey is FeedKey,
                        onClick = {
                            if (currentKey !is FeedKey) {
                                backStack.clear()
                                backStack.add(FeedKey)
                            }
                        },
                        icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                        label = { Text("Feed") }
                    )
                    NavigationBarItem(
                        selected = currentKey is CallsKey,
                        onClick = {
                            if (currentKey !is CallsKey) {
                                backStack.clear()
                                backStack.add(CallsKey)
                            }
                        },
                        icon = { Icon(Icons.Default.Phone, contentDescription = "Calls") },
                        label = { Text("Calls") }
                    )
                    NavigationBarItem(
                        selected = currentKey is SafetyCenterKey,
                        onClick = {
                            if (currentKey !is SafetyCenterKey) {
                                backStack.clear()
                                backStack.add(SafetyCenterKey)
                            }
                        },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Safety") },
                        label = { Text("Safety") }
                    )
                    NavigationBarItem(
                        selected = currentKey is DemoDirectorKey,
                        onClick = {
                            if (currentKey !is DemoDirectorKey) {
                                backStack.clear()
                                backStack.add(DemoDirectorKey)
                            }
                        },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Director") },
                        label = { Text("Director") }
                    )
                    NavigationBarItem(
                        selected = currentKey is SettingsKey,
                        onClick = {
                            if (currentKey !is SettingsKey) {
                                backStack.clear()
                                backStack.add(SettingsKey)
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryProvider = entryProvider {
                    entry<ChatsListKey> {
                        ChatsListScreen(
                            onSelectChat = { id -> backStack.add(ChatDetailKey(id)) },
                            onOpenPersonaPicker = { backStack.add(PersonaPickerKey) }
                        )
                    }
                    entry<ChatDetailKey> { key ->
                        ChatScreen(
                            chatId = key.chatId,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenSafetyCenter = { backStack.add(SafetyCenterKey) }
                        )
                    }
                    entry<FeedKey> {
                        FeedScreen()
                    }
                    entry<CallsKey> {
                        CallsScreen()
                    }
                    entry<SafetyCenterKey> {
                        SafetyCenterScreen()
                    }
                    entry<DemoDirectorKey> {
                        DemoDirectorScreen()
                    }
                    entry<PersonaPickerKey> {
                        PersonaPickerScreen(
                            onDismiss = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<SettingsKey> {
                        SettingsScreen()
                    }
                }
            )
        }
    }
}
