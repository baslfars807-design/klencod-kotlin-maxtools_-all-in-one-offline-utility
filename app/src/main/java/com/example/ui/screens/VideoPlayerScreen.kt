package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.VideoHistoryRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerScreen(
    videoHistoryRepository: VideoHistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoTitle by remember { mutableStateOf("مشغل الفيديو الذكي") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var subtitleText by remember { mutableStateOf<String?>(null) }

    // Brightness & Volume feedback overlay
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoUri = it
            videoTitle = it.lastPathSegment ?: "فيديو محلي"
            coroutineScope.launch {
                val savedPos = videoHistoryRepository.getPosition(it.toString())
                if (savedPos > 0) {
                    currentPosMs = savedPos
                    Toast.makeText(context, "استئناف المشاهدة من موضع سابق", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    subtitleText = reader.readLine() ?: "تم تحميل ملف الترجمة بنجاح"
                    Toast.makeText(context, "تم تحميل ملف الترجمة SRT", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة ملف الترجمة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Keep screen awake while playing
    DisposableEffect(videoUri) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            mediaPlayer?.release()
        }
    }

    // Progress polling loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosMs = player.currentPosition.toLong()
                    durationMs = player.duration.coerceAtLeast(1).toLong()
                    videoUri?.let {
                        videoHistoryRepository.savePosition(it.toString(), videoTitle, currentPosMs, durationMs)
                    }
                }
            }
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            if (showControls && !isLocked) {
                MaxToolsTopBar(
                    title = videoTitle,
                    subtitle = "السرعة: ${playbackSpeed}x",
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (videoUri == null) {
                // Empty State / Picker
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "مشغل الفيديو المحلي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يدعم MP4, MKV, 3GP مع التحكم بالسرعة وإيماءات السطوع والصوت",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_video_btn")
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اختر فيديو من جهازك")
                            }
                        }
                    }
                }
            } else {
                // Video Playback Area with Touch Gestures
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { gestureOverlayText = null },
                                onVerticalDrag = { change, dragAmount ->
                                    val isLeft = change.position.x < (size.width / 2)
                                    if (isLeft) {
                                        // Brightness Gesture
                                        activity?.window?.let { win ->
                                            val lp = win.attributes
                                            val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                            val newBrightness = (current - (dragAmount / 500f)).coerceIn(0.05f, 1.0f)
                                            lp.screenBrightness = newBrightness
                                            win.attributes = lp
                                            gestureOverlayText = "السطوع: ${(newBrightness * 100).toInt()}%"
                                        }
                                    } else {
                                        // Volume Gesture
                                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val step = if (dragAmount < 0) 1 else -1
                                        val newVol = (cur + step).coerceIn(0, max)
                                        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        gestureOverlayText = "الصوت: ${(newVol * 100) / max}%"
                                    }
                                }
                            )
                        }
                        .clickable {
                            if (!isLocked) showControls = !showControls else isLocked = false
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        try {
                                            val player = MediaPlayer().apply {
                                                setDisplay(holder)
                                                setDataSource(ctx, videoUri!!)
                                                prepareAsync()
                                                setOnPreparedListener { mp ->
                                                    durationMs = mp.duration.toLong()
                                                    if (currentPosMs > 0 && currentPosMs < durationMs) {
                                                        mp.seekTo(currentPosMs.toInt())
                                                    }
                                                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                                    mp.start()
                                                    isPlaying = true
                                                }
                                                setOnCompletionListener {
                                                    isPlaying = false
                                                }
                                            }
                                            mediaPlayer = player
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtitle Overlay
                    subtitleText?.let { sub ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sub,
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Gesture overlay text badge
                    gestureOverlayText?.let { overlay ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = overlay,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Control Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            // Top action icons: Lock & Subtitles & Change Video
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = { subtitlePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Filled.Subtitles, contentDescription = "ترجمة نصية", tint = Color.White)
                                }
                                IconButton(onClick = { isLocked = true; showControls = false }) {
                                    Icon(Icons.Filled.Lock, contentDescription = "قفل الشاشة", tint = Color.White)
                                }
                                IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = "فيديو آخر", tint = Color.White)
                                }
                            }

                            // Center Play / Pause & Seek buttons
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        mediaPlayer?.let { mp ->
                                            val newPos = (mp.currentPosition - 10000).coerceAtLeast(0)
                                            mp.seekTo(newPos)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Replay10, contentDescription = "تأخير 10 ثوان", tint = Color.White, modifier = Modifier.size(36.dp))
                                }

                                FloatingActionButton(
                                    onClick = {
                                        mediaPlayer?.let { mp ->
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "تشغيل / إيقاف",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        mediaPlayer?.let { mp ->
                                            val newPos = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                                            mp.seekTo(newPos)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Forward10, contentDescription = "تقديم 10 ثوان", tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }

                            // Bottom bar: Progress slider & Speed selectors
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Slider(
                                    value = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                                    onValueChange = { frac ->
                                        val targetMs = (frac * durationMs).toInt()
                                        mediaPlayer?.seekTo(targetMs)
                                        currentPosMs = targetMs.toLong()
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatTime(currentPosMs)} / ${formatTime(durationMs)}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )

                                    // Speed Selector: 0.25x, 0.5x, 1.0x, 1.5x, 2.0x, 4.0x
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                            AssistChip(
                                                onClick = {
                                                    playbackSpeed = speed
                                                    mediaPlayer?.let { mp ->
                                                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                                                    }
                                                },
                                                label = { Text("${speed}x", fontSize = 11.sp) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                isMuted = !isMuted
                                                val vol = if (isMuted) 0f else 1f
                                                mediaPlayer?.setVolume(vol, vol)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "كتم الصوت",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
