package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.TrashRecord
import com.example.data.repository.TrashRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrashScreen(
    trashRepository: TrashRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trashList by trashRepository.allTrash.collectAsState(initial = emptyList())
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    val totalBytes = trashList.sumOf { it.fileSize }
    val totalSizeMb = String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "سلة المهملات الآمنة",
                subtitle = "المساحة المحجوزة: $totalSizeMb",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (trashList.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirmDialog = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "تفريغ السلة", tint = AccentRed)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (trashList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "سلة المهملات فارغة حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أي ملف يتم حذفه من مدير الملفات ينتقل هنا أولاً للحماية من الفقدان",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trashList, key = { it.id }) { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.fileName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.fileSize / 1024} KB • حُذف: ${sdf.format(Date(item.deletedTimestamp))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Restore action
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val restored = trashRepository.restoreFile(item)
                                            if (restored) {
                                                Toast.makeText(context, "تمت استعادة الملف إلى مكانه الأصلي", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تعذر استعادة الملف", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Restore, contentDescription = "استعادة", tint = AccentGreen)
                                }

                                // Permanent delete action
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            trashRepository.permanentlyDelete(item)
                                            Toast.makeText(context, "تم حذف الملف نهائياً", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = "حذف نهائي", tint = AccentRed)
                                }
                            }
                        }
                    }
                }
            }

            if (showEmptyConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showEmptyConfirmDialog = false },
                    title = { Text("تفريغ سلة المهملات") },
                    text = { Text("هل أنت متأكد من تفريغ سلة المهملات بالكامل وحذف جميع الملفات نهائياً؟ هذا الإجراء لا يمكن التراجع عنه.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEmptyConfirmDialog = false
                                coroutineScope.launch {
                                    trashRepository.emptyTrash()
                                    Toast.makeText(context, "تم تفريغ سلة المهملات بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Text("تفريغ وحذف نهائي")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEmptyConfirmDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
