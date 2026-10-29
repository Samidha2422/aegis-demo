package com.aegis.demo.ui.components

import android.net.Uri
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aegis.demo.data.model.NudgeResult
import com.aegis.demo.ui.theme.SafetyTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComposerFloatingToolbar(
    onSendMessage: (String) -> Unit,
    onSendImage: (ByteArray, String) -> Unit = { _, _ -> },
    onSendVoice: (ByteArray, String) -> Unit = { _, _ -> },
    onCheckNudge: suspend (String) -> NudgeResult,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var nudgeResult by remember { mutableStateOf<NudgeResult?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    val stopRecording = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose { recordingJob?.cancel() }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recording) return
        attachmentError = null
        recordingSeconds = 0
        recording = true
        stopRecording.set(false)
        recordingJob = scope.launch {
            try {
                val wav = withContext(Dispatchers.IO) {
                    val sampleRate = 16_000
                    val maxBytes = sampleRate * 2 * 60
                    val minBuffer = AudioRecord.getMinBufferSize(
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                    )
                    check(minBuffer > 0) { "Microphone format unavailable." }
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, 4096)
                    )
                    try {
                        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable." }
                        recorder.startRecording()
                        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Could not start recording." }
                        val pcm = ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        var lastSecond = -1
                        while (!stopRecording.get() && pcm.size() < maxBytes) {
                            currentCoroutineContext().ensureActive()
                            val count = recorder.read(buffer, 0, minOf(buffer.size, maxBytes - pcm.size()), AudioRecord.READ_NON_BLOCKING)
                            check(count >= 0) { "Microphone recording failed." }
                            if (count > 0) pcm.write(buffer, 0, count) else delay(20)
                            val seconds = pcm.size() / (sampleRate * 2)
                            if (seconds != lastSecond) {
                                lastSecond = seconds
                                withContext(Dispatchers.Main) { recordingSeconds = seconds }
                            }
                        }
                        val samples = pcm.toByteArray()
                        check(samples.isNotEmpty()) { "Recording is empty." }
                        ByteBuffer.allocate(44 + samples.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(36 + samples.size)
                            put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); putInt(16)
                            putShort(1); putShort(1); putInt(sampleRate); putInt(sampleRate * 2)
                            putShort(2); putShort(16); put("data".toByteArray(Charsets.US_ASCII))
                            putInt(samples.size); put(samples)
                        }.array()
                    } finally {
                        try { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
                        finally { recorder.release() }
                    }
                }
                onSendVoice(wav, "voice_${System.currentTimeMillis()}.wav")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                attachmentError = error.message ?: "Unable to record audio."
            } finally {
                recording = false
                recordingJob = null
            }
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else attachmentError = "Microphone permission is required to record."
    }

    fun loadAttachment(
        maxBytes: Int,
        openStream: () -> InputStream?,
        onLoaded: (ByteArray) -> Unit
    ) {
        attachmentError = null
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val stream = openStream() ?: throw java.io.IOException("Cannot open attachment")
                    stream.use {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = it.read(buffer)
                            if (count == -1) break
                            require(count <= maxBytes - output.size()) { "Attachment is too large." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                }
                require(bytes.isNotEmpty()) { "Attachment is empty." }
                onLoaded(bytes)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                attachmentError = if (error is IllegalArgumentException) error.message else "Unable to read attachment."
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                attachmentError = null
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw java.io.IOException("Cannot open image")
                        val maxDim = 1280
                        val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size, boundsOpts)
                        var sampleSize = 1
                        while (boundsOpts.outWidth / sampleSize > maxDim * 1.5 || boundsOpts.outHeight / sampleSize > maxDim * 1.5) {
                            sampleSize *= 2
                        }
                        val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size, decodeOpts)
                            ?: throw IllegalArgumentException("Unsupported image format")
                        val width = bitmap.width
                        val height = bitmap.height
                        val finalBitmap = if (width > maxDim || height > maxDim) {
                            val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                            android.graphics.Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
                        } else {
                            bitmap
                        }
                        val out = ByteArrayOutputStream()
                        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                        out.toByteArray()
                    }
                    require(bytes.isNotEmpty()) { "Image is empty" }
                    onSendImage(bytes, "photo_${System.currentTimeMillis()}.jpg")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    attachmentError = error.message ?: "Unable to read image."
                }
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            loadAttachment(25 * 1024 * 1024, { context.contentResolver.openInputStream(uri) }) { bytes ->
                require(bytes.size >= 12 &&
                    bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray(Charsets.US_ASCII)) &&
                    bytes.copyOfRange(8, 12).contentEquals("WAVE".toByteArray(Charsets.US_ASCII))
                ) { "Choose a WAV audio file." }
                onSendVoice(bytes, "upload_${System.currentTimeMillis()}.wav")
            }
        }
    }

    fun handleTextChange(newText: String) {
        text = newText
        debounceJob?.cancel()
        if (newText.length >= 4) {
            debounceJob = scope.launch {
                delay(350)
                val res = onCheckNudge(newText)
                if (res.nudge_text != null || res.severity >= 50f) {
                    nudgeResult = res
                } else {
                    nudgeResult = null
                }
            }
        } else {
            nudgeResult = null
        }
    }

    fun submit() {
        if (text.isNotBlank()) {
            onSendMessage(text)
            text = ""
            nudgeResult = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        attachmentError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Pre-Send Nudge Banner (Scenario 7 / F11)
        AnimatedVisibility(
            visible = nudgeResult?.nudge_text != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SafetyTheme.reviewRiskContainer
                ),
                border = BorderStroke(1.dp, SafetyTheme.reviewRiskBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Nudge",
                            tint = SafetyTheme.reviewRisk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (nudgeResult?.available == false) "Safety check unavailable" else if (nudgeResult?.simulated == true) "SIMULATED safety nudge" else "Safety nudge",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SafetyTheme.reviewRisk
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nudgeResult?.nudge_text
                            ?: "This message may violate communication boundaries. Consider revising before sending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SafetyTheme.onReviewRiskContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { nudgeResult = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Send Anyway",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { nudgeResult = null },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SafetyTheme.reviewRisk,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Revise Draft",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Floating Composer Toolbar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (recording) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recording ${recordingSeconds}s / 60s (auto-send)", modifier = Modifier.weight(1f))
                    TextButton(onClick = { recordingJob?.cancel() }) { Text("Cancel") }
                    Button(onClick = { stopRecording.set(true) }) { Text("Send") }
                }
            } else Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { showAttachMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Media",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Choose from Gallery") },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                imagePicker.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Demo Proxy Image (SIMULATED)") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                loadAttachment(10 * 1024 * 1024, {
                                    context.assets.open("assets/proxy_nsfw.png")
                                }) { onSendImage(it, "demo_proxy_nsfw.png") }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Choose WAV audio") },
                            leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                audioPicker.launch(arrayOf("audio/wav", "audio/x-wav"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Synthetic Audio (Scenario 5)") },
                            leadingIcon = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                loadAttachment(25 * 1024 * 1024, {
                                    context.assets.open("assets/synthetic_audio.wav")
                                }) { onSendVoice(it, "synthetic_audio.wav") }
                            }
                        )
                    }
                }

                TextField(
                    value = text,
                    onValueChange = { handleTextChange(it) },
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() })
                )

                if (text.isNotBlank()) {
                    IconButton(
                        onClick = { submit() }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startRecording()
                            } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record voice message",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
