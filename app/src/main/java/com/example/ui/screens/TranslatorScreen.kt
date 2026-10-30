package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.OcrEngine
import com.example.engine.OfflineTranslator
import com.example.engine.TranslationResult
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import kotlinx.coroutines.launch

@Composable
fun TranslatorScreen(
    initialText: String = "",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf(initialText) }
    var toArabic by remember { mutableStateOf(true) }
    var translationResult by remember { mutableStateOf<TranslationResult?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

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
                selectedBitmap = bmp
                isTranslating = true
                coroutineScope.launch {
                    val ocr = OcrEngine.recognizeTextFromBitmap(bmp)
                    inputText = ocr.fullText
                    toArabic = !OcrEngine.containsArabic(ocr.fullText)
                    translationResult = OfflineTranslator.translate(inputText, toArabic)
                    isTranslating = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) {
            toArabic = !OcrEngine.containsArabic(initialText)
            translationResult = OfflineTranslator.translate(initialText, toArabic)
        }
    }

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "مترجم الصور والنصوص",
                subtitle = "قاموس ونموذج محلي دون إنترنت",
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
                // Language Switch Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (toArabic) "الإنجليزية (English)" else "العربية (Arabic)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                toArabic = !toArabic
                                if (inputText.isNotBlank()) {
                                    coroutineScope.launch {
                                        translationResult = OfflineTranslator.translate(inputText, toArabic)
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "تبديل اتجاه الترجمة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (toArabic) "العربية (Arabic)" else "الإنجليزية (English)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                // Photo translation trigger
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CameraEnhance, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ترجمة نص من صورة (Photo Translate)")
                }
            }

            selectedBitmap?.let { bmp ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("الصورة الأصلية المحفوظة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Original translated image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            item {
                // Input Text Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "النص الأصلي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("اكتب أو الصق الكلمات هنا للترجمة...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translate_input_text"),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                isTranslating = true
                                coroutineScope.launch {
                                    translationResult = OfflineTranslator.translate(inputText, toArabic)
                                    isTranslating = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ترجمة فورية أوفلاين")
                    }
                }
            }

            if (isTranslating) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }

            translationResult?.let { res ->
                item {
                    // Side-by-Side Comparison Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الترجمة الناتجة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AccentGreen
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", res.translatedText))
                                    Toast.makeText(context, "تم نسخ الترجمة!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = res.translatedText.ifEmpty { "لا يوجد نص مترجم" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        if (res.wordPairs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "جدول الكلمات المترجمة جنباً إلى جنب:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                res.wordPairs.take(6).forEach { (orig, trans) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(orig, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(trans, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
