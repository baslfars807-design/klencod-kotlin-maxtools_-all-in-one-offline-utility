package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FavoriteRecord
import com.example.data.repository.StorageStats
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.components.StatTile
import com.example.ui.components.ToolShortcutButton
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    storageStats: StorageStats,
    favorites: List<FavoriteRecord>,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToTool: (String) -> Unit
) {
    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "MaxTools",
                subtitle = "مجموعة الأدوات الشاملة دون إنترنت"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Storage Overview Card
                StorageOverviewCard(storageStats = storageStats)
            }

            item {
                // Section Title: Categories
                Text(
                    text = "أقسام الملفات والتخزين",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 2x3 Grid of categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        title = "الصور",
                        count = "${storageStats.photoCount}",
                        icon = Icons.Filled.PhotoLibrary,
                        iconBgColor = AccentCyan,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToCategory("images") }

                    StatTile(
                        title = "الفيديوهات",
                        count = "${storageStats.videoCount}",
                        icon = Icons.Filled.VideoLibrary,
                        iconBgColor = AccentOrange,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToCategory("videos") }

                    StatTile(
                        title = "الصوتيات",
                        count = "${storageStats.audioCount}",
                        icon = Icons.Filled.Audiotrack,
                        iconBgColor = AccentPurple,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToCategory("audio") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        title = "المستندات",
                        count = "${storageStats.documentCount}",
                        icon = Icons.Filled.Description,
                        iconBgColor = LightPrimary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToCategory("documents") }

                    StatTile(
                        title = "ملفات APK",
                        count = "${storageStats.apkCount}",
                        icon = Icons.Filled.Android,
                        iconBgColor = AccentGreen,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToTool("apk_manager") }

                    StatTile(
                        title = "سلة المهملات",
                        count = "محمي",
                        icon = Icons.Filled.Delete,
                        iconBgColor = AccentRed,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToTool("trash") }
                }
            }

            item {
                // Section Title: Quick Tools
                Text(
                    text = "الأدوات السريعة دون إنترنت",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Grid of 8 quick tools
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolShortcutButton(
                            title = "ماسح QR",
                            icon = Icons.Filled.QrCodeScanner,
                            accentColor = AccentCyan,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("scanner") }

                        ToolShortcutButton(
                            title = "استخراج OCR",
                            icon = Icons.Filled.DocumentScanner,
                            accentColor = AccentPurple,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("ocr") }

                        ToolShortcutButton(
                            title = "مترجم الصور",
                            icon = Icons.Filled.Translate,
                            accentColor = AccentOrange,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("translator") }

                        ToolShortcutButton(
                            title = "مشاركة P2P",
                            icon = Icons.Filled.Share,
                            accentColor = AccentGreen,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("sharing") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolShortcutButton(
                            title = "مشغل الفيديو",
                            icon = Icons.Filled.PlayCircle,
                            accentColor = AccentRed,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("video_player") }

                        ToolShortcutButton(
                            title = "محول الصوت",
                            icon = Icons.Filled.MusicNote,
                            accentColor = LightPrimary,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("media_converter") }

                        ToolShortcutButton(
                            title = "فلاتر سينمائية",
                            icon = Icons.Filled.AutoFixHigh,
                            accentColor = AccentOrange,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("filters") }

                        ToolShortcutButton(
                            title = "تحويل الصيغ",
                            icon = Icons.Filled.Transform,
                            accentColor = AccentCyan,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToTool("format_converter") }
                    }
                }
            }

            // Favorites section
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "الملفات المفضلة (${favorites.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        favorites.take(4).forEach { fav ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "مفضل",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fav.fileName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${fav.fileType} • ${(fav.fileSize / 1024)} كيلوبايت",
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StorageOverviewCard(storageStats: StorageStats) {
    val usedGb = String.format("%.1f", storageStats.usedBytes / (1024.0 * 1024.0 * 1024.0))
    val totalGb = String.format("%.1f", storageStats.totalBytes / (1024.0 * 1024.0 * 1024.0))
    val freeGb = String.format("%.1f", storageStats.freeBytes / (1024.0 * 1024.0 * 1024.0))
    val percentage = (storageStats.usedPercentage * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_overview_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "التخزين",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ذاكرة التخزين المحلية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "متبقي $freeGb جيجابايت متاح",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$percentage%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { storageStats.usedPercentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مستخدم: $usedGb جيجابايت",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "إجمالي: $totalGb جيجابايت",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
