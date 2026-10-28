package com.aegis.demo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.EngineStatus
import com.aegis.demo.di.ServiceLocator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val repository by ServiceLocator.repository.collectAsState()
    val currentUrl by ServiceLocator.baseUrl.collectAsState()
    val replay by ServiceLocator.isReplayMode.collectAsState()
    val scope = rememberCoroutineScope()
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var urlError by remember { mutableStateOf(false) }
    var status by remember(repository) { mutableStateOf<EngineStatus?>(null) }
    var error by remember(repository) { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var resetStatus by remember { mutableStateOf<String?>(null) }

    suspend fun checkConnection() {
        checking = true
        error = null
        status = null
        try {
            status = repository.getEngineStatus()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = "Engine status unavailable. Check the chat server's engine URL and API key."
        } finally { checking = false }
    }
    LaunchedEffect(repository) { checkConnection() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            })
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Offline replay (SIMULATED)", modifier = Modifier.weight(1f))
                Switch(checked = replay, onCheckedChange = ServiceLocator::setReplayMode)
            }
            OutlinedTextField(
                value = inputUrl, onValueChange = { inputUrl = it; urlError = false },
                label = { Text("Chat server URL") }, singleLine = true,
                isError = urlError, modifier = Modifier.fillMaxWidth(),
                supportingText = { if (urlError) Text("Enter an HTTP or HTTPS URL without credentials or query parameters.") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { urlError = !ServiceLocator.setBaseUrl(inputUrl) }) { Text("Save") }
                OutlinedButton(enabled = !checking, onClick = { scope.launch { checkConnection() } }) { Text(if (checking) "Checking…" else "Check connection") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            status?.let { engine ->
                Text(if (replay) "SIMULATED: bundled fixtures" else if (engine.health.mock) "SIMULATED: engine mock mode" else "Engine connected", style = MaterialTheme.typography.titleMedium)
                engine.health.degraded.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                engine.models.forEach { model ->
                    ListItem(
                        headlineContent = { Text(model.name) },
                        supportingContent = { Text(buildString {
                            append(model.backend)
                            model.version?.let { append(" · ").append(it) }
                            if (model.degraded.isNotEmpty()) append("\n").append(model.degraded.joinToString("\n"))
                        }) }
                    )
                }
                if (!replay && engine.models.isEmpty()) Text("No model backends reported")
            }
            HorizontalDivider()
            OutlinedButton(onClick = {
                scope.launch {
                    repository.resetDemo()
                    resetStatus = "Chat state reset requested. Engine history is managed separately."
                }
            }) { Text("Reset chat state") }
            resetStatus?.let { Text(it) }
            Text("Replay uses fictional personas and synthetic assets. Live requests are analyzed by the host engine.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }
    }
}
