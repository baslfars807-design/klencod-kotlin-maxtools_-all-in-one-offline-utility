package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import com.example.engine.P2PSharingEngine
import com.example.engine.QrEngine
import com.example.engine.TransferState
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SharingScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSenderMode by remember { mutableStateOf(false) } // false = Receive, true = Send
    var transferState by remember { mutableStateOf(TransferState()) }
    var receiverIp by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val list = mutableListOf<File>()
            for (uri in uris) {
                try {
                    val returnCursor = context.contentResolver.query(uri, null, null, null, null)
                    val nameIndex = returnCursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
                    returnCursor?.moveToFirst()
                    val fileName = if (nameIndex != -1) returnCursor?.getString(nameIndex) ?: "file_${System.currentTimeMillis()}" else "file_${System.currentTimeMillis()}"
                    returnCursor?.close()

                    val tempFile = File(context.cacheDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    list.add(tempFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            selectedFiles = list
        }
    }

    // When entering Receiver mode, start server & generate connection QR
    LaunchedEffect(isSenderMode) {
        if (!isSenderMode) {
            val localIp = P2PSharingEngine.getLocalIpAddress(context)
            receiverIp = localIp
            val qrText = "MAXSHARE:$localIp:${P2PSharingEngine.DEFAULT_PORT}"
            qrCodeBitmap = QrEngine.generateQrBitmap(qrText, 360)

            coroutineScope.launch {
                P2PSharingEngine.startReceiving(
                    context = context,
                    port = P2PSharingEngine.DEFAULT_PORT,
                    onUpdate = { state -> transferState = state }
                )
            }
        } else {
            P2PSharingEngine.cancel()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            P2PSharingEngine.cancel()
        }
    }

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "مشاركة الملفات دون إنترنت",
                subtitle = "نقل مباشر P2P بسرعة فائقة",
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
                // Mode Toggle: Send vs Receive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isSenderMode = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSenderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isSenderMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استقبال ملفات")
                    }

                    Button(
                        onClick = { isSenderMode = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSenderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSenderMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال ملفات")
                    }
                }
            }

            // Receiver View
            if (!isSenderMode) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "رمز الاستقبال (امسحه من جهاز الإرسال):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            qrCodeBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Receiver QR",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "عنوان IP المحلي: $receiverIp",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Sender View
            if (isSenderMode) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "إعدادات الاتصال والملفات:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = receiverIp,
                            onValueChange = { receiverIp = it },
                            label = { Text("أدخل عنوان IP لجهاز الاستقبال (مثال: 192.168.1.5)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار ملفات للإرسال (${selectedFiles.size} محددة)")
                        }
                        if (selectedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (receiverIp.isBlank()) {
                                        Toast.makeText(context, "يرجى إدخال IP المستقبل", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        P2PSharingEngine.startSending(
                                            targetIp = receiverIp.trim(),
                                            targetPort = P2PSharingEngine.DEFAULT_PORT,
                                            filesToSend = selectedFiles,
                                            onUpdate = { state -> transferState = state }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_transfer_btn")
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بدء الإرسال الآن")
                            }
                        }
                    }
                }
            }

            // Live Transfer Status Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (transferState.isRunning) "حالة النقل المباشر:" else "حالة الاتصال:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (transferState.isRunning) {
                            Text(
                                text = String.format("%.2f MB/s", transferState.speedMbps),
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentCyan,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = transferState.statusMessage.ifBlank { "جاهز للنقل دون إنترنت" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (transferState.isRunning) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { transferState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(transferState.progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${transferState.filesSentOrReceived} / ${transferState.totalFiles} ملفات",
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { P2PSharingEngine.cancel() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إلغاء عملية النقل")
                        }
                    }

                    transferState.error?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تنبيه: $err",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
