package com.example.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.FavoriteRepository
import com.example.data.repository.TrashRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class FileSortOrder {
    NAME,
    DATE,
    SIZE
}

@Composable
fun FileManagerScreen(
    trashRepository: TrashRepository,
    favoriteRepository: FavoriteRepository,
    initialCategory: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    var currentDir by remember {
        mutableStateOf(context.getExternalFilesDir(null) ?: context.filesDir)
    }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NAME) }
    var selectedCategoryFilter by remember { mutableStateOf(initialCategory) }

    // Dialogs
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var fileDetailsDialog by remember { mutableStateOf<File?>(null) }

    // Refresh trigger
    var refreshKey by remember { mutableIntStateOf(0) }

    val filesList = remember(currentDir, searchQuery, sortOrder, selectedCategoryFilter, refreshKey) {
        val all = currentDir.listFiles()?.toList() ?: emptyList()

        val filtered = all.filter { file ->
            val matchSearch = searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)
            val matchCategory = when (selectedCategoryFilter) {
                "images" -> file.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")
                "videos" -> file.extension.lowercase() in setOf("mp4", "mkv", "3gp", "avi")
                "audio" -> file.extension.lowercase() in setOf("mp3", "m4a", "aac", "wav")
                "documents" -> file.extension.lowercase() in setOf("pdf", "doc", "docx", "txt")
                else -> true
            }
            matchSearch && matchCategory
        }

        when (sortOrder) {
            FileSortOrder.NAME -> filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            FileSortOrder.DATE -> filtered.sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified() }))
            FileSortOrder.SIZE -> filtered.sortedWith(compareBy({ !it.isDirectory }, { -it.length() }))
        }
    }

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "مدير الملفات الشامل",
                subtitle = currentDir.name,
                canNavigateBack = currentDir.parentFile != null && currentDir != context.filesDir,
                onNavigateBack = {
                    val parent = currentDir.parentFile
                    if (parent != null && parent.canRead()) {
                        currentDir = parent
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "إنشاء مجلد")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث عن ملف أو مجلد...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(null to "الكل", "images" to "صور", "videos" to "فيديو", "documents" to "مستندات").forEach { (cat, label) ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Sort toggle
                IconButton(
                    onClick = {
                        sortOrder = when (sortOrder) {
                            FileSortOrder.NAME -> FileSortOrder.DATE
                            FileSortOrder.DATE -> FileSortOrder.SIZE
                            FileSortOrder.SIZE -> FileSortOrder.NAME
                        }
                    }
                ) {
                    Icon(
                        imageVector = when (sortOrder) {
                            FileSortOrder.NAME -> Icons.Filled.SortByAlpha
                            FileSortOrder.DATE -> Icons.Filled.Schedule
                            FileSortOrder.SIZE -> Icons.Filled.DataUsage
                        },
                        contentDescription = "ترتيب الملفات",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File items
            if (filesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("المجلد فارغ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filesList, key = { it.absolutePath }) { file ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else {
                                    fileDetailsDialog = file
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (file.isDirectory) AccentOrange.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (file.isDirectory) AccentOrange else AccentCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (file.isDirectory) "مجلد" else "${file.length() / 1024} KB • ${sdf.format(Date(file.lastModified()))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Favorite Toggle
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        favoriteRepository.toggleFavorite(file)
                                        Toast.makeText(context, "تم تحديث المفضلة", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.StarBorder, contentDescription = "المفضلة", modifier = Modifier.size(18.dp))
                                }

                                // Rename
                                IconButton(onClick = {
                                    fileToRename = file
                                    newFileName = file.name
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "إعادة تسمية", modifier = Modifier.size(18.dp))
                                }

                                // Delete to Trash
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val moved = trashRepository.moveToTrash(file)
                                        if (moved) {
                                            refreshKey++
                                            Toast.makeText(context, "تم نقل الملف إلى سلة المهملات بأمان", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشل حذف الملف", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = AccentRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Create Folder Dialog
            if (showCreateFolderDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateFolderDialog = false },
                    title = { Text("إنشاء مجلد جديد") },
                    text = {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("اسم المجلد") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newFolderName.isNotBlank()) {
                                val newDir = File(currentDir, newFolderName.trim())
                                if (newDir.mkdir()) {
                                    refreshKey++
                                    Toast.makeText(context, "تم إنشاء المجلد بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showCreateFolderDialog = false
                            newFolderName = ""
                        }) {
                            Text("إنشاء")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateFolderDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Rename Dialog
            fileToRename?.let { targetFile ->
                AlertDialog(
                    onDismissRequest = { fileToRename = null },
                    title = { Text("إعادة تسمية الملف") },
                    text = {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text("الاسم الجديد") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newFileName.isNotBlank()) {
                                val dest = File(targetFile.parentFile, newFileName.trim())
                                if (targetFile.renameTo(dest)) {
                                    refreshKey++
                                    Toast.makeText(context, "تمت إعادة التسمية بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            fileToRename = null
                        }) {
                            Text("حفظ الاسم")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { fileToRename = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Details Dialog
            fileDetailsDialog?.let { file ->
                AlertDialog(
                    onDismissRequest = { fileDetailsDialog = null },
                    title = { Text("تفاصيل الملف") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الاسم: ${file.name}", fontWeight = FontWeight.Bold)
                            Text("المسار: ${file.absolutePath}", fontSize = 12.sp)
                            Text("الحجم: ${file.length() / 1024} كيلوبايت")
                            Text("تاريخ التعديل: ${sdf.format(Date(file.lastModified()))}")
                            Text("الصلاحيات: ${if (file.canWrite()) "قراءة وكتابة" else "قراءة فقط"}")
                        }
                    },
                    confirmButton = {
                        Button(onClick = { fileDetailsDialog = null }) {
                            Text("إغلاق")
                        }
                    }
                )
            }
        }
    }
}
