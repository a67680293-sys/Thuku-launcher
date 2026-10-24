package com.thuku.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    lateinit var widgetHost: AppWidgetHost
        private set
    val widgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(this) }
    val widgetRevision = mutableIntStateOf(0)
    val refreshSignal = mutableIntStateOf(0)
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val widgetPrefs by lazy { getSharedPreferences("thuku_widgets", MODE_PRIVATE) }

    private val configureWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID && result.resultCode == Activity.RESULT_OK) {
            saveWidgetId(id)
        } else if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { widgetHost.deleteAppWidgetId(id) }
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private val pickWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val returnedId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
            ?: pendingWidgetId
        if (result.resultCode != Activity.RESULT_OK || returnedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            return@registerForActivityResult
        }
        pendingWidgetId = returnedId
        val info = runCatching { widgetManager.getAppWidgetInfo(returnedId) }.getOrNull()
        if (info?.configure != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, returnedId)
            runCatching { configureWidget.launch(configureIntent) }.onFailure { saveWidgetId(returnedId) }
        } else {
            saveWidgetId(returnedId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHost = AppWidgetHost(this, 0x54484B)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { ThukuRoot(this) }
    }

    override fun onResume() {
        super.onResume()
        refreshSignal.intValue++
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        runCatching { widgetHost.stopListening() }
        super.onStop()
    }

    fun launchApp(app: LauncherApp) {
        try {
            val service = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
            service.startMainActivity(app.componentName, app.user, null as Rect?, null)
        } catch (_: Exception) {
            runCatching {
                packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
            }
        }
    }

    fun pickSystemWidget() {
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        runCatching { pickWidget.launch(pickIntent) }.onFailure {
            runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    fun getWidgetIds(): List<Int> = widgetPrefs.getString("ids", "").orEmpty()
        .split(',').mapNotNull { it.trim().toIntOrNull() }.distinct()

    private fun saveWidgetId(id: Int) {
        val ids = (getWidgetIds() + id).distinct()
        widgetPrefs.edit().putString("ids", ids.joinToString(",")).apply()
        widgetRevision.intValue++
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    fun deleteWidget(id: Int) {
        val ids = getWidgetIds().filterNot { it == id }
        widgetPrefs.edit().putString("ids", ids.joinToString(",")).apply()
        runCatching { widgetHost.deleteAppWidgetId(id) }
        widgetRevision.intValue++
    }

    fun setAsDefaultLauncher() {
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    fun openSystemPanel(action: String) {
        runCatching { startActivity(Intent(action)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    fun chooseWallpaper() {
        runCatching { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
            .onFailure { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
    }
}

@Composable
private fun ThukuRoot(activity: MainActivity) {
    val preferenceStore = remember { LauncherPreferences(activity) }
    var config by remember { mutableStateOf(preferenceStore.load()) }
    val onConfig: (LauncherConfig) -> Unit = { updated -> config = updated; preferenceStore.save(updated) }
    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var appsLoading by remember { mutableStateOf(true) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var appToManage by remember { mutableStateOf<LauncherApp?>(null) }
    var appToAssignFolder by remember { mutableStateOf<LauncherApp?>(null) }
    var folderToOpen by remember { mutableStateOf<String?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var showQuickPanel by remember { mutableStateOf(false) }
    val widgetRevision = activity.widgetRevision.intValue
    val refreshSignal = activity.refreshSignal.intValue

    BackHandler(enabled = screen != Screen.HOME || showQuickPanel) {
        if (showQuickPanel) showQuickPanel = false else screen = Screen.HOME
    }

    LaunchedEffect(refreshSignal) {
        appsLoading = true
        apps = withContext(Dispatchers.IO) { queryLauncherApps(activity) }
        appsLoading = false
    }

    val colorScheme = remember(config.accentArgb, config.useGlass) {
        val accent = Color(config.accentArgb)
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF171B12),
            secondary = Color(0xFFB4BDFB),
            onSecondary = Color(0xFF121625),
            background = Color(0xFF08090D),
            onBackground = Color(0xFFF3F5F0),
            surface = if (config.useGlass) Color(0xCC161A23) else Color(0xFF161A23),
            onSurface = Color(0xFFF3F5F0),
            surfaceVariant = Color(0xFF242A36),
            onSurfaceVariant = Color(0xFFBEC4D1),
            outline = Color(0xFF3A4353)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(Modifier.fillMaxSize()) {
            WallpaperBackdrop(config, refreshSignal)
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0x9907090D), Color(0x4D08090D), Color(0xC008090D))))
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
            ) {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        if (config.animateUi) {
                            (slideInVertically { it / 10 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 18 } + fadeOut())
                        } else fadeIn() togetherWith fadeOut()
                    },
                    label = "screen-transition"
                ) { current ->
                    when (current) {
                        Screen.HOME -> HomeScreen(
                            activity = activity, apps = apps, config = config, widgetRevision = widgetRevision,
                            onScreen = { screen = it }, onConfig = onConfig, onManageApp = { appToManage = it },
                            onOpenFolder = { folderToOpen = it }, onDeleteFolder = { folderToDelete = it },
                            onOpenQuickPanel = { showQuickPanel = true }
                        )
                        Screen.APPS -> AppDrawerScreen(
                            activity = activity, apps = apps, config = config, onScreen = { screen = it },
                            onManageApp = { appToManage = it }
                        )
                        Screen.FOCUS -> FocusScreen(activity = activity, config = config, apps = apps, onScreen = { screen = it })
                        Screen.TOOLS -> ToolsScreen(activity = activity, onScreen = { screen = it })
                        Screen.CUSTOMIZE -> CustomizeScreen(
                            config = config, onConfig = onConfig, apps = apps, onScreen = { screen = it },
                            onChooseWallpaper = { activity.chooseWallpaper() },
                            onMakeDefault = { activity.setAsDefaultLauncher() },
                            onOpenTools = { screen = Screen.TOOLS }
                        )
                    }
                }
                BottomDock(screen = screen, config = config, onScreen = { screen = it }, onQuickPanel = { showQuickPanel = true })
            }
            if (showQuickPanel) QuickPanel(onDismiss = { showQuickPanel = false }, onNavigate = { screen = it; showQuickPanel = false }, activity = activity)
            appToManage?.let { app ->
                AppManageDialog(
                    app = app,
                    isPinned = config.favourites.contains(app.key),
                    isHidden = config.hidden.contains(app.key),
                    onDismiss = { appToManage = null },
                    onFolder = { appToAssignFolder = app; appToManage = null },
                    onPinToggle = {
                        val next = if (it) config.favourites + app.key else config.favourites - app.key
                        onConfig(config.copy(favourites = next))
                        appToManage = null
                    },
                    onHideToggle = {
                        val next = if (it) config.hidden + app.key else config.hidden - app.key
                        onConfig(config.copy(hidden = next))
                        appToManage = null
                    }
                )
            }
            appToAssignFolder?.let { app ->
                FolderAssignmentDialog(
                    app = app,
                    existingFolders = config.folders.keys.sorted(),
                    onDismiss = { appToAssignFolder = null },
                    onSave = { folderName ->
                        val name = folderName.trim().take(32)
                        if (name.isNotBlank()) {
                            val nextFolders = config.folders.toMutableMap()
                            nextFolders[name] = nextFolders[name].orEmpty() + app.key
                            onConfig(config.copy(folders = nextFolders))
                        }
                        appToAssignFolder = null
                    }
                )
            }
            folderToOpen?.let { name ->
                val folderApps = apps.filter { it.key in config.folders[name].orEmpty() && it.key !in config.hidden }
                FolderContentsDialog(
                    name = name,
                    apps = folderApps,
                    config = config,
                    onDismiss = { folderToOpen = null },
                    onLaunch = { app -> folderToOpen = null; activity.launchApp(app) },
                    onRemoveApp = { app ->
                        val next = config.folders.toMutableMap()
                        val members = next[name].orEmpty() - app.key
                        if (members.isEmpty()) next.remove(name) else next[name] = members
                        onConfig(config.copy(folders = next))
                    },
                    onDeleteFolder = { folderToDelete = name; folderToOpen = null }
                )
            }
            folderToDelete?.let { name ->
                AlertDialog(
                    onDismissRequest = { folderToDelete = null },
                    title = { Text("Delete folder?") },
                    text = { Text("Remove the '$name' folder from Thuku Launcher? The apps themselves will stay installed.") },
                    confirmButton = { TextButton(onClick = { onConfig(config.copy(folders = config.folders - name)); folderToDelete = null }) { Text("Delete folder") } },
                    dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("Cancel") } },
                    containerColor = Color(0xFF171B24), titleContentColor = Color.White, textContentColor = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun queryLauncherApps(context: Context): List<LauncherApp> {
    val service = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
    val profiles = runCatching { service.profiles }.getOrDefault(listOf(Process.myUserHandle()))
    return profiles.flatMap { profile ->
        // If a work profile is paused or restricted, preserve apps from the other profiles.
        runCatching {
            service.getActivityList(null, profile).mapNotNull { entry ->
                runCatching {
                    LauncherApp(
                        label = entry.label?.toString()?.trim().orEmpty().ifBlank { entry.applicationInfo.loadLabel(context.packageManager).toString() },
                        packageName = entry.applicationInfo.packageName,
                        componentName = entry.componentName,
                        user = entry.user,
                        icon = entry.getBadgedIcon(0),
                        systemCategory = if (android.os.Build.VERSION.SDK_INT >= 26) entry.applicationInfo.category else ApplicationInfoCategoryFallback
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }.filter { it.packageName != context.packageName }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

private const val ApplicationInfoCategoryFallback = -1

@Composable
private fun WallpaperBackdrop(config: LauncherConfig, wallpaperRefreshKey: Int) {
    val context = LocalContext.current
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(config.useWallpaper, wallpaperRefreshKey) {
        wallpaperBitmap = if (!config.useWallpaper) null else withContext(Dispatchers.IO) {
            runCatching {
                val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
                drawableToBitmap(drawable, 1600)
            }.getOrNull()
        }
    }
    if (wallpaperBitmap != null) {
        Image(wallpaperBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = config.wallpaperDim)))
    } else {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0A0D13), Color(0xFF15162A), Color(0xFF071416)))))
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(config.accentArgb).copy(alpha = 0.15f), Color.Transparent)),
                radius = size.minDimension * 0.52f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF7A60ED).copy(alpha = 0.14f), Color.Transparent)),
                radius = size.minDimension * 0.68f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.78f)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    activity: MainActivity,
    apps: List<LauncherApp>,
    config: LauncherConfig,
    widgetRevision: Int,
    onScreen: (Screen) -> Unit,
    onConfig: (LauncherConfig) -> Unit,
    onManageApp: (LauncherApp) -> Unit,
    onOpenFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenQuickPanel: () -> Unit
) {
    val hiddenApps = config.hidden
    val visibleApps = apps.filterNot { it.key in hiddenApps }
    val favourites = visibleApps.filter { it.key in config.favourites }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); delay(30_000) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("THUKU / HOME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(icon = Icons.Filled.Notifications, label = "Quick panel", onClick = onOpenQuickPanel)
                RoundAction(icon = Icons.Filled.Tune, label = "Customize", onClick = { onScreen(Screen.CUSTOMIZE) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(activity)) "HH:mm" else "h:mm", Locale.getDefault()).format(now),
                fontSize = when { config.compactHome -> 52.sp; config.clockStyle == "Minimal" -> 58.sp; else -> 66.sp },
                lineHeight = 66.sp,
                fontWeight = if (config.clockStyle == "Mono") FontWeight.Medium else FontWeight.Light,
                fontFamily = if (config.clockStyle == "Mono") FontFamily.Monospace else FontFamily.SansSerif,
                color = Color.White,
                letterSpacing = if (config.clockStyle == "Mono") 0.sp else (-3).sp
            )
            if (config.showDate) Text(SimpleDateFormat("EEEE  ·  MMMM d", Locale.getDefault()).format(now), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
        }

        SearchLauncherBar(onClick = { onScreen(Screen.APPS) }, hint = "Search apps, fast actions…")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniInfoCard(Modifier.weight(1f), eyebrow = "PROFILE", title = "Your space", subtitle = "${favourites.size} pinned apps", icon = Icons.Filled.Home, accent = MaterialTheme.colorScheme.primary)
            MiniInfoCard(Modifier.weight(1f), eyebrow = "FOCUS MODE", title = "Reset your mind", subtitle = "Start a focus session", icon = Icons.Filled.Timer, accent = Color(0xFFB8ACFF), onClick = { onScreen(Screen.FOCUS) })
        }

        if (config.folders.isNotEmpty()) {
            SectionLabel("FOLDERS", "Tap to open · long-press to remove")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                config.folders.toSortedMap().forEach { (name, keys) ->
                    FolderTile(name = name, count = keys.count { key -> apps.any { it.key == key } && key !in config.hidden }, config = config,
                        onClick = { onOpenFolder(name) }, onLongClick = { onDeleteFolder(name) })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("YOUR APPS", "Hold any app for actions")
            TextButton(onClick = { onScreen(Screen.APPS) }) { Text("All apps", color = MaterialTheme.colorScheme.primary) }
        }
        if (favourites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = config.surfaceColor()),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Text("Build your quick-launch row", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Pin your daily apps from the app drawer. Long-press an app to pin it, hide it, or manage it.", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onScreen(Screen.APPS) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose apps")
                    }
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favourites.take(12).forEach { app ->
                    AppTile(app = app, config = config, modifier = Modifier.weight(1f).widthIn(min = 58.dp, max = 100.dp), onClick = { activity.launchApp(app) }, onLongClick = { onManageApp(app) })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("WIDGET STUDIO", "Real Android widgets")
            FilledTonalButton(onClick = { activity.pickSystemWidget() }, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
            }
        }
        val widgetIds = remember(widgetRevision) { activity.getWidgetIds() }
        if (widgetIds.isEmpty()) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor()), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF9BB9FF).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Apps, contentDescription = null, tint = Color(0xFF9BB9FF), modifier = Modifier.size(25.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Add a live widget", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Clock, weather, calendar, music and compatible widgets from your installed apps.", color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.65f))
                }
            }
        } else {
            widgetIds.forEach { id ->
                SystemWidgetCard(activity = activity, widgetId = id, height = if (config.compactHome) 142.dp else 190.dp)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AppDrawerScreen(
    activity: MainActivity,
    apps: List<LauncherApp>,
    config: LauncherConfig,
    onScreen: (Screen) -> Unit,
    onManageApp: (LauncherApp) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    var sortPinnedFirst by remember { mutableStateOf(false) }
    val visibleApps = apps.filter { app ->
            val hiddenMode = filter == AppFilter.HIDDEN
            val passesHidden = if (hiddenMode) app.key in config.hidden else app.key !in config.hidden
            if (!passesHidden) return@filter false
            val matchesSearch = app.label.contains(search.trim(), ignoreCase = true) || app.packageName.contains(search.trim(), ignoreCase = true)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.FAVORITES -> app.key in config.favourites
                AppFilter.HIDDEN -> true
                else -> classifyApp(app) == filter
            }
            matchesSearch && matchesFilter
        }
        .let { if (sortPinnedFirst) it.sortedByDescending { app -> config.favourites.contains(app.key) } else it }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("APP LIBRARY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Everything, organized.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            RoundAction(icon = Icons.Filled.Tune, label = "Customize", onClick = { onScreen(Screen.CUSTOMIZE) })
        }
        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search apps or package names") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = RoundedCornerShape(19.dp)
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilter.values().forEach { option ->
                val selected = option == filter
                FilterPill(option.title, selected = selected, onClick = { filter = option })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${visibleApps.size} apps", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { sortPinnedFirst = !sortPinnedFirst }) { Text(if (sortPinnedFirst) "Pinned first ✓" else "Pinned first") }
        }
        if (visibleApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Text("Nothing found", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Try another name or category.", color = Color.White.copy(alpha = 0.62f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(config.columns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleApps, key = { it.key }) { app ->
                    AppTile(app, config, modifier = Modifier.fillMaxWidth(), onClick = { activity.launchApp(app) }, onLongClick = { onManageApp(app) })
                }
            }
        }
    }
}

@Composable
private fun FocusScreen(activity: MainActivity, config: LauncherConfig, apps: List<LauncherApp>, onScreen: (Screen) -> Unit) {
    var durationMinutes by remember { mutableIntStateOf(25) }
    var remaining by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var sessionDone by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        while (running && remaining > 0) { delay(1000); remaining-- }
        if (running && remaining == 0) { running = false; sessionDone = true }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("FOCUS MODE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("A little space to reset.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            RoundAction(Icons.Filled.Home, "Home", onClick = { onScreen(Screen.HOME) })
        }
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor()), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(205.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color.White.copy(alpha = 0.1f), style = Stroke(width = 10.dp.toPx()))
                        val fraction = if (durationMinutes * 60 == 0) 0f else remaining.toFloat() / (durationMinutes * 60f)
                        drawArc(Color(config.accentArgb), -90f, 360f * fraction, false, style = Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.getDefault(), "%02d:%02d", remaining / 60, remaining % 60), fontSize = 38.sp, color = Color.White, fontWeight = FontWeight.Light)
                        Text(if (sessionDone) "SESSION COMPLETE" else if (running) "STAY IN THE FLOW" else "READY WHEN YOU ARE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { minutes ->
                        FilterPill("${minutes}m", selected = durationMinutes == minutes, onClick = {
                            durationMinutes = minutes; remaining = minutes * 60; running = false; sessionDone = false
                        })
                    }
                }
                Button(onClick = {
                    if (running) running = false else {
                        if (remaining <= 0 || sessionDone) remaining = durationMinutes * 60
                        sessionDone = false; running = true
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                    Icon(if (running) Icons.Filled.Close else Icons.Filled.Timer, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text(if (running) "Pause session" else if (sessionDone) "Start again" else "Start focus")
                }
                Text("This timer helps you focus, but it doesn't block or restrict other apps.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionLabel("QUICK ACCESS", "Your pinned apps")
        val pinnedApps = apps.filter { it.key in config.favourites && it.key !in config.hidden }.take(8)
        if (pinnedApps.isEmpty()) {
            Text("Pin apps from the App Library to make them appear here.", color = Color.White.copy(alpha = 0.65f))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pinnedApps.forEach { app ->
                    MiniAppChip(app, config, onClick = { activity.launchApp(app) })
                }
            }
        }
    }
}

@Composable
private fun ToolsScreen(activity: MainActivity, onScreen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("CONTROL CENTER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Quick actions", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            RoundAction(Icons.Filled.Home, "Home", onClick = { onScreen(Screen.HOME) })
        }
        Text("These shortcuts open Android's own settings panels when your phone supports them.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickToolCard(Modifier.weight(1f), Icons.Filled.Wifi, "Internet", "Wi-Fi & data", Color(0xFF9BB9FF)) { activity.openSystemPanel("android.settings.panel.action.INTERNET_CONNECTIVITY") }
            QuickToolCard(Modifier.weight(1f), Icons.Filled.Bluetooth, "Bluetooth", "Connected devices", Color(0xFF8CEBFF)) { activity.openSystemPanel(Settings.ACTION_BLUETOOTH_SETTINGS) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickToolCard(Modifier.weight(1f), Icons.Filled.DisplaySettings, "Display", "Brightness & size", Color(0xFFC3A4FF)) { activity.openSystemPanel("android.settings.panel.action.DISPLAY") }
            QuickToolCard(Modifier.weight(1f), Icons.Filled.Speaker, "Sound", "Volume & audio", Color(0xFFFF9FC9)) { activity.openSystemPanel("android.settings.panel.action.VOLUME") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickToolCard(Modifier.weight(1f), Icons.Filled.Wallpaper, "Wallpaper", "Change the look", Color(0xFFFFC16A)) { activity.chooseWallpaper() }
            QuickToolCard(Modifier.weight(1f), Icons.Filled.BatteryFull, "Battery", "Power settings", Color(0xFF8CFFD0)) { activity.openSystemPanel(Settings.ACTION_BATTERY_SAVER_SETTINGS) }
        }
        QuickToolCard(Modifier.fillMaxWidth(), Icons.Filled.Settings, "System settings", "Open Android Settings", MaterialTheme.colorScheme.primary) { activity.openSystemPanel(Settings.ACTION_SETTINGS) }
    }
}

@Composable
private fun CustomizeScreen(
    config: LauncherConfig,
    onConfig: (LauncherConfig) -> Unit,
    apps: List<LauncherApp>,
    onScreen: (Screen) -> Unit,
    onChooseWallpaper: () -> Unit,
    onMakeDefault: () -> Unit,
    onOpenTools: () -> Unit
) {
    val context = LocalContext.current
    val iconPacks = remember { discoverIconPacks(context) }
    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backupConfig(config)) } }
    }
    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not read the selected file.")
            onConfig(restoreConfig(text, config))
        }.onFailure { android.widget.Toast.makeText(context, it.message ?: "Could not restore backup", android.widget.Toast.LENGTH_LONG).show() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PIXEL LAB / PLUS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Make it yours.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            RoundAction(Icons.Filled.Home, "Home", onClick = { onScreen(Screen.HOME) })
        }
        SectionLabel("COLOR SYSTEM", "Choose an accent")
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor()), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(Color(config.accentArgb)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Palette, null, tint = Color(0xFF13160D), modifier = Modifier.size(25.dp))
                    }
                    Column {
                        Text(accentPresets.firstOrNull { it.argb == config.accentArgb }?.name ?: "Custom accent", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Themes update buttons, focus ring and highlights.", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentPresets.forEach { preset ->
                        Box(Modifier.size(38.dp).clip(CircleShape).background(preset.color).then(if (config.accentArgb == preset.argb) Modifier.border(3.dp, Color.White, CircleShape) else Modifier).combinedClickable(onClick = { onConfig(config.copy(accentArgb = preset.argb)) }, onLongClick = { onConfig(config.copy(accentArgb = preset.argb)) }), contentAlignment = Alignment.Center) {
                            if (config.accentArgb == preset.argb) Icon(Icons.Filled.Check, null, tint = Color(0xFF10130B), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        SectionLabel("CLOCK STUDIO", "Shape the first thing you see")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Editorial", "Minimal", "Mono").forEach { style ->
                val selected = config.clockStyle == style
                Surface(modifier = Modifier.weight(1f), onClick = { onConfig(config.copy(clockStyle = style)) }, shape = RoundedCornerShape(15.dp), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.17f) else config.surfaceColor(), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f))) {
                    Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("12:45", color = if (selected) MaterialTheme.colorScheme.primary else Color.White, fontFamily = if (style == "Mono") FontFamily.Monospace else FontFamily.SansSerif, fontWeight = if (style == "Mono") FontWeight.Medium else FontWeight.Light, fontSize = if (style == "Minimal") 18.sp else 21.sp)
                        Text(style, color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        SectionLabel("HOME LAYOUT", "Built to adapt")
        SettingsCard(config) {
            SettingsSliderRow(title = "App icon scale", valueText = "${(config.iconScale * 100).toInt()}%", value = config.iconScale, range = 0.7f..1.35f, onValue = { onConfig(config.copy(iconScale = it)) })
            SettingsSliderRow(title = "App drawer columns", valueText = "${config.columns}", value = config.columns.toFloat(), range = 3f..6f, steps = 2, onValue = { onConfig(config.copy(columns = it.toInt().coerceIn(3, 6))) })
            SettingsSwitchRow("App labels", "Show names below icons", config.showLabels) { onConfig(config.copy(showLabels = it)) }
            SettingsSwitchRow("Glass surfaces", "Translucent cards and soft borders", config.useGlass) { onConfig(config.copy(useGlass = it)) }
            SettingsSwitchRow("Compact home", "Tighter clock and widget spacing", config.compactHome) { onConfig(config.copy(compactHome = it)) }
            SettingsSwitchRow("Animated transitions", "Subtle screen motion and fades", config.animateUi) { onConfig(config.copy(animateUi = it)) }
            SettingsSwitchRow("Show date", "Display date under the clock", config.showDate) { onConfig(config.copy(showDate = it)) }
        }

        SectionLabel("WALLPAPER STUDIO", "Keep the wallpaper visible")
        SettingsCard(config) {
            SettingsSwitchRow("Use system wallpaper", "Use your current phone wallpaper behind the launcher", config.useWallpaper) { onConfig(config.copy(useWallpaper = it)) }
            SettingsSliderRow(title = "Wallpaper dim", valueText = "${(config.wallpaperDim * 100).toInt()}%", value = config.wallpaperDim, range = 0f..0.8f, onValue = { onConfig(config.copy(wallpaperDim = it)) })
            FilledTonalButton(onClick = onChooseWallpaper, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Filled.Wallpaper, null); Spacer(Modifier.width(8.dp)); Text("Change device wallpaper")
            }
        }

        SectionLabel("ICON SHAPE", "A small detail, a big difference")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Circle", "Squircle", "Rounded", "Square").forEach { shape ->
                val selected = config.iconShape == shape
                Surface(modifier = Modifier.weight(1f).clip(RoundedCornerShape(15.dp)).combinedClickable(onClick = { onConfig(config.copy(iconShape = shape)) }, onLongClick = { onConfig(config.copy(iconShape = shape)) }), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.17f) else config.surfaceColor(), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(15.dp)) {
                    Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(27.dp).clip(iconShape(shape)).background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.68f)))
                        Text(shape, color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        SectionLabel("ICON PACKS", "Installed packs that use common launcher standards")
        SettingsCard(config) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("System default", selected = config.iconPackPackage.isBlank(), onClick = { onConfig(config.copy(iconPackPackage = "")) })
                iconPacks.forEach { pack ->
                    FilterPill(pack.label, selected = config.iconPackPackage == pack.packageName, onClick = { onConfig(config.copy(iconPackPackage = pack.packageName)) })
                }
            }
            if (iconPacks.isEmpty()) {
                Text("No compatible icon packs found. Install an icon pack that supports common launcher integration, then return here.", color = Color.White.copy(alpha = 0.59f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
            } else {
                Text("Icons missing from a selected pack automatically fall back to their original app icon.", color = Color.White.copy(alpha = 0.59f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
            }
        }

        SectionLabel("SMART SHORTCUTS", "One tap to Android tools")
        SettingsCard(config) {
            SettingsNavigationRow(Icons.Filled.Home, "Set as default launcher", "Choose Thuku as your Home app", onMakeDefault)
            SettingsNavigationRow(Icons.Filled.Wifi, "Quick tools", "Internet, display, sound and more", onOpenTools)
            SettingsNavigationRow(Icons.Filled.Apps, "Hidden apps", "${config.hidden.size} hidden in this launcher", { onScreen(Screen.APPS) })
            SettingsNavigationRow(Icons.Filled.Lock, "Privacy note", "This build has no internet permission and stores settings locally", { android.widget.Toast.makeText(context, "Thuku Launcher does not request internet access in this build. Android widgets may have their own permissions and network behavior.", android.widget.Toast.LENGTH_LONG).show() })
        }

        SectionLabel("BACKUP & RESTORE", "Take your setup with you")
        SettingsCard(config) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { backupLauncher.launch("thuku-launcher-backup.json") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Export setup") }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Import setup") }
            }
            Text("The backup includes appearance settings, selected icon pack, pins, hidden apps and folders. System widgets stay on this phone and are not included.", color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.bodySmall)
        }

        SectionLabel("ABOUT THUKU", "Version 0.1.0")
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor())) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("THUKU LAUNCHER", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("A privacy-first, deeply customizable home screen. Designed for phones first and responsive layouts beyond them.", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                Text("Some system controls, widgets and gesture actions vary by Android version and device manufacturer.", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCard(config: LauncherConfig, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor()), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), content = content)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SettingsSliderRow(title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onValue: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(valueText, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onValue, valueRange = range, steps = steps)
    }
}

@Composable
private fun SettingsNavigationRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).combinedClickable(onClick = onClick, onLongClick = onClick).padding(vertical = 13.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(alpha = 0.54f), style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(alpha = 0.45f))
    }
}

@Composable
private fun SystemWidgetCard(activity: MainActivity, widgetId: Int, height: Dp) {
    val info = remember(widgetId) { runCatching { activity.widgetManager.getAppWidgetInfo(widgetId) }.getOrNull() }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xAA171A22)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(info?.loadLabel(activity.packageManager)?.toString() ?: "System widget", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = { activity.deleteWidget(widgetId) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.Close, "Remove widget", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(17.dp)) }
            }
            if (info != null) {
                AndroidView(
                    factory = { context ->
                        activity.widgetHost.createView(context, widgetId, info).apply {
                            setAppWidget(widgetId, info)
                            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(height)
                )
            } else {
                Text("This widget is no longer available. Remove it and add it again.", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, config: LauncherConfig, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    val scaled by animateFloatAsState(if (config.animateUi) config.iconScale else 1f, animationSpec = spring(stiffness = 480f), label = "icon-scale")
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AppIcon(app, config.iconShape, 52.dp * scaled, config.iconPackPackage)
        if (config.showLabels) Text(app.label, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AppIcon(app: LauncherApp, shape: String, size: Dp, iconPackPackage: String = "") {
    val context = LocalContext.current
    val drawable = remember(app.key, iconPackPackage) {
        if (iconPackPackage.isBlank()) app.icon else loadIconPackDrawable(context, iconPackPackage, app) ?: app.icon
    }
    val bitmap = remember(app.key, iconPackPackage) { drawableToBitmap(drawable) }
    Image(bitmap.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(size).clip(iconShape(shape)), contentScale = ContentScale.Fit)
}

private fun iconShape(value: String): androidx.compose.ui.graphics.Shape = when (value) {
    "Circle" -> CircleShape
    "Square" -> RoundedCornerShape(3.dp)
    "Rounded" -> RoundedCornerShape(11.dp)
    else -> RoundedCornerShape(15.dp)
}

@Composable
private fun MiniAppChip(app: LauncherApp, config: LauncherConfig, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(15.dp), color = config.surfaceColor(), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(app, config.iconShape, 24.dp, config.iconPackPackage)
            Text(app.label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppManageDialog(app: LauncherApp, isPinned: Boolean, isHidden: Boolean, onDismiss: () -> Unit, onFolder: () -> Unit, onPinToggle: (Boolean) -> Unit, onHideToggle: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Apps, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(app.label) },
        text = { Text("Choose what Thuku Launcher should do with this app. This only changes how it appears in this launcher; it does not uninstall or disable the app.") },
        confirmButton = {
            TextButton(onClick = { onPinToggle(!isPinned) }) { Text(if (isPinned) "Unpin from home" else "Pin to home") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onFolder) { Text("Folder") }
                TextButton(onClick = { onHideToggle(!isHidden) }) { Text(if (isHidden) "Unhide" else "Hide") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        containerColor = Color(0xFF171B24),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
private fun SearchLauncherBar(onClick: () -> Unit, hint: String) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color(0xB8181C25), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary)
            Text(hint, color = Color.White.copy(alpha = 0.64f), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("↵", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun MiniInfoCard(modifier: Modifier, eyebrow: String, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onClick) else Modifier
    Card(modifier = modifier.then(clickModifier), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xA91A1E27)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(eyebrow, color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall, maxLines = 2, minLines = 2)
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.3.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FilterPill(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(999.dp), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color(0x66191D27), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f))) {
        Text(title, color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
    }
}

@Composable
private fun BottomDock(screen: Screen, config: LauncherConfig, onScreen: (Screen) -> Unit, onQuickPanel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp))
            .background(if (config.useGlass) Color(0xCC151922) else Color(0xFF151922))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem(Icons.Filled.Home, "Home", screen == Screen.HOME, config, onClick = { onScreen(Screen.HOME) })
        DockItem(Icons.Filled.Apps, "Apps", screen == Screen.APPS, config, onClick = { onScreen(Screen.APPS) })
        DockItem(Icons.Filled.Timer, "Focus", screen == Screen.FOCUS, config, onClick = { onScreen(Screen.FOCUS) })
        DockItem(Icons.Filled.Tune, "Lab", screen == Screen.CUSTOMIZE, config, onClick = { onScreen(Screen.CUSTOMIZE) })
        DockItem(Icons.Filled.MoreHoriz, "Tools", screen == Screen.TOOLS, config, onClick = { onQuickPanel() })
    }
}

@Composable
private fun DockItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, config: LauncherConfig, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(17.dp)).combinedClickable(onClick = onClick, onLongClick = onClick).background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(21.dp))
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color(0xAA171B24), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(19.dp)) }
    }
}

@Composable
private fun QuickToolCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onClick), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color(0xAD171B24)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickPanel(onDismiss: () -> Unit, onNavigate: (Screen) -> Unit, activity: MainActivity) {
    Box(Modifier.fillMaxSize().background(Color(0xB8000000)), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFF11151D)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("THUKU CONTROL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Your shortcuts, one swipe away", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                RoundAction(Icons.Filled.Close, "Close", onDismiss)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Modifier.weight(1f), Icons.Filled.Wifi, "Internet") { activity.openSystemPanel("android.settings.panel.action.INTERNET_CONNECTIVITY") }
                QuickTile(Modifier.weight(1f), Icons.Filled.Bluetooth, "Bluetooth") { activity.openSystemPanel(Settings.ACTION_BLUETOOTH_SETTINGS) }
                QuickTile(Modifier.weight(1f), Icons.Filled.DisplaySettings, "Display") { activity.openSystemPanel(Settings.ACTION_DISPLAY_SETTINGS) }
                QuickTile(Modifier.weight(1f), Icons.Filled.Speaker, "Sound") { activity.openSystemPanel("android.settings.panel.action.VOLUME") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { onNavigate(Screen.APPS) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Apps, null); Spacer(Modifier.width(6.dp)); Text("Apps") }
                FilledTonalButton(onClick = { onNavigate(Screen.CUSTOMIZE) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Palette, null); Spacer(Modifier.width(6.dp)); Text("Pixel Lab") }
                FilledTonalButton(onClick = { onNavigate(Screen.TOOLS) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(6.dp)); Text("Tools") }
            }
            Text("Android controls open system panels where supported; exact options depend on your device.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickTile(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(modifier = modifier, onClick = onClick, shape = RoundedCornerShape(17.dp), color = Color(0xFF222835), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FolderTile(
    name: String,
    count: Int,
    config: LauncherConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(152.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = config.surfaceColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count app${if (count == 1) "" else "s"}", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FolderAssignmentDialog(
    app: LauncherApp,
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var folderName by remember(app.key) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Organize ${app.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a folder or type a new name. An app can be in more than one folder.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Folder name") },
                    placeholder = { Text("e.g. Daily, Media, School") }
                )
                if (existingFolders.isNotEmpty()) {
                    Text("EXISTING FOLDERS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        existingFolders.forEach { existing ->
                            FilterPill(existing, selected = folderName.equals(existing, ignoreCase = true), onClick = { folderName = existing })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (folderName.isNotBlank()) onSave(folderName) }) { Text("Save folder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color(0xFF171B24), titleContentColor = Color.White, textContentColor = Color.White.copy(alpha = 0.72f)
    )
}

@Composable
private fun FolderContentsDialog(
    name: String,
    apps: List<LauncherApp>,
    config: LauncherConfig,
    onDismiss: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onRemoveApp: (LauncherApp) -> Unit,
    onDeleteFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(name) },
        text = {
            if (apps.isEmpty()) {
                Text("This folder is empty. Long-press an app in the App Library and choose Folder to add it.")
            } else {
                Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    apps.forEach { app ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).combinedClickable(onClick = { onLaunch(app) }, onLongClick = { onRemoveApp(app) }).padding(horizontal = 5.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppIcon(app, config.iconShape, 34.dp, config.iconPackPackage)
                            Text(app.label, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onRemoveApp(app) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Close, "Remove from folder", tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDeleteFolder) { Text("Delete folder") } },
        containerColor = Color(0xFF171B24), titleContentColor = Color.White, textContentColor = Color.White.copy(alpha = 0.7f)
    )
}
