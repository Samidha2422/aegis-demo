package com.aegis.demo.di

import com.aegis.demo.data.api.ChatApiService
import com.aegis.demo.data.repository.ChatRepository
import com.aegis.demo.data.repository.FixtureRepository
import com.aegis.demo.data.repository.NetworkChatRepository
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _isReplayMode = MutableStateFlow(true)
    val isReplayMode: StateFlow<Boolean> = _isReplayMode.asStateFlow()

    private val _baseUrl = MutableStateFlow("http://localhost:8080/")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val fixtureRepository = FixtureRepository()
    private var networkRepository: NetworkChatRepository? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private fun createApiService(url: String): ChatApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ChatApiService::class.java)
    }

    init {
        rebuildNetworkRepository(_baseUrl.value)
    }

    fun setReplayMode(enabled: Boolean) {
        _isReplayMode.value = enabled
        fixtureRepository.setReplayMode(enabled)
        networkRepository?.setReplayMode(enabled)
    }

    fun setBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        _baseUrl.value = formatted
        rebuildNetworkRepository(formatted)
    }

    private fun rebuildNetworkRepository(url: String) {
        try {
            val api = createApiService(url)
            val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://") + "ws"
            networkRepository = NetworkChatRepository(api, okHttpClient, wsUrl)
        } catch (e: Exception) {
            // Keep existing or fallback
        }
    }

    fun getRepository(): ChatRepository {
        return if (_isReplayMode.value) {
            fixtureRepository
        } else {
            networkRepository ?: fixtureRepository
        }
    }
}
