package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxToolsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.LocalThemeController

@Composable
fun SettingsScreen(
    keepScreenAwakeGlobal: Boolean,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val themeController = LocalThemeController.current

    Scaffold(
        topBar = {
            MaxToolsTopBar(
                title = "الإعدادات والتخصيص",
                subtitle = "التحكم بالمظهر والشاشة والخصوصية",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Selection
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "مظهر التطبيق (Day / Night Mode)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "انتقال سينمائي تدريجي وسلس 60FPS",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple(AppThemeMode.LIGHT, "الوضع النهاري (Light)", Icons.Filled.LightMode),
                            Triple(AppThemeMode.DARK, "الوضع الليلي (Dark)", Icons.Filled.DarkMode),
                            Triple(AppThemeMode.SYSTEM, "تلقائي حسب وقت الجهاز (Auto Time)", Icons.Filled.BrightnessAuto)
                        ).forEach { (mode, label, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (themeController.currentMode == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { themeController.onModeChanged(mode) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (themeController.currentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (themeController.currentMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                                RadioButton(
                                    selected = themeController.currentMode == mode,
                                    onClick = { themeController.onModeChanged(mode) }
                                )
                            }
                        }
                    }
                }
            }

            // Keep Screen Awake (FLAG_KEEP_SCREEN_ON)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إبقاء الشاشة مستيقظة دائماً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يمنع قفل الشاشة أثناء استخدام أدوات MaxTools والمشاهدة ونقل الملفات",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepScreenAwakeGlobal,
                            onCheckedChange = { checked ->
                                onToggleKeepScreenAwake(checked)
                                if (checked) {
                                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    Toast.makeText(context, "تم تفعيل إبقاء الشاشة مضاءة", Toast.LENGTH_SHORT).show()
                                } else {
                                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    Toast.makeText(context, "تم تعطيل إبقاء الشاشة مضاءة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("keep_screen_on_switch")
                        )
                    }
                }
            }

            // Storage & Cache management
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "صيانة الذاكرة والتخزين",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تحرير المساحة المؤقتة الناتجة عن المعالجة والتحويلات",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            context.cacheDir.deleteRecursively()
                            context.cacheDir.mkdirs()
                            Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تنظيف الذاكرة المؤقتة (Clear Cache)")
                    }
                }
            }

            // Privacy & Offline Promise
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "أمان تام وخصوصية 100% دون إنترنت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AccentGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تطبيق MaxTools يعمل بنظام Offline First بالكامل، بدون أي خوادم خارجية أو تتبع أو استهلاك للبيانات. جميع عملياتك وملفاتك ومعالجاتك تبقى مشفرة ومحفوظة على جهازك فقط.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MaxTools Suite v2.0 • Offline Edition",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
