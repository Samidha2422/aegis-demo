package com.aegis.demo

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey

@Serializable
data object ChatsListKey : AppNavKey

@Serializable
data class ChatDetailKey(val chatId: String) : AppNavKey

@Serializable
data object FeedKey : AppNavKey

@Serializable
data object CallsKey : AppNavKey

@Serializable
data object SafetyCenterKey : AppNavKey

@Serializable
data object DemoDirectorKey : AppNavKey

@Serializable
data object PersonaPickerKey : AppNavKey

@Serializable
data object SettingsKey : AppNavKey
