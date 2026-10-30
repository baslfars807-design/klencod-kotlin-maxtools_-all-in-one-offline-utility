package com.example.ui.screens

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AudioBitrate
import com.example.engine.ConversionProgress
import com.example.engine.MediaConverterEngine
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MediaConverterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf<String?>(null) }
    var durationSeconds by remember { mutableLongStateOf(0L) }
    var selectedBitrate by remember { mutableStateOf(AudioBitrate.HIGH) }
    var selectedExt by remember { mutableStateOf("m4a") } // m4a or mp3
    var conversionProgress by remember { mutableStateOf<ConversionProgress?>(null) }
    var isConverting by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            videoName = it.lastPathSegment ?: "video.mp4"
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, it)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationSeconds = (durStr?.toLongOrNull() ?: 10000L) / 1000L
                retriever.release()
            } catch (e: Exception) {
                durationSeconds = 60L
            }
        }
    }

    val estimatedBytes = MediaConverterEngine.estimateOutputSize(durationSeconds, selectedBitrate.kbps)
    val estimatedMb = String.format("%.2f MB", estimatedBytes / (1024.0 * 1024.0))

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "محول الفيديو والصوت",
                subtitle = "استخراج الصوت بجودة فائقة دون إنترنت",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Button(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("converter_pick_video")
                ) {
                    Icon(Icons.Filled.VideoFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedVideoUri == null) "اختيار فيديو من الجهاز" else "تغيير الفيديو المحدد")
                }
            }

            selectedVideoUri?.let {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "الفيديو المحدد:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = videoName ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "المدة: ${durationSeconds / 60} دقيقة و ${durationSeconds % 60} ثانية",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "إعدادات استخراج الصوت:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Bitrate Selector
                        Text("معدل البت (Bitrate):", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioBitrate.values().forEach { br ->
                                FilterChip(
                                    selected = selectedBitrate == br,
                                    onClick = { selectedBitrate = br },
                                    label = { Text("${br.kbps}k") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Format Selector
                        Text("صيغة الملف الناتج:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("m4a" to "M4A (عالي الجودة)", "aac" to "AAC (مباشر)", "mp3" to "MP3 (متوافق)").forEach { (ext, label) ->
                                FilterChip(
                                    selected = selectedExt == ext,
                                    onClick = { selectedExt = ext },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Estimated size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الحجم المتوقع للملف:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(estimatedMb, fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isConverting = true
                                coroutineScope.launch {
                                    try {
                                        MediaConverterEngine.extractAudioFromVideo(
                                            context = context,
                                            videoUri = selectedVideoUri!!,
                                            outputFormat = selectedExt,
                                            bitrate = selectedBitrate,
                                            onProgress = { prog ->
                                                conversionProgress = prog
                                                if (prog.isCompleted) {
                                                    isConverting = false
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isConverting = false
                                        Toast.makeText(context, "فشل التحويل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isConverting,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AudioFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConverting) "جاري التحويل..." else "بدء استخراج الصوت الآن")
                        }
                    }
                }
            }

            conversionProgress?.let { prog ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (prog.isCompleted) "اكتمل التحويل بنجاح!" else "تقدم استخراج الصوت:",
                                fontWeight = FontWeight.Bold,
                                color = if (prog.isCompleted) AccentGreen else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${(prog.percentage * 100).toInt()}%",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { prog.percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentGreen
                        )
                        prog.outputFile?.let { f ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تم الحفظ في: ${f.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
