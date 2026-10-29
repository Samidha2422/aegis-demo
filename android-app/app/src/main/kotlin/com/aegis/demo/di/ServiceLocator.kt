package com.aegis.demo.di

import android.content.Context
import com.aegis.demo.data.api.ChatApiService
import com.aegis.demo.data.repository.ChatRepository
import com.aegis.demo.data.repository.FixtureRepository
import com.aegis.demo.data.repository.NetworkChatRepository
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private fun isEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        val hardware = android.os.Build.HARDWARE
        return (fingerprint.startsWith("generic")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk_gphone")
                || product.contains("vbox86p")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product)
    }

    private fun defaultUrl(): String = if (isEmulator()) "http://10.0.2.2:8081/" else "http://127.0.0.1:8081/"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _isReplayMode = MutableStateFlow(false)
    val isReplayMode: StateFlow<Boolean> = _isReplayMode.asStateFlow()
    private val _baseUrl = MutableStateFlow(defaultUrl())
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()
    private var preferences: android.content.SharedPreferences? = null
    private val fixtureRepository = FixtureRepository()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Message bodies and drafts must never be written to Logcat.
        .build()
    private var networkRepository = createRepository(_baseUrl.value)
    private val _repository = MutableStateFlow<ChatRepository>(networkRepository)
    val repository: StateFlow<ChatRepository> = _repository.asStateFlow()

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences("connection", Context.MODE_PRIVATE)
        val saved = preferences?.getString("chat_url", null)
        val initialUrl = if (saved == null || (!isEmulator() && saved.contains("10.0.2.2")) || saved.contains(":8080")) {
            defaultUrl()
        } else {
            saved
        }
        setBaseUrl(initialUrl)
        setReplayMode(preferences?.getBoolean("replay", false) ?: false)
    }

    fun setReplayMode(enabled: Boolean) {
        _isReplayMode.value = enabled
        fixtureRepository.setReplayMode(enabled)
        _repository.value = if (enabled) fixtureRepository else networkRepository
        if (enabled) networkRepository.close() else networkRepository.start()
        preferences?.edit()?.putBoolean("replay", enabled)?.apply()
    }

    fun setBaseUrl(newUrl: String): Boolean {
        val parsed = newUrl.trim().toHttpUrlOrNull() ?: return false
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty() || parsed.query != null || parsed.fragment != null) return false
        val formatted = parsed.toString().let { if (it.endsWith("/")) it else "$it/" }
        val replacement = createRepository(formatted)
        networkRepository.close()
        networkRepository = replacement
        _baseUrl.value = formatted
        if (!_isReplayMode.value) {
            _repository.value = replacement
            replacement.start()
        }
        preferences?.edit()?.putString("chat_url", formatted)?.apply()
        return true
    }

    private fun createRepository(url: String): NetworkChatRepository {
        val api = Retrofit.Builder().baseUrl(url).client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ChatApiService::class.java)
        val wsUrl = url.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + "ws"
        return NetworkChatRepository(api, okHttpClient, wsUrl)
    }

    fun getRepository(): ChatRepository = _repository.value
}
