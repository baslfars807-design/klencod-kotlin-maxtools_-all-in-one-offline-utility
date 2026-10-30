package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.MaxToolsDatabase
import com.example.data.model.AppThemeMode
import com.example.data.repository.*
import com.example.ui.screens.*
import com.example.ui.theme.MaxToolsTheme
import kotlinx.coroutines.launch

enum class MainNavTab(val id: String, val titleAr: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "الرئيسية", Icons.Filled.Dashboard),
    TOOLS("tools", "الأدوات", Icons.Filled.Build),
    SCANNER("scanner", "الماسح", Icons.Filled.QrCodeScanner),
    FILES("files", "الملفات", Icons.Filled.Folder),
    SETTINGS("settings", "الإعدادات", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = MaxToolsDatabase.getDatabase(this)
        val scanRepo = ScanRepository(db.scanDao())
        val trashRepo = TrashRepository(db.trashDao(), this)
        val videoRepo = VideoHistoryRepository(db.videoHistoryDao())
        val favRepo = FavoriteRepository(db.favoriteDao())
        val storageRepo = StorageRepository(this)

        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
            var keepScreenAwake by remember { mutableStateOf(false) }

            MaxToolsTheme(
                themeMode = themeMode,
                onThemeModeChange = { themeMode = it }
            ) {
                MainAppContent(
                    scanRepo = scanRepo,
                    trashRepo = trashRepo,
                    videoRepo = videoRepo,
                    favRepo = favRepo,
                    storageRepo = storageRepo,
                    keepScreenAwake = keepScreenAwake,
                    onToggleKeepScreenAwake = { keepScreenAwake = it }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    scanRepo: ScanRepository,
    trashRepo: TrashRepository,
    videoRepo: VideoHistoryRepository,
    favRepo: FavoriteRepository,
    storageRepo: StorageRepository,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: (Boolean) -> Unit
) {
    var currentTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
    var activeSubTool by remember { mutableStateOf<String?>(null) }
    var passedTextToTranslate by remember { mutableStateOf("") }
    var passedCategoryToFileManager by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var storageStats by remember {
        mutableStateOf(
            StorageStats(
                totalBytes = 64_000_000_000L,
                freeBytes = 32_000_000_000L,
                usedBytes = 32_000_000_000L,
                usedPercentage = 0.5f
            )
        )
    }

    val favorites by favRepo.allFavorites.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        storageStats = storageRepo.getStorageStats()
    }

    // System Back Press handling
    BackHandler(enabled = activeSubTool != null || currentTab != MainNavTab.DASHBOARD) {
        if (activeSubTool != null) {
            activeSubTool = null
        } else if (currentTab != MainNavTab.DASHBOARD) {
            currentTab = MainNavTab.DASHBOARD
        }
    }

    Scaffold(
        bottomBar = {
            if (activeSubTool == null || activeSubTool == "video_player") {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    MainNavTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab && activeSubTool == null,
                            onClick = {
                                activeSubTool = null
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.titleAr) },
                            label = { Text(tab.titleAr) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeSubTool ?: currentTab.id,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    // Main Tabs
                    "dashboard" -> DashboardScreen(
                        storageStats = storageStats,
                        favorites = favorites,
                        onNavigateToCategory = { cat ->
                            passedCategoryToFileManager = cat
                            currentTab = MainNavTab.FILES
                        },
                        onNavigateToTool = { toolId ->
                            activeSubTool = toolId
                        }
                    )
                    "tools" -> ToolsHubScreen(
                        onLaunchTool = { toolId ->
                            activeSubTool = toolId
                        }
                    )
                    "scanner" -> ScannerScreen(
                        scanRepository = scanRepo,
                        onNavigateBack = {
                            if (activeSubTool != null) activeSubTool = null else currentTab = MainNavTab.DASHBOARD
                        }
                    )
                    "files" -> FileManagerScreen(
                        trashRepository = trashRepo,
                        favoriteRepository = favRepo,
                        initialCategory = passedCategoryToFileManager,
                        onNavigateBack = {
                            passedCategoryToFileManager = null
                            currentTab = MainNavTab.DASHBOARD
                        }
                    )
                    "settings" -> SettingsScreen(
                        keepScreenAwakeGlobal = keepScreenAwake,
                        onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                        onNavigateBack = { currentTab = MainNavTab.DASHBOARD }
                    )

                    // Sub Tools
                    "ocr" -> OcrScreen(
                        onNavigateBack = { activeSubTool = null },
                        onSendToTranslate = { text ->
                            passedTextToTranslate = text
                            activeSubTool = "translator"
                        }
                    )
                    "translator" -> TranslatorScreen(
                        initialText = passedTextToTranslate,
                        onNavigateBack = {
                            passedTextToTranslate = ""
                            activeSubTool = null
                        }
                    )
                    "sharing" -> SharingScreen(
                        onNavigateBack = { activeSubTool = null }
                    )
                    "video_player" -> VideoPlayerScreen(
                        videoHistoryRepository = videoRepo,
                        onNavigateBack = { activeSubTool = null }
                    )
                    "media_converter" -> MediaConverterScreen(
                        onNavigateBack = { activeSubTool = null }
                    )
                    "filters" -> FiltersScreen(
                        onNavigateBack = { activeSubTool = null }
                    )
                    "format_converter" -> FormatConverterScreen(
                        onNavigateBack = { activeSubTool = null }
                    )
                    "apk_manager" -> ApkManagerScreen(
                        onNavigateBack = { activeSubTool = null }
                    )
                    "trash" -> TrashScreen(
                        trashRepository = trashRepo,
                        onNavigateBack = { activeSubTool = null }
                    )

                    else -> DashboardScreen(
                        storageStats = storageStats,
                        favorites = favorites,
                        onNavigateToCategory = { currentTab = MainNavTab.FILES },
                        onNavigateToTool = { toolId -> activeSubTool = toolId }
                    )
                }
            }
        }
    }
}