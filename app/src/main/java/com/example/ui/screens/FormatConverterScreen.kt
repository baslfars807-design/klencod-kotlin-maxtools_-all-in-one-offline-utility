package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ConversionSettings
import com.example.engine.FileConverterEngine
import com.example.engine.TargetFormat
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FormatConverterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fileName by remember { mutableStateOf("image.png") }
    var selectedFormat by remember { mutableStateOf(TargetFormat.PDF) }
    var quality by remember { mutableFloatStateOf(90f) }
    var customWidth by remember { mutableStateOf("") }
    var customHeight by remember { mutableStateOf("") }
    var convertedFile by remember { mutableStateOf<File?>(null) }
    var isConverting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                sourceBitmap = bmp
                fileName = it.lastPathSegment ?: "image"
                customWidth = bmp.width.toString()
                customHeight = bmp.height.toString()
                convertedFile = null
            } catch (e: Exception) {
                Toast.makeText(context, "فشل فتح الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "محول صيغ الملفات",
                subtitle = "تحويل حقيقي (PDF, SVG, ICO, WebP, PNG, JPG)",
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
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("format_converter_pick_btn")
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (sourceBitmap == null) "اختيار الملف المراد تحويله" else "تغيير الملف")
                }
            }

            sourceBitmap?.let { bmp ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("الأبعاد الأصلية: ${bmp.width} × ${bmp.height} بكسل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("اختر الصيغة المستهدفة للتحويل:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TargetFormat.values().forEach { fmt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedFormat == fmt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedFormat == fmt,
                                        onClick = { selectedFormat = fmt }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fmt.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("امتداد .${fmt.extension}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Quality setting for lossy formats
                        if (selectedFormat == TargetFormat.JPG || selectedFormat == TargetFormat.WEBP) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("جودة الضغط:", fontSize = 13.sp)
                                Text("${quality.toInt()}%", fontWeight = FontWeight.Bold, color = AccentCyan)
                            }
                            Slider(
                                value = quality,
                                onValueChange = { quality = it },
                                valueRange = 10f..100f
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom dimensions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customWidth,
                                onValueChange = { customWidth = it },
                                label = { Text("العرض (W)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customHeight,
                                onValueChange = { customHeight = it },
                                label = { Text("الارتفاع (H)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isConverting = true
                                coroutineScope.launch {
                                    val targetW = customWidth.toIntOrNull()
                                    val targetH = customHeight.toIntOrNull()
                                    val settings = ConversionSettings(
                                        format = selectedFormat,
                                        quality = quality.toInt(),
                                        targetWidth = targetW,
                                        targetHeight = targetH
                                    )
                                    val out = FileConverterEngine.convertBitmap(
                                        context = context,
                                        sourceBitmap = bmp,
                                        settings = settings,
                                        outputFileName = fileName
                                    )
                                    convertedFile = out
                                    isConverting = false
                                    Toast.makeText(context, "تم تحويل الملف بنجاح إلى صيغة ${selectedFormat.extension.uppercase()}!", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isConverting,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConverting) "جاري التحويل الحقيقي..." else "تنفيذ التحويل الآن")
                        }
                    }
                }
            }

            convertedFile?.let { file ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FileDownloadDone, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اكتمل التحويل الحقيقي!", fontWeight = FontWeight.Bold, color = AccentGreen)
                                Text("اسم الملف: ${file.name}", fontSize = 13.sp)
                                Text("الحجم: ${file.length() / 1024} كيلوبايت", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
