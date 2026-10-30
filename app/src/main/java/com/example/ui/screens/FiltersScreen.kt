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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.engine.CinematicFilterType
import com.example.engine.FilterEngine
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import kotlinx.coroutines.launch

@Composable
fun FiltersScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(CinematicFilterType.CINEMATIC) }
    var intensity by remember { mutableFloatStateOf(0.85f) }
    var showBeforeAfter by remember { mutableStateOf(false) }

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
                originalBitmap = bmp
                coroutineScope.launch {
                    filteredBitmap = FilterEngine.applyFilter(bmp, selectedFilter, intensity)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل فتح الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "الفلاتر السينمائية",
                subtitle = "محرر ألوان سينمائي مدمج دون إنترنت",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_pick_photo_btn")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (originalBitmap == null) "اختيار صورة للمحرر" else "تغيير الصورة")
                    }

                    if (originalBitmap != null) {
                        OutlinedButton(
                            onClick = { showBeforeAfter = !showBeforeAfter }
                        ) {
                            Icon(Icons.Filled.Compare, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showBeforeAfter) "عرض النتيجة" else "مقارنة قبل/بعد")
                        }
                    }
                }
            }

            // Preview Area
            val displayBitmap = if (showBeforeAfter) originalBitmap else (filteredBitmap ?: originalBitmap)
            displayBitmap?.let { bmp ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showBeforeAfter) {
                                Text(
                                    text = "معاينة الأصل (قبل التعديل):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Filtered preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                            )
                        }
                    }
                }
            }

            if (originalBitmap != null) {
                // Filter Type Selector (10 Presets)
                item {
                    Text(
                        text = "اختر الفلتر السينمائي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CinematicFilterType.values()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = {
                                    selectedFilter = filter
                                    coroutineScope.launch {
                                        filteredBitmap = FilterEngine.applyFilter(originalBitmap!!, filter, intensity)
                                    }
                                },
                                label = { Text(filter.titleAr) }
                            )
                        }
                    }
                }

                // Intensity Slider (0% - 100%)
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("شدة التأثير السينمائي:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${(intensity * 100).toInt()}%", fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                        Slider(
                            value = intensity,
                            onValueChange = { newVal ->
                                intensity = newVal
                                coroutineScope.launch {
                                    filteredBitmap = FilterEngine.applyFilter(originalBitmap!!, selectedFilter, newVal)
                                }
                            },
                            valueRange = 0f..1f
                        )
                    }
                }

                // Save button
                item {
                    Button(
                        onClick = {
                            filteredBitmap?.let { bmp ->
                                coroutineScope.launch {
                                    val savedFile = FilterEngine.saveFilteredImage(context, bmp, "photo.jpg")
                                    Toast.makeText(context, "تم حفظ الصورة المعدلة في مجلد Cinematic!\nالملف الأصلي لم يتغير.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ الصورة المعدلة بجودة أصلية")
                    }
                }
            }
        }
    }
}
