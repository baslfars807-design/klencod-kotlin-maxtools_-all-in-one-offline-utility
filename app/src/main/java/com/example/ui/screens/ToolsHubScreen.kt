package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.*

data class ToolDefinition(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ToolsHubScreen(
    onLaunchTool: (String) -> Unit
) {
    val toolsList = listOf(
        ToolDefinition(
            id = "scanner",
            titleAr = "ماسح وإنشاء QR والباركود",
            titleEn = "QR & Barcode Scanner / Generator",
            description = "مسح مباشر بالكاميرا أو المعرض، فلاش، تكبير، وتوليد رموز Wi-Fi وروابط وجهات اتصال محلياً.",
            icon = Icons.Filled.QrCodeScanner,
            color = AccentCyan
        ),
        ToolDefinition(
            id = "ocr",
            titleAr = "استخراج النصوص OCR",
            titleEn = "Offline Text Recognition",
            description = "استخراج النص العربي والإنجليزي والأرقام من الصور دون إنترنت، مع إمكانية البحث والنسخ والمشاركة.",
            icon = Icons.Filled.DocumentScanner,
            color = AccentPurple
        ),
        ToolDefinition(
            id = "translator",
            titleAr = "مترجم الصور والنصوص",
            titleEn = "Offline Photo & Text Translator",
            description = "ترجمة مدمجة ثنائية الاتجاه (عربي ↔ إنجليزي) مع عرض مقارنة جنباً إلى جنب دون إنترنت.",
            icon = Icons.Filled.Translate,
            color = AccentOrange
        ),
        ToolDefinition(
            id = "sharing",
            titleAr = "مشاركة الملفات P2P دون إنترنت",
            titleEn = "Offline Direct File Share",
            description = "إرسال واستقبال الصور والفيديوهات والـ APK بسرعات فائقة مباشرة بين الأجهزة مع عداد MB/s.",
            icon = Icons.Filled.Share,
            color = AccentGreen
        ),
        ToolDefinition(
            id = "video_player",
            titleAr = "مشغل الفيديو الاحترافي",
            titleEn = "Pro Video Player",
            description = "دعم تسريع وإبطاء الفيديو (0.25x إلى 4x)، إيماءات السطوع والصوت، الترجمة الخارجية وقفل الشاشة.",
            icon = Icons.Filled.PlayCircle,
            color = AccentRed
        ),
        ToolDefinition(
            id = "media_converter",
            titleAr = "محول الفيديو والصوت",
            titleEn = "Video to Audio Extractor",
            description = "استخراج الصوت بجودات متعددة ومعدل بت (128k, 192k, 320k) مع مؤشر تقدم دقيق وحجم متوقع.",
            icon = Icons.Filled.Audiotrack,
            color = LightPrimary
        ),
        ToolDefinition(
            id = "filters",
            titleAr = "الفلاتر السينمائية",
            titleEn = "Cinematic Photo Filters",
            description = "10 تأثيرات ألوان سينمائية احترافية مع شريط تحكم في الشدة ومقارنة قبل وبعد وحفظ مستقل.",
            icon = Icons.Filled.AutoFixHigh,
            color = AccentOrange
        ),
        ToolDefinition(
            id = "format_converter",
            titleAr = "محول صيغ الملفات",
            titleEn = "Format Converter (PDF, SVG, ICO)",
            description = "تحويل حقيقي وتوليد مستندات PDF ورسوم متجهة SVG وأيقونات Windows ICO وضغط WebP/PNG/JPG.",
            icon = Icons.Filled.Transform,
            color = AccentCyan
        ),
        ToolDefinition(
            id = "apk_manager",
            titleAr = "مثبت التطبيقات الذكي",
            titleEn = "APK / APKS / XAPK Installer",
            description = "فحص الحزم، فحص سلامة الملفات، استخراج الأيقونة والتفاصيل، وتثبيت آمن عبر نظام Android الرسمي.",
            icon = Icons.Filled.Android,
            color = AccentGreen
        ),
        ToolDefinition(
            id = "trash",
            titleAr = "سلة المهملات الآمنة",
            titleEn = "Recycle Bin",
            description = "حماية ملفاتك من الحذف غير المقصود، إمكانية الاستعادة الفورية أو الحذف النهائي وتفريغ السلة.",
            icon = Icons.Filled.Delete,
            color = AccentRed
        )
    )

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "مركز الأدوات المتقدمة",
                subtitle = "جميع الأدوات تعمل محلياً 100% دون خوادم"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "الأدوات المتاحة (${toolsList.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(toolsList.size) { index ->
                val tool = toolsList[index]
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onLaunchTool(tool.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tool.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.titleAr,
                                tint = tool.color,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.titleAr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.description,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
