package com.aegis.demo.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aegis.demo.data.model.SafeMessage
import com.aegis.demo.di.ServiceLocator
import com.aegis.demo.ui.theme.SafetyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SafeMessageBubble(
    message: SafeMessage,
    isSelf: Boolean,
    onInspectSafety: (SafeMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val safety = message.safety
    val state = safety.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val baseUrl by ServiceLocator.baseUrl.collectAsState()

    val mediaUrl = remember(message.media_url, baseUrl) {
        message.media_url?.let { path ->
            when {
                path.startsWith("http://") || path.startsWith("https://") -> path
                path.startsWith("assets/") -> "file:///android_asset/$path"
                path.contains("/media/") -> baseUrl.trimEnd('/') + "/" + path.substringAfter("/media/").let { "media/$it" }
                else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
            }
        }
    }

    val isImageSensitive = remember(message.id, safety.state, safety.recommended_action, safety.signals) {
        val imgSignal = safety.signals["image"] ?: 0f
        message.kind == "image" && (
            safety.state == "Blurred" ||
            safety.recommended_action == "blur_media" ||
            imgSignal >= 0.5f ||
            safety.reasons.any {
                it.contains("explicit", ignoreCase = true) ||
                it.contains("nsfw", ignoreCase = true) ||
                it.contains("synthetic intimate", ignoreCase = true)
            }
        )
    }

    var isBlurred by remember(isImageSensitive) { mutableStateOf(isImageSensitive) }
    var isHeldExpanded by remember { mutableStateOf(false) }
    var audioRevealed by remember(message.id) { mutableStateOf(false) }

    var isVoicePlaying by remember(message.id) { mutableStateOf(false) }
    var isVoicePreparing by remember(message.id) { mutableStateOf(false) }
    var playbackError by remember(message.id) { mutableStateOf(false) }
    var player by remember(message.id) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(message.id) {
        onDispose {
            player?.release()
            player = null
        }
    }

    fun playVoiceAudio() {
        if (player != null) {
            if (isVoicePlaying) {
                player?.pause()
                isVoicePlaying = false
            } else {
                player?.start()
                isVoicePlaying = true
            }
            return
        }

        isVoicePreparing = true
        playbackError = false

        scope.launch(Dispatchers.IO) {
            try {
                if (message.media_url?.startsWith("assets/") == true) {
                    val media = MediaPlayer()
                    context.assets.openFd(message.media_url).use { asset ->
                        media.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                    }
                    withContext(Dispatchers.Main) {
                        player = media
                        media.setOnPreparedListener {
                            it.start()
                            isVoicePreparing = false
                            isVoicePlaying = true
                        }
                        media.setOnCompletionListener {
                            isVoicePlaying = false
                        }
                        media.setOnErrorListener { failed, _, _ ->
                            failed.release()
                            player = null
                            playbackError = true
                            isVoicePreparing = false
                            isVoicePlaying = false
                            true
                        }
                        media.prepareAsync()
                    }
                } else if (mediaUrl != null) {
                    val sanitizedId = message.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val cacheFile = File(context.cacheDir, "voice_$sanitizedId.wav")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        val req = okhttp3.Request.Builder().url(mediaUrl).build()
                        val res = ServiceLocator.getOkHttpClient().newCall(req).execute()
                        if (res.isSuccessful) {
                            res.body?.byteStream()?.use { input ->
                                cacheFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }

                    if (cacheFile.exists() && cacheFile.length() > 0) {
                        withContext(Dispatchers.Main) {
                            val media = MediaPlayer()
                            player = media
                            media.setDataSource(cacheFile.absolutePath)
                            media.setOnPreparedListener {
                                it.start()
                                isVoicePreparing = false
                                isVoicePlaying = true
                            }
                            media.setOnCompletionListener {
                                isVoicePlaying = false
                            }
                            media.setOnErrorListener { failed, _, _ ->
                                failed.release()
                                player = null
                                playbackError = true
                                isVoicePreparing = false
                                isVoicePlaying = false
                                true
                            }
                            media.prepareAsync()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isVoicePreparing = false
                            playbackError = true
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isVoicePreparing = false
                        playbackError = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    player?.release()
                    player = null
                    isVoicePreparing = false
                    isVoicePlaying = false
                    playbackError = true
                }
            }
        }
    }

    val bubbleAlignment = if (isSelf) Alignment.End else Alignment.Start
    val bubbleShape = if (isSelf) {
        RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp)
    }

    val bubbleColor = when {
        isSelf -> MaterialTheme.colorScheme.primary
        state == "Held" || state == "Hidden" -> MaterialTheme.colorScheme.surfaceVariant
        state == "Warned" -> SafetyTheme.reviewRiskContainer
        state == "Escalated" -> SafetyTheme.highRiskContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val bubbleBorder = when {
        isSelf -> null
        state == "Warned" -> BorderStroke(1.dp, SafetyTheme.reviewRiskBorder)
        state == "Escalated" -> BorderStroke(1.dp, SafetyTheme.highRiskBorder)
        state == "Held" || state == "Hidden" -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val contentColor = when {
        isSelf -> MaterialTheme.colorScheme.onPrimary
        state == "Warned" -> SafetyTheme.onReviewRiskContainer
        state == "Escalated" -> SafetyTheme.onHighRiskContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val timestampColor = when {
        isSelf -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = bubbleBorder,
            shadowElevation = if (isSelf) 0.5.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clickable {
                    if (safety.severity > 0 || safety.reasons.isNotEmpty()) {
                        onInspectSafety(message)
                    }
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Safety badge header if flagged or held
                if (state != "Delivered") {
                    SafetyStateHeader(
                        state = state,
                        severity = safety.severity,
                        onInspect = { onInspectSafety(message) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (state) {
                    "Scanning" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Evaluating behavioral signals...",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = contentColor
                            )
                        }
                    }

                    "Held", "Hidden" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Held",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Message Quarantined",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Automated quarantine based on behavioral risk pattern.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHeldExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                if (message.kind == "image") {
                                    BlurOverlay(
                                        isBlurred = isBlurred,
                                        onToggleBlur = { isBlurred = !isBlurred },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (mediaUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(mediaUrl)
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.DISABLED)
                                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                                        .build(),
                                                    imageLoader = ServiceLocator.getImageLoader(context),
                                                    contentDescription = if (safety.simulated) "Simulated image" else "Image attachment",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Image,
                                                    contentDescription = "Image not retained",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (message.kind == "audio") {
                                    AudioMessageContent(
                                        message = message,
                                        isSelf = isSelf,
                                        isVoicePlaying = isVoicePlaying,
                                        isVoicePreparing = isVoicePreparing,
                                        playbackError = playbackError,
                                        onPlayToggle = { playVoiceAudio() },
                                        contentColor = contentColor,
                                        timestampColor = timestampColor
                                    )
                                } else {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isHeldExpanded = !isHeldExpanded },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isHeldExpanded) "Hide" else "Reveal (Discretion)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    "TranscriptOnly" -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneCallback,
                                    contentDescription = "Protected Voice",
                                    tint = if (isSelf) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (safety.simulated) "SYNTHETIC AUDIO (PROTECTED)" else "AUDIO NOTE (PROTECTED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelf) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }

                            val transcriptText = message.transcript ?: message.text
                            if (transcriptText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelf) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "TRANSCRIPT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelf) Color.White else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"$transcriptText\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor
                                        )
                                    }
                                }
                            }

                            if (!audioRevealed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { audioRevealed = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Listen Anyway (Discretionary Reveal)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                AudioMessageContent(
                                    message = message,
                                    isSelf = isSelf,
                                    isVoicePlaying = isVoicePlaying,
                                    isVoicePreparing = isVoicePreparing,
                                    playbackError = playbackError,
                                    onPlayToggle = { playVoiceAudio() },
                                    contentColor = contentColor,
                                    timestampColor = timestampColor
                                )
                            }
                        }
                    }

                    else -> {
                        if (message.kind == "image") {
                            BlurOverlay(
                                isBlurred = isBlurred,
                                onToggleBlur = { isBlurred = !isBlurred },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (mediaUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(mediaUrl)
                                                .crossfade(true)
                                                .diskCachePolicy(CachePolicy.DISABLED)
                                                .memoryCachePolicy(CachePolicy.DISABLED)
                                                .build(),
                                            imageLoader = ServiceLocator.getImageLoader(context),
                                            contentDescription = if (safety.simulated) "Simulated image" else "Image attachment",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = "Image not retained",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(8.dp)
                                    ) {
                                        val labelText = if (message.media_url == "assets/proxy_nsfw.png" || message.text == "DEMO PROXY IMAGE") "DEMO PROXY IMAGE" else if (isImageSensitive) "SENSITIVE MEDIA" else ""
                                        if (labelText.isNotEmpty()) {
                                            Text(
                                                text = labelText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Risk Reasons Breakdown for Image
                            if (safety.reasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (safety.severity >= 70f) SafetyTheme.highRiskContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (safety.severity >= 70f) SafetyTheme.highRiskBorder else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = if (safety.severity >= 70f) SafetyTheme.highRisk else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "RISK REASONS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (safety.severity >= 70f) SafetyTheme.highRisk else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        safety.reasons.forEach { r ->
                                            Text(
                                                text = "• $r",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (safety.severity >= 70f) SafetyTheme.onHighRiskContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Vision Backend & Deepfake / NSFW status tag
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SafetyTheme.reviewRiskContainer,
                                    border = BorderStroke(0.5.dp, SafetyTheme.reviewRiskBorder)
                                ) {
                                    Text(
                                        text = safety.modules["image"]?.let { "${it.status} · ${it.backend}" } ?: if (safety.simulated) "SIMULATED" else "ONNX Vision",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyTheme.reviewRisk,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (message.text.isNotEmpty() && message.text != "DEMO PROXY IMAGE" && message.text != "Image" && message.text != "demo_proxy_nsfw.jpg") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        } else if (message.kind == "audio") {
                            AudioMessageContent(
                                message = message,
                                isSelf = isSelf,
                                isVoicePlaying = isVoicePlaying,
                                isVoicePreparing = isVoicePreparing,
                                playbackError = playbackError,
                                onPlayToggle = { playVoiceAudio() },
                                contentColor = contentColor,
                                timestampColor = timestampColor
                            )
                        } else {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )
                        }
                    }
                }

                // C1 Baseline Keyword Comparison indicator
                if (safety.severity >= 60f && !isSelf) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SafetyTheme.keywordMissedContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Keyword Baseline: 0 Flags",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafetyTheme.keywordMissed
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Aegis AI: ${safety.severity.toInt()}/100",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafetyTheme.engineCaught
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAssetSimulated = message.text == "DEMO PROXY IMAGE" ||
                        message.media_url?.contains("synthetic") == true ||
                        message.media_url?.contains("proxy") == true ||
                        safety.tags.contains("SYNTHETIC AUDIO") ||
                        safety.tags.contains("DEMO PROXY IMAGE")
                    if (isAssetSimulated) {
                        Surface(
                            color = if (isSelf) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SIMULATED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelf) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = formatMessageTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = timestampColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioMessageContent(
    message: SafeMessage,
    isSelf: Boolean,
    isVoicePlaying: Boolean,
    isVoicePreparing: Boolean,
    playbackError: Boolean,
    onPlayToggle: () -> Unit,
    contentColor: Color,
    timestampColor: Color
) {
    Column {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = if (message.safety.simulated) "SYNTHETIC AUDIO" else "AUDIO NOTE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelf) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Voice Note Player Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                enabled = !isVoicePreparing,
                onClick = onPlayToggle,
                modifier = Modifier.size(36.dp)
            ) {
                if (isVoicePreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isVoicePlaying) "Pause" else "Play",
                        tint = contentColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    playbackError -> "Playback unavailable"
                    isVoicePreparing -> "Loading audio..."
                    isVoicePlaying -> "Playing audio..."
                    else -> "Voice note"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }

        // Display transcription if available
        val transcriptText = message.transcript ?: message.text
        if (transcriptText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelf) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "TRANSCRIPT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelf) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"$transcriptText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyStateHeader(
    state: String,
    severity: Float,
    onInspect: () -> Unit
) {
    val (icon, label, badgeBg, badgeBorder, badgeFg) = when (state) {
        "Warned" -> Quintuple(Icons.Default.Warning, "FLAGGED BY AEGIS", SafetyTheme.reviewRiskContainer, SafetyTheme.reviewRiskBorder, SafetyTheme.onReviewRiskContainer)
        "Blurred" -> Quintuple(Icons.Default.VisibilityOff, "SENSITIVE MEDIA BLURRED", SafetyTheme.highRiskContainer, SafetyTheme.highRiskBorder, SafetyTheme.onHighRiskContainer)
        "Held" -> Quintuple(Icons.Default.Shield, "QUARANTINED", MaterialTheme.colorScheme.errorContainer, SafetyTheme.highRiskBorder, MaterialTheme.colorScheme.onErrorContainer)
        "Escalated" -> Quintuple(Icons.Default.Security, "CRITICAL RISK", SafetyTheme.criticalRiskContainer, SafetyTheme.criticalRiskBorder, SafetyTheme.onCriticalRiskContainer)
        "EngineUnavailable" -> Quintuple(Icons.Default.CloudOff, "RULE ENGINE FALLBACK", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        "TranscriptOnly" -> Quintuple(Icons.Default.Shield, "TRANSCRIPT PROTECTED", SafetyTheme.highRiskContainer, SafetyTheme.highRiskBorder, SafetyTheme.onHighRiskContainer)
        else -> Quintuple(Icons.Default.Info, state.uppercase(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSecondaryContainer)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBg,
        border = BorderStroke(1.dp, badgeBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = badgeFg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (severity > 0) {
                Text(
                    text = "${severity.toInt()}/100",
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

private fun formatMessageTimestamp(raw: String): String {
    return try {
        val dt = java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
        formatter.format(dt)
    } catch (_: Exception) {
        if (raw.contains("T")) {
            raw.substringAfter("T").take(5)
        } else {
            raw
        }
    }
}
