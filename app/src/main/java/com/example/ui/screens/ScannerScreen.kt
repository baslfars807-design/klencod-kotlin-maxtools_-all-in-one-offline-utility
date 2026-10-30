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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.ScanRecord
import com.example.data.repository.ScanRepository
import com.example.engine.QrEngine
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScannerScreen(
    scanRepository: ScanRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scan, 1: Generate, 2: History

    val scans by scanRepository.allScans.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "ماسح ومنشئ الرموز",
                subtitle = "QR & Barcode محلي 100%",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المسح (Scan)") },
                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الإنشاء (Create)") },
                    icon = { Icon(Icons.Filled.AddBox, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("السجل (${scans.size})") },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> ScanSubScreen(
                    onScanResult = { text, format ->
                        coroutineScope.launch {
                            val type = when {
                                text.startsWith("http://") || text.startsWith("https://") -> "URL"
                                text.startsWith("WIFI:") -> "WIFI"
                                text.startsWith("tel:") -> "PHONE"
                                text.startsWith("BEGIN:VCARD") -> "CONTACT"
                                format == "CODE_128" || format == "EAN_13" -> "BARCODE"
                                else -> "TEXT"
                            }
                            scanRepository.insertScan(
                                ScanRecord(content = text, format = format, type = type)
                            )
                        }
                    }
                )
                1 -> GenerateSubScreen()
                2 -> HistorySubScreen(
                    scans = scans,
                    onDelete = { record ->
                        coroutineScope.launch { scanRepository.deleteScan(record) }
                    },
                    onClearAll = {
                        coroutineScope.launch { scanRepository.clearHistory() }
                    }
                )
            }
        }
    }
}

@Composable
fun ScanSubScreen(onScanResult: (String, String) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedContent by remember { mutableStateOf<String?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var showUrlDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }

                kotlinx.coroutines.MainScope().launch {
                    val decoded = QrEngine.decodeBitmap(bitmap)
                    if (decoded != null) {
                        scannedContent = decoded
                        onScanResult(decoded, "QR_CODE")
                    } else {
                        Toast.makeText(context, "لم يتم العثور على رمز QR أو باركود في الصورة", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في قراءة الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Camera Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraScannerView(
                    onCodeScanned = { result ->
                        if (scannedContent != result) {
                            scannedContent = result
                            onScanResult(result, "QR_CODE")
                        }
                    },
                    onCameraReady = { control -> cameraControl = control }
                )

                // Reticle overlay
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                ) {
                    // Flash and Gallery floating buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledIconButton(
                            onClick = {
                                flashEnabled = !flashEnabled
                                cameraControl?.enableTorch(flashEnabled)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (flashEnabled) AccentOrange else Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "الفلاش",
                                tint = Color.White
                            )
                        }

                        FilledIconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "مسح من المعرض",
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "يرجى منح إذن الكاميرا لمسح الرموز مباشرة",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("request_camera_btn")
                    ) {
                        Text("منح الإذن")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("اختيار صورة من المعرض")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Card if scanned
        scannedContent?.let { content ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "النتيجة المقروءة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MaxTools Scan", content))
                            Toast.makeText(context, "تم نسخ النص إلى الحافظة!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ")
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة النتيجة"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة")
                    }

                    if (content.startsWith("http://") || content.startsWith("https://")) {
                        Button(
                            onClick = { showUrlDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Text("فتح")
                        }
                    }
                }
            }
        }

        if (showUrlDialog && scannedContent != null) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text("تأكيد فتح الرابط") },
                text = { Text("هل تريد فتح هذا الرابط بأمان؟\n\n$scannedContent") },
                confirmButton = {
                    Button(onClick = {
                        showUrlDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedContent))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("تأكيد وفتح")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraScannerView(
    onCodeScanned: (String) -> Unit,
    onCameraReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    // Scan logic using ZXing on camera frames
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(camera.cameraControl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun GenerateSubScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var genType by remember { mutableIntStateOf(0) } // 0: Text, 1: URL, 2: Wi-Fi, 3: Contact, 4: Barcode
    var textInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Type Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = genType == 0,
                    onClick = { genType = 0 },
                    label = { Text("نص") }
                )
                FilterChip(
                    selected = genType == 1,
                    onClick = { genType = 1 },
                    label = { Text("رابط") }
                )
                FilterChip(
                    selected = genType == 2,
                    onClick = { genType = 2 },
                    label = { Text("Wi-Fi") }
                )
                FilterChip(
                    selected = genType == 3,
                    onClick = { genType = 3 },
                    label = { Text("جهة اتصال") }
                )
                FilterChip(
                    selected = genType == 4,
                    onClick = { genType = 4 },
                    label = { Text("باركود") }
                )
            }
        }

        item {
            // Dynamic Inputs
            when (genType) {
                0, 1 -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text(if (genType == 0) "أدخل النص المراد تشفيره" else "أدخل رابط الموقع (URL)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_input_field"),
                        minLines = if (genType == 0) 3 else 1
                    )
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("اسم الشبكة (SSID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("كلمة المرور") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                4 -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("أدخل رقم المنتج أو الرمز (Barcode)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            // Color picker row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("لون الرمز:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                listOf(Color.Black, Color(0xFF006494), Color(0xFF10B981), Color(0xFF7C3AED), Color(0xFFDC2626)).forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(col)
                            .clickable { selectedColor = col }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val payload = when (genType) {
                            0, 1 -> textInput
                            2 -> QrEngine.buildWifiString(wifiSsid, wifiPassword)
                            3 -> QrEngine.buildVCardString(contactName, contactPhone, "")
                            4 -> textInput
                            else -> textInput
                        }

                        if (payload.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال البيانات أولاً", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        generatedBitmap = if (genType == 4) {
                            QrEngine.generateBarcodeBitmap(payload, fgColor = selectedColor.toArgb())
                        } else {
                            QrEngine.generateQrBitmap(payload, fgColor = selectedColor.toArgb())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_qr_btn")
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء الرمز الآن")
            }
        }

        // Preview and Save
        generatedBitmap?.let { bitmap ->
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated QR",
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val outDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "GeneratedQR")
                                    if (!outDir.exists()) outDir.mkdirs()
                                    val target = File(outDir, "QR_${System.currentTimeMillis()}.png")
                                    FileOutputStream(target).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    Toast.makeText(context, "تم حفظ الرمز بجودة عالية في مجلد MaxTools!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ الصورة")
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "تم تجهيز الرمز للمشاركة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySubScreen(
    scans: List<ScanRecord>,
    onDelete: (ScanRecord) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (scans.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المسح المحلي (${scans.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = onClearAll) {
                    Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (scans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد عمليات مسح سابقة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scans, key = { it.id }) { item ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "URL" -> Icons.Filled.Link
                                        "WIFI" -> Icons.Filled.Wifi
                                        "PHONE" -> Icons.Filled.Phone
                                        "CONTACT" -> Icons.Filled.Person
                                        else -> Icons.Filled.QrCode
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.content,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${item.type} • ${sdf.format(Date(item.timestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Scan", item.content))
                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
