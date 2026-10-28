package com.aegis.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
    val repository by com.aegis.demo.di.ServiceLocator.repository.collectAsState()
    val connectionError by repository.getConnectionError().collectAsState(initial = null)
    val backStack = rememberNavBackStack(ChatsListKey)
    val currentKey = backStack.lastOrNull() ?: ChatsListKey

    // Only show bottom navigation on the 4 primary root tabs
    val isPrimaryTab = currentKey is ChatsListKey || currentKey is FeedKey || currentKey is CallsKey || currentKey is SafetyCenterKey

    key(repository) {
    Scaffold(
        bottomBar = {
            if (isPrimaryTab) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        NavigationBarItem(
                            selected = currentKey is ChatsListKey,
                            onClick = {
                                if (currentKey !is ChatsListKey) {
                                    backStack.clear()
                                    backStack.add(ChatsListKey)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentKey is ChatsListKey) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Chats"
                                )
                            },
                            label = { Text("Chats", fontWeight = if (currentKey is ChatsListKey) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal) },
                            colors = navItemColors
                        )
                        NavigationBarItem(
                            selected = currentKey is FeedKey,
                            onClick = {
                                if (currentKey !is FeedKey) {
                                    backStack.clear()
                                    backStack.add(FeedKey)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentKey is FeedKey) Icons.Filled.DynamicFeed else Icons.Outlined.DynamicFeed,
                                    contentDescription = "Feed"
                                )
                            },
                            label = { Text("Feed", fontWeight = if (currentKey is FeedKey) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal) },
                            colors = navItemColors
                        )
                        NavigationBarItem(
                            selected = currentKey is CallsKey,
                            onClick = {
                                if (currentKey !is CallsKey) {
                                    backStack.clear()
                                    backStack.add(CallsKey)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentKey is CallsKey) Icons.Filled.PhoneCallback else Icons.Outlined.PhoneCallback,
                                    contentDescription = "Calls"
                                )
                            },
                            label = { Text("Calls", fontWeight = if (currentKey is CallsKey) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal) },
                            colors = navItemColors
                        )
                        NavigationBarItem(
                            selected = currentKey is SafetyCenterKey,
                            onClick = {
                                if (currentKey !is SafetyCenterKey) {
                                    backStack.clear()
                                    backStack.add(SafetyCenterKey)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentKey is SafetyCenterKey) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = "Safety"
                                )
                            },
                            label = { Text("Safety", fontWeight = if (currentKey is SafetyCenterKey) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal) },
                            colors = navItemColors
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            connectionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
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
                            onOpenPersonaPicker = { backStack.add(PersonaPickerKey) },
                            onOpenDirector = { backStack.add(DemoDirectorKey) },
                            onOpenSettings = { backStack.add(SettingsKey) }
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
                        FeedScreen(
                            onOpenDirector = { backStack.add(DemoDirectorKey) }
                        )
                    }
                    entry<CallsKey> {
                        CallsScreen(
                            onOpenSafetyCenter = { backStack.add(SafetyCenterKey) }
                        )
                    }
                    entry<SafetyCenterKey> {
                        SafetyCenterScreen()
                    }
                    entry<DemoDirectorKey> {
                        DemoDirectorScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<PersonaPickerKey> {
                        PersonaPickerScreen(
                            onDismiss = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<SettingsKey> {
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            )
        }
    }
}
}
