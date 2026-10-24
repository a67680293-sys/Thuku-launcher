@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.thuku.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.dynamicDarkColorScheme
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    lateinit var widgetHost: AppWidgetHost
        private set
    val widgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(this) }
    val widgetRevision = mutableIntStateOf(0)
    val refreshSignal = mutableIntStateOf(0)
    val homeIntentSignal = mutableIntStateOf(0)
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
        CrashDiagnostics.install(applicationContext)
        widgetHost = AppWidgetHost(this, 0x54484B)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { ThukuRoot(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeIntentSignal.intValue++
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSignal.intValue++
    }

    internal fun isDefaultLauncher(): Boolean = runCatching {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
    }.getOrDefault(false)

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        runCatching { widgetHost.stopListening() }
        super.onStop()
    }

    internal fun launchApp(app: LauncherApp) {
        val primaryResult = runCatching {
            val service = getSystemService(LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: error("Android launcher service is unavailable")
            service.startMainActivity(app.componentName, app.user, null as Rect?, null)
        }
        if (primaryResult.isSuccess) {
            AppUsageStore(this).record(app.key)
            return
        }

        val fallbackIntent = runCatching { packageManager.getLaunchIntentForPackage(app.packageName) }.getOrNull()
        val fallbackResult = fallbackIntent?.let { intent -> runCatching { startActivity(intent) } }
        if (fallbackResult?.isSuccess == true) {
            AppUsageStore(this).record(app.key)
            return
        }

        CrashDiagnostics.record(this, "Unable to launch app '${app.label}' (${app.packageName})", primaryResult.exceptionOrNull())
        android.widget.Toast.makeText(this, "Couldn't open ${app.label}. It may have been updated or disabled.", android.widget.Toast.LENGTH_LONG).show()
    }

    fun pickSystemWidget() {
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        runCatching { pickWidget.launch(pickIntent) }.onFailure { error ->
            runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            CrashDiagnostics.record(this, "Unable to open the system widget picker", error)
            android.widget.Toast.makeText(this, "The widget picker isn't available on this device.", android.widget.Toast.LENGTH_LONG).show()
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
        val result = runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        if (result.isFailure) {
            val fallback = runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            if (fallback.isFailure) {
                CrashDiagnostics.record(this, "Unable to open default Home settings", result.exceptionOrNull())
                android.widget.Toast.makeText(this, "Open Android Settings and search for Default apps → Home app.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openSystemPanel(action: String) {
        val result = runCatching { startActivity(Intent(action)) }
        if (result.isFailure) {
            val fallback = runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            if (fallback.isFailure) {
                CrashDiagnostics.record(this, "Unable to open system panel '$action'", result.exceptionOrNull())
                android.widget.Toast.makeText(this, "That settings panel isn't supported on this device.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun chooseWallpaper() {
        val result = runCatching { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
        if (result.isFailure) {
            val fallback = runCatching { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            if (fallback.isFailure) {
                CrashDiagnostics.record(this, "Unable to open wallpaper settings", result.exceptionOrNull())
                android.widget.Toast.makeText(this, "Couldn't open wallpaper settings on this device.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

}

@Composable
private fun ThukuRoot(activity: MainActivity) {
    val preferenceStore = remember { LauncherPreferences(activity) }
    var config by remember { mutableStateOf(preferenceStore.load()) }
    val onConfig: (LauncherConfig) -> Unit = { updated -> config = updated; preferenceStore.save(updated) }
    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var appsLoading by remember { mutableStateOf(true) }
    var appsLoadError by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var appToManage by remember { mutableStateOf<LauncherApp?>(null) }
    var appToAssignFolder by remember { mutableStateOf<LauncherApp?>(null) }
    var folderToOpen by remember { mutableStateOf<String?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var showQuickPanel by remember { mutableStateOf(false) }
    val widgetRevision = activity.widgetRevision.intValue
    val refreshSignal = activity.refreshSignal.intValue

    val homeIntentSignal = activity.homeIntentSignal.intValue
    LaunchedEffect(homeIntentSignal) {
        screen = Screen.HOME
        showQuickPanel = false
    }
    BackHandler(enabled = true) {
        when {
            showQuickPanel -> showQuickPanel = false
            screen != Screen.HOME -> screen = Screen.HOME
            else -> Unit // Back on the selected Home app should not return the user to a random previous app.
        }
    }

    LaunchedEffect(refreshSignal) {
        appsLoading = true
        appsLoadError = null
        try {
            apps = withContext(Dispatchers.IO) { queryLauncherApps(activity) }
            if (apps.isEmpty()) appsLoadError = "Android didn't return any launchable apps. Try refreshing or check whether a work profile is paused."
            else preferenceStore.seedWorkspaceIfNeeded(apps)?.let { config = it }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            apps = emptyList()
            appsLoadError = "Thuku couldn't read the app list. Refresh to try again."
            CrashDiagnostics.record(activity, "Installed-app discovery failed", error)
        } finally {
            appsLoading = false
        }
    }

    val colorScheme = remember(config.accentArgb, config.useGlass, config.useDynamicColor) {
        if (config.useDynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(activity)
        } else {
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
                            onScreen = { screen = it }, onConfig = onConfig,
                            onManageApp = { app ->
                                if (config.workspaceLocked) android.widget.Toast.makeText(activity, "Workspace is locked. Turn off Workspace lock in Customize to edit icons.", android.widget.Toast.LENGTH_SHORT).show()
                                else appToManage = app
                            },
                            onOpenFolder = { folderToOpen = it }, onDeleteFolder = { folderToDelete = it },
                            onOpenQuickPanel = { showQuickPanel = true }
                        )
                        Screen.APPS -> AppDrawerScreen(
                            activity = activity, apps = apps, config = config,
                            appsLoading = appsLoading, appsLoadError = appsLoadError,
                            onScreen = { screen = it }, onConfig = onConfig,
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
                if (screen == Screen.HOME) {
                    WorkspaceDock(
                        apps = apps,
                        config = config,
                        onLaunch = { activity.launchApp(it) },
                        onManageApp = { appToManage = it },
                        onOpenDrawer = { screen = Screen.APPS },
                        onCustomize = { screen = Screen.CUSTOMIZE }
                    )
                } else {
                    BottomDock(screen = screen, config = config, onScreen = { screen = it }, onQuickPanel = { showQuickPanel = true })
                }
            }
            if (showQuickPanel) QuickPanel(onDismiss = { showQuickPanel = false }, onNavigate = { screen = it; showQuickPanel = false }, activity = activity)
            appToManage?.let { app ->
                AppManageDialog(
                    app = app,
                    isPinned = app.key in config.homeOrder || config.favourites.contains(app.key),
                    isDocked = app.key in config.dockApps,
                    isHidden = config.hidden.contains(app.key),
                    displayLabel = config.customLabels[app.key].orEmpty(),
                    onRename = { raw ->
                        val safe = raw.filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(40)
                        val labels = config.customLabels.toMutableMap()
                        if (safe.isBlank() || safe == app.label) labels.remove(app.key) else labels[app.key] = safe
                        onConfig(config.copy(customLabels = labels))
                    },
                    onDismiss = { appToManage = null },
                    onFolder = { appToAssignFolder = app; appToManage = null },
                    onPinToggle = {
                        val nextFavourites = if (it) config.favourites + app.key else config.favourites - app.key
                        val currentOrder = config.homeOrder.ifEmpty { config.favourites.toList() }
                        val nextOrder = if (it) (currentOrder.filterNot { key -> key == app.key } + app.key) else currentOrder.filterNot { key -> key == app.key }
                        val nextDock = if (it) config.dockApps else config.dockApps.filterNot { key -> key == app.key }
                        onConfig(config.copy(favourites = nextFavourites, homeOrder = nextOrder, dockApps = nextDock))
                        appToManage = null
                    },
                    onDockToggle = { docked ->
                        if (!docked && config.dockApps.size >= config.dockSlots) {
                            android.widget.Toast.makeText(activity, "The dock holds up to ${config.dockSlots} apps. Change dock capacity in Customize or remove one first.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val nextDock = if (docked) config.dockApps.filterNot { it == app.key } else (config.dockApps + app.key).distinct().take(config.dockSlots)
                            onConfig(config.copy(dockApps = nextDock))
                            appToManage = null
                        }
                    },
                    onMoveEarlier = {
                        val order = (config.homeOrder.ifEmpty { config.favourites.toList() }).toMutableList()
                        val index = order.indexOf(app.key)
                        if (index > 0) {
                            val previous = order[index - 1]
                            order[index - 1] = order[index]
                            order[index] = previous
                            onConfig(config.copy(homeOrder = order))
                        }
                        appToManage = null
                    },
                    onMoveLater = {
                        val order = (config.homeOrder.ifEmpty { config.favourites.toList() }).toMutableList()
                        val index = order.indexOf(app.key)
                        if (index >= 0 && index < order.lastIndex) {
                            val next = order[index + 1]
                            order[index + 1] = order[index]
                            order[index] = next
                            onConfig(config.copy(homeOrder = order))
                        }
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
                        val name = LauncherValidation.folderName(folderName)
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
    LaunchedEffect(config.useWallpaper, config.customWallpaperUri, wallpaperRefreshKey) {
        wallpaperBitmap = if (!config.useWallpaper) null else withContext(Dispatchers.IO) {
            runCatching {
                val uri = config.customWallpaperUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                val customBitmap = if (uri != null) decodeScaledWallpaper(context, uri) else null
                if (customBitmap != null) customBitmap else {
                    val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
                    drawableToBitmap(drawable, 1600)
                }
            }.getOrNull()
        }
    }
    val currentWallpaperBitmap = wallpaperBitmap
    if (currentWallpaperBitmap != null) {
        Image(currentWallpaperBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
    val visibleApps = apps.filterNot { it.key in config.hidden }
    val orderedKeys = remember(config.homeOrder, config.favourites) {
        (config.homeOrder.ifEmpty { config.favourites.toList() }).distinct()
    }
    val homeApps = remember(apps, orderedKeys, config.hidden) {
        val lookup = apps.associateBy { it.key }
        orderedKeys.mapNotNull { lookup[it] }.filterNot { it.key in config.hidden }
    }
    val pageSize = config.columns.coerceIn(3, 6) * config.homeRows.coerceIn(2, 5)
    val pageCount = maxOf(1, (homeApps.size + pageSize - 1) / pageSize)
    var activePage by remember { mutableIntStateOf(0) }
    LaunchedEffect(pageCount) { if (activePage >= pageCount) activePage = pageCount - 1 }
    val pageApps = homeApps.drop(activePage * pageSize).take(pageSize)
    val customWidgetStore = remember(activity) { CustomWidgetStore(activity) }
    var customWidgets by remember(activity) { mutableStateOf(customWidgetStore.load()) }
    var showCustomWidgetPicker by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (config.compactHome) 8.dp else 11.dp)
    ) {
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now).uppercase(Locale.getDefault()), color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.15.sp)
                Text(
                    SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(activity)) "HH:mm" else "h:mm", Locale.getDefault()).format(now),
                    fontSize = if (config.compactHome) 48.sp else 58.sp,
                    lineHeight = if (config.compactHome) 52.sp else 60.sp,
                    fontWeight = if (config.clockStyle == "Mono") FontWeight.Medium else FontWeight.Light,
                    fontFamily = if (config.clockStyle == "Mono") FontFamily.Monospace else FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = if (config.clockStyle == "Mono") 0.sp else (-2.5).sp
                )
                if (config.showDate) Text(SimpleDateFormat("EEEE · MMMM d", Locale.getDefault()).format(now), color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(icon = Icons.Filled.Notifications, label = "Quick controls", onClick = onOpenQuickPanel)
                RoundAction(icon = Icons.Filled.Tune, label = "Launcher settings", onClick = { onScreen(Screen.CUSTOMIZE) })
            }
        }
        SearchLauncherBar(onClick = { onScreen(Screen.APPS) }, hint = "Search installed apps")

        if (config.weatherEnabled && config.weatherCity.isNotBlank()) {
            WeatherCard(city = config.weatherCity, unit = config.weatherUnit)
        }
        if (config.showMusicControls) MusicControlsCard()

        if (!activity.isDefaultLauncher()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Make Thuku your home", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Use the Home button to return here", color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { activity.setAsDefaultLauncher() }) { Text("Set up") }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("WORKSPACE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Text("${homeApps.size} shortcuts · ${if (config.workspaceLocked) "workspace locked" else "hold an icon to edit"}", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { onScreen(Screen.APPS) }) { Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("App library") }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(104.dp * config.homeRows.coerceIn(2, 5).toFloat())
                .pointerInput(activePage, pageCount) {
                    var dragX = 0f
                    var dragY = 0f
                    detectDragGestures(
                        onDragStart = { dragX = 0f; dragY = 0f },
                        onDrag = { change, amount ->
                            dragX += amount.x
                            dragY += amount.y
                            if (kotlin.math.abs(dragX) > 14f || kotlin.math.abs(dragY) > 14f) change.consume()
                        },
                        onDragEnd = {
                            when {
                                dragY < -65f -> when (config.swipeUpAction) {
                                    "Quick panel" -> onOpenQuickPanel()
                                    "Focus timer" -> onScreen(Screen.FOCUS)
                                    "Customize" -> onScreen(Screen.CUSTOMIZE)
                                    "Quick tools" -> onScreen(Screen.TOOLS)
                                    else -> onScreen(Screen.APPS)
                                }
                                dragY > 90f -> when (config.swipeDownAction) {
                                    "App Library" -> onScreen(Screen.APPS)
                                    "Focus timer" -> onScreen(Screen.FOCUS)
                                    "Customize" -> onScreen(Screen.CUSTOMIZE)
                                    "Quick tools" -> onScreen(Screen.TOOLS)
                                    else -> onOpenQuickPanel()
                                }
                                dragX < -65f && activePage < pageCount - 1 -> activePage++
                                dragX > 65f && activePage > 0 -> activePage--
                            }
                        }
                    )
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(config.columns.coerceIn(3, 6)),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(pageSize, key = { "workspace-$activePage-slot-$it" }) { index ->
                    val app = pageApps.getOrNull(index)
                    if (app != null) {
                        AppTile(
                            app = app, config = config, modifier = Modifier.fillMaxWidth(),
                            onClick = { activity.launchApp(app) },
                            onLongClick = {
                                if (config.workspaceLocked) android.widget.Toast.makeText(activity, "Workspace locked. Disable Workspace lock in Customize to edit.", android.widget.Toast.LENGTH_SHORT).show()
                                else onManageApp(app)
                            }
                        )
                    } else {
                        WorkspaceAddTile(config = config, onClick = { onScreen(Screen.APPS) })
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(pageCount) { page ->
                val selected = page == activePage
                Box(
                    Modifier.padding(horizontal = 4.dp).size(width = if (selected) 20.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.32f))
                        .combinedClickable(onClick = { activePage = page }, onLongClick = { activePage = page })
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.White.copy(alpha = 0.44f), modifier = Modifier.size(17.dp))
            Text("Swipe up: ${config.swipeUpAction} · swipe down: ${config.swipeDownAction} · swipe sideways for pages", color = Color.White.copy(alpha = 0.45f), style = MaterialTheme.typography.labelSmall)
        }

        if (config.showRecentApps) {
            val recentApps = remember(apps, activity.refreshSignal.intValue, config.hidden) { AppUsageStore(activity).recent(apps.filterNot { it.key in config.hidden }, 5) }
            if (recentApps.isNotEmpty()) {
                SectionLabel("RECENTLY OPENED", "Stored on this device · no usage permission required")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentApps.forEach { app -> MiniAppChip(app, config, onClick = { activity.launchApp(app) }) }
                }
            }
        }

        if (config.folders.isNotEmpty()) {
            SectionLabel("FOLDERS", "Tap to open · long-press to remove")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                config.folders.toSortedMap().forEach { (name, keys) ->
                    FolderTile(
                        name = name,
                        count = keys.count { key -> apps.any { it.key == key } && key !in config.hidden },
                        config = config,
                        onClick = { onOpenFolder(name) },
                        onLongClick = { onDeleteFolder(name) }
                    )
                }
            }
        }

        if (customWidgets.isNotEmpty()) {
            SectionLabel("YOUR CUSTOM WIDGETS", "${customWidgets.size} saved · local and configurable")
            customWidgets.forEach { widget ->
                CustomWidgetCard(
                    item = widget, config = config, appCount = visibleApps.size,
                    favouriteCount = homeApps.size, folderCount = config.folders.size,
                    onUpdate = { customWidgets = customWidgetStore.update(it) },
                    onRemove = { customWidgets = customWidgetStore.remove(widget.id) },
                    onOpenApps = { onScreen(Screen.APPS) }
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WIDGET STUDIO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("${CustomWidgetKind.entries.size} custom widgets + Android widgets", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { showCustomWidgetPicker = true }, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(3.dp)); Text("Custom")
                }
                OutlinedButton(onClick = { activity.pickSystemWidget() }, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) { Text("Android") }
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
                        Text("Add a live Android widget", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Clock, weather, calendar, music and other widgets from installed apps.", color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.65f))
                }
            }
        } else {
            widgetIds.forEach { id -> SystemWidgetCard(activity = activity, widgetId = id, height = if (config.compactHome) 142.dp else 190.dp) }
        }
        Spacer(Modifier.height(18.dp))
    }
    if (showCustomWidgetPicker) {
        CustomWidgetCatalogDialog(
            onDismiss = { showCustomWidgetPicker = false },
            onAdd = { kind -> customWidgets = customWidgetStore.add(kind) },
            currentCount = customWidgets.size
        )
    }
}

@Composable
private fun WorkspaceAddTile(config: LauncherConfig, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = onClick, onLongClick = onClick).padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(52.dp).clip(iconShape(config.iconShape)).background(Color.White.copy(alpha = 0.055f)).border(1.dp, Color.White.copy(alpha = 0.10f), iconShape(config.iconShape)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "Add app to home", tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(22.dp))
        }
        if (config.showLabels) Text("Add app", color = Color.White.copy(alpha = 0.47f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun WorkspaceDock(
    apps: List<LauncherApp>,
    config: LauncherConfig,
    onLaunch: (LauncherApp) -> Unit,
    onManageApp: (LauncherApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onCustomize: () -> Unit
) {
    val lookup = remember(apps) { apps.associateBy { it.key } }
    val context = LocalContext.current
    val dockApps = config.dockApps.mapNotNull { lookup[it] }.filterNot { it.key in config.hidden }.take(config.dockSlots.coerceIn(3, 7))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp).clip(RoundedCornerShape(28.dp))
            .background(if (config.useGlass) Color(0xD91A1E28) else Color(0xFF151922))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dockApps.forEach { app ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).combinedClickable(
                    onClick = { onLaunch(app) },
                    onLongClick = {
                        if (config.workspaceLocked) android.widget.Toast.makeText(context, "Workspace locked. Disable Workspace lock in Customize to edit.", android.widget.Toast.LENGTH_SHORT).show()
                        else onManageApp(app)
                    }
                ).padding(vertical = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                AppIcon(app, config.iconShape, 39.dp * config.iconScale.coerceIn(0.8f, 1.15f), config.iconPackPackage)
                if (config.showLabels) Text(launcherLabel(app, config), color = Color.White.copy(alpha = 0.73f), style = MaterialTheme.typography.labelSmall, fontSize = (8.sp * config.labelScale), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(onClick = onOpenDrawer, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)) ) {
            Box(Modifier.size(width = 48.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Apps, contentDescription = "Open app drawer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
        Surface(onClick = onCustomize, shape = CircleShape, color = Color.White.copy(alpha = 0.07f)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Tune, contentDescription = "Customize launcher", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(19.dp)) }
        }
    }
}

@Composable
private fun AppDrawerScreen(
    activity: MainActivity,
    apps: List<LauncherApp>,
    config: LauncherConfig,
    appsLoading: Boolean,
    appsLoadError: String?,
    onScreen: (Screen) -> Unit,
    onConfig: (LauncherConfig) -> Unit,
    onManageApp: (LauncherApp) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    var sortPinnedFirst by remember { mutableStateOf(false) }
    val visibleApps = apps.filter { app ->
            val hiddenMode = filter == AppFilter.HIDDEN
            val passesHidden = if (hiddenMode) app.key in config.hidden else app.key !in config.hidden
            if (!passesHidden) return@filter false
            val matchesSearch = launcherLabel(app, config).contains(search.trim(), ignoreCase = true) || app.label.contains(search.trim(), ignoreCase = true) || app.packageName.contains(search.trim(), ignoreCase = true)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.FAVORITES -> app.key in config.favourites
                AppFilter.HIDDEN -> true
                else -> classifyApp(app) == filter
            }
            matchesSearch && matchesFilter
        }
        .let { filtered ->
            val usage = AppUsageStore(activity)
            val sorted = when (config.drawerSort) {
                "Most used" -> filtered.sortedWith(compareByDescending<LauncherApp> { usage.count(it.key) }.thenBy { launcherLabel(it, config).lowercase(Locale.getDefault()) })
                "Recently used" -> filtered.sortedWith(compareByDescending<LauncherApp> { usage.lastUsed(it.key) }.thenBy { launcherLabel(it, config).lowercase(Locale.getDefault()) })
                "Package name" -> filtered.sortedBy { it.packageName.lowercase(Locale.getDefault()) }
                else -> filtered.sortedBy { launcherLabel(it, config).lowercase(Locale.getDefault()) }
            }
            if (sortPinnedFirst) sorted.sortedWith(compareByDescending<LauncherApp> { config.favourites.contains(it.key) }.thenBy { launcherLabel(it, config).lowercase(Locale.getDefault()) }) else sorted
        }

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("View", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
            FilterPill("Grid", selected = config.drawerLayout == "Grid", onClick = { onConfig(config.copy(drawerLayout = "Grid")) })
            FilterPill("List", selected = config.drawerLayout == "List", onClick = { onConfig(config.copy(drawerLayout = "List")) })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { sortPinnedFirst = !sortPinnedFirst }) { Text(if (sortPinnedFirst) "Pinned ✓" else "Pinned first") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
            listOf("Alphabetical", "Most used", "Recently used", "Package name").forEach { mode ->
                FilterPill(mode, selected = config.drawerSort == mode, onClick = { onConfig(config.copy(drawerSort = mode)) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${visibleApps.size} apps", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { activity.refreshSignal.intValue++ }) { Text("Refresh") }
        }
        if (visibleApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(24.dp)) {
                    if (appsLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Scanning installed apps…", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Thuku is building your app library.", color = Color.White.copy(alpha = 0.62f), textAlign = TextAlign.Center)
                    } else if (apps.isEmpty() && appsLoadError != null) {
                        Icon(Icons.Filled.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                        Text("Couldn't load your apps", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(appsLoadError, color = Color.White.copy(alpha = 0.68f), textAlign = TextAlign.Center)
                        Button(onClick = { activity.refreshSignal.intValue++ }, shape = RoundedCornerShape(15.dp)) {
                            Icon(Icons.Filled.Tune, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Try again")
                        }
                    } else {
                        Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                        Text("Nothing found", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Try another name or category.", color = Color.White.copy(alpha = 0.62f), textAlign = TextAlign.Center)
                        TextButton(onClick = { search = ""; filter = AppFilter.ALL }) { Text("Clear search and filters") }
                    }
                }
            }
        } else {
            if (config.drawerLayout == "List") {
                LazyColumn(
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleApps, key = { it.key }) { app ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(
                                onClick = { activity.launchApp(app) },
                                onLongClick = { onManageApp(app) }
                            ).background(config.surfaceColor()).padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIcon(app, config.iconShape, 40.dp * config.iconScale.coerceIn(0.8f, 1.25f), config.iconPackPackage)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(launcherLabel(app, config), color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onManageApp(app) }) { Icon(Icons.Filled.MoreHoriz, "App options", tint = Color.White.copy(alpha = 0.7f)) }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.columns), modifier = Modifier.weight(1f),
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
    var diagnosticText by remember { mutableStateOf(CrashDiagnostics.read(activity)) }
    val exportDiagnostics = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                val output = activity.contentResolver.openOutputStream(uri) ?: error("Could not open the selected file for writing.")
                output.bufferedWriter().use { it.write(diagnosticText ?: "No diagnostic report is saved.") }
            }.onSuccess {
                android.widget.Toast.makeText(activity, "Diagnostic report exported.", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                CrashDiagnostics.record(activity, "Diagnostic report export failed", error)
                android.widget.Toast.makeText(activity, error.message ?: "Couldn't export diagnostics.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
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

        SectionLabel("RELIABILITY", "Recovery tools that stay on your phone")
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (diagnosticText.isNullOrBlank()) "No saved crash report" else "A diagnostic report is available", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Unexpected app crashes and selected recovery errors are saved locally. Thuku does not upload them.", color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!diagnosticText.isNullOrBlank()) {
                    Text(diagnosticText!!.takeLast(1_400), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { activity.refreshSignal.intValue++ }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Refresh apps")
                    }
                    OutlinedButton(onClick = { exportDiagnostics.launch("thuku-diagnostics.txt") }, enabled = !diagnosticText.isNullOrBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Export log")
                    }
                }
                TextButton(
                    onClick = {
                        val cleared = CrashDiagnostics.clear(activity)
                        diagnosticText = CrashDiagnostics.read(activity)
                        android.widget.Toast.makeText(activity, if (cleared) "Local diagnostics cleared." else "Couldn't clear diagnostics.", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = !diagnosticText.isNullOrBlank()
                ) { Text("Clear saved report") }
            }
        }
    }
}

/** Bounds user-selected backup input before JSON parsing. */
private fun readLauncherBackupBounded(input: InputStream): String = input.use { stream ->
    val maxBytes = 100_000
    val output = ByteArrayOutputStream(maxBytes + 1)
    val buffer = ByteArray(8_192)
    var total = 0
    while (total <= maxBytes) {
        val count = stream.read(buffer, 0, minOf(buffer.size, maxBytes + 1 - total))
        if (count <= 0) break
        output.write(buffer, 0, count)
        total += count
    }
    require(total <= maxBytes) { "Backup file exceeds the 100 KB size limit." }
    output.toByteArray().toString(Charsets.UTF_8)
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
        if (uri != null) runCatching {
            val output = context.contentResolver.openOutputStream(uri) ?: error("Could not create the backup file.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(backupConfig(config)) }
        }.onFailure { android.widget.Toast.makeText(context, it.message ?: "Could not export backup", android.widget.Toast.LENGTH_LONG).show() }
    }
    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: error("Could not read the selected file.")
            val text = readLauncherBackupBounded(input)
            onConfig(restoreConfig(text, config))
        }.onFailure { android.widget.Toast.makeText(context, it.message ?: "Could not restore backup", android.widget.Toast.LENGTH_LONG).show() }
    }
    val customWallpaperPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onConfig(config.copy(customWallpaperUri = uri.toString(), useWallpaper = true))
            android.widget.Toast.makeText(context, "Custom wallpaper saved.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    var weatherCityDraft by remember(config.weatherCity) { mutableStateOf(config.weatherCity) }

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

        SectionLabel("THEME CODE STUDIO", "Build and apply your own theme")
        SettingsCard(config) {
            ThemeCodeStudio(config = config, onApply = onConfig)
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
            SettingsSliderRow(title = "Grid columns", valueText = "${config.columns}", value = config.columns.toFloat(), range = 3f..6f, steps = 2, onValue = { onConfig(config.copy(columns = it.toInt().coerceIn(3, 6))) })
            SettingsSliderRow(title = "Workspace rows", valueText = "${config.homeRows}", value = config.homeRows.toFloat(), range = 2f..5f, steps = 2, onValue = { onConfig(config.copy(homeRows = it.toInt().coerceIn(2, 5))) })
            SettingsSliderRow(title = "App label size", valueText = "${(config.labelScale * 100).toInt()}%", value = config.labelScale, range = 0.8f..1.4f, onValue = { onConfig(config.copy(labelScale = it.coerceIn(0.8f, 1.4f))) })
            SettingsSliderRow(title = "Dock capacity", valueText = "${config.dockSlots} apps", value = config.dockSlots.toFloat(), range = 3f..7f, steps = 3, onValue = {
                val slots = it.toInt().coerceIn(3, 7)
                onConfig(config.copy(dockSlots = slots, dockApps = config.dockApps.take(slots)))
            })
            SettingsSwitchRow("App labels", "Show names below icons", config.showLabels) { onConfig(config.copy(showLabels = it)) }
            SettingsSwitchRow("Dynamic system colors", "Use Material You colors on Android 12 and newer", config.useDynamicColor) { onConfig(config.copy(useDynamicColor = it)) }
            SettingsSwitchRow("Glass surfaces", "Translucent cards and soft borders", config.useGlass) { onConfig(config.copy(useGlass = it)) }
            SettingsSwitchRow("Compact home", "Tighter clock and widget spacing", config.compactHome) { onConfig(config.copy(compactHome = it)) }
            SettingsSwitchRow("Animated transitions", "Subtle screen motion and fades", config.animateUi) { onConfig(config.copy(animateUi = it)) }
            SettingsSwitchRow("Show date", "Display date under the clock", config.showDate) { onConfig(config.copy(showDate = it)) }
            SettingsSwitchRow("Recently opened apps", "Local-only recent shortcuts on the home page", config.showRecentApps) { onConfig(config.copy(showRecentApps = it)) }
            SettingsSwitchRow("Lock workspace", "Prevent home-icon shortcut editing by long press", config.workspaceLocked) { onConfig(config.copy(workspaceLocked = it)) }
        }

        SectionLabel("WALLPAPER STUDIO", "Photo picker, system wallpaper and readability")
        SettingsCard(config) {
            SettingsSwitchRow("Use a wallpaper backdrop", "Show your selected photo or the device wallpaper", config.useWallpaper) { onConfig(config.copy(useWallpaper = it)) }
            SettingsSliderRow(title = "Wallpaper dim", valueText = "${(config.wallpaperDim * 100).toInt()}%", value = config.wallpaperDim, range = 0f..0.8f, onValue = { onConfig(config.copy(wallpaperDim = it)) })
            FilledTonalButton(onClick = { customWallpaperPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Filled.Wallpaper, null); Spacer(Modifier.width(8.dp)); Text(if (config.customWallpaperUri.isBlank()) "Choose image from device" else "Change custom wallpaper")
            }
            if (config.customWallpaperUri.isNotBlank()) {
                OutlinedButton(onClick = { onConfig(config.copy(customWallpaperUri = "")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text("Use system wallpaper instead") }
            }
            OutlinedButton(onClick = onChooseWallpaper, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Filled.Wallpaper, null); Spacer(Modifier.width(8.dp)); Text("Change device wallpaper")
            }
        }

        SectionLabel("WEATHER", "Optional manual city lookup · no location permission")
        SettingsCard(config) {
            SettingsSwitchRow("Show weather card", "Fetch conditions only when a city is entered", config.weatherEnabled) { onConfig(config.copy(weatherEnabled = it)) }
            OutlinedTextField(
                value = weatherCityDraft,
                onValueChange = { value -> weatherCityDraft = value.filter { it.code >= 0x20 && it.code != 0x7F }.take(80) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("City name") }, placeholder = { Text("e.g. Agra") }, shape = RoundedCornerShape(15.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Celsius", selected = config.weatherUnit == "C", onClick = { onConfig(config.copy(weatherUnit = "C")) })
                FilterPill("Fahrenheit", selected = config.weatherUnit == "F", onClick = { onConfig(config.copy(weatherUnit = "F")) })
                Spacer(Modifier.weight(1f))
                Button(onClick = { onConfig(config.copy(weatherCity = weatherCityDraft.trim().take(80), weatherEnabled = weatherCityDraft.isNotBlank())) }, enabled = weatherCityDraft.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("Save city") }
            }
            Text("Uses the Open-Meteo geocoding and forecast APIs over HTTPS. The city is sent to that provider only when this card is enabled. No GPS/location permission is requested.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
        }

        SectionLabel("MEDIA CONTROLS", "The useful part of a music widget")
        SettingsCard(config) {
            SettingsSwitchRow("Show now-playing card", "Optional transport controls for active media sessions", config.showMusicControls) { onConfig(config.copy(showMusicControls = it)) }
            Text("If enabled, Android may ask you to grant notification-listener access. Thuku reads active media-session metadata for the player controls; it does not save or upload notification text. You can revoke access in Android Settings at any time.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
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

        SectionLabel("GESTURE LAB", "Configure the swipe actions on your workspace")
        SettingsCard(config) {
            Text("Swipe up", color = Color.White, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                launcherGestureActions.forEach { action -> FilterPill(action, selected = config.swipeUpAction == action, onClick = { onConfig(config.copy(swipeUpAction = action)) }) }
            }
            Text("Swipe down", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                launcherGestureActions.forEach { action -> FilterPill(action, selected = config.swipeDownAction == action, onClick = { onConfig(config.copy(swipeDownAction = action)) }) }
            }
            Text("Gestures work inside the empty workspace grid region. The operating system owns status-bar and edge navigation gestures.", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
        }

        SectionLabel("SMART SHORTCUTS", "One tap to Android tools")
        SettingsCard(config) {
            SettingsNavigationRow(Icons.Filled.Home, "Set as default launcher", "Choose Thuku as your Home app", onMakeDefault)
            SettingsNavigationRow(Icons.Filled.Wifi, "Quick tools", "Internet, display, sound and more", onOpenTools)
            SettingsNavigationRow(Icons.Filled.Apps, "Hidden apps", "${config.hidden.size} hidden in this launcher", { onScreen(Screen.APPS) })
            SettingsNavigationRow(Icons.Filled.Lock, "Privacy & permissions", "Local-first; optional city weather and media controls", { android.widget.Toast.makeText(context, "Launcher settings, layout and launch history stay local. Internet is used only for weather when you enable the weather card and enter a city. Notification-listener access is optional for active media controls; Thuku does not store or upload notification text. Third-party system widgets can have their own permissions and network behavior.", android.widget.Toast.LENGTH_LONG).show() })
        }

        SectionLabel("BACKUP & RESTORE", "Take your setup with you")
        SettingsCard(config) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { backupLauncher.launch("thuku-launcher-backup.json") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Export setup") }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Import setup") }
            }
            Text("The backup includes appearance settings, selected icon pack, workspace order, dock shortcuts, hidden apps, custom labels, city-weather settings and folders. System widgets and media access remain on this phone and are not included.", color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.bodySmall)
        }

        SectionLabel("ABOUT THUKU", "Version 0.5.0")
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = config.surfaceColor())) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("THUKU LAUNCHER", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("A privacy-conscious, deeply customizable home screen. Settings, workspace order, labels and launch history stay on this device.", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                Text("Network use is optional city weather from Open-Meteo. Music controls require explicit notification-listener access. Android widget providers may run their own code. Some system controls and gestures vary by device.", color = Color.White.copy(alpha = 0.48f), style = MaterialTheme.typography.labelSmall)
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
        if (config.showLabels) Text(launcherLabel(app, config), color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp * config.labelScale, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp * config.labelScale, modifier = Modifier.fillMaxWidth())
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
            Text(launcherLabel(app, config), color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppManageDialog(
    app: LauncherApp,
    isPinned: Boolean,
    isDocked: Boolean,
    isHidden: Boolean,
    displayLabel: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
    onFolder: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDockToggle: (Boolean) -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onHideToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var labelDraft by remember(app.key, displayLabel) { mutableStateOf(displayLabel) }
    var showUninstallConfirm by remember(app.key) { mutableStateOf(false) }
    val isSystemApp = remember(app.packageName) {
        runCatching {
            (context.packageManager.getApplicationInfo(app.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Apps, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(displayLabel.ifBlank { app.label }) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(app.packageName, color = Color.White.copy(alpha = 0.54f), style = MaterialTheme.typography.labelSmall)
                Text("Rename shortcut", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it.filter { ch -> ch.code >= 0x20 && ch.code != 0x7F }.take(40) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text(app.label) }, shape = RoundedCornerShape(13.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = { onRename(labelDraft); onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Save name") }
                    OutlinedButton(onClick = { labelDraft = ""; onRename(""); onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Reset name") }
                }
                if (isPinned) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = onMoveEarlier) { Text("Move earlier") }
                        TextButton(onClick = onMoveLater) { Text("Move later") }
                    }
                }
                Text("App actions", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { android.widget.Toast.makeText(context, "App info isn't available on this device.", android.widget.Toast.LENGTH_SHORT).show() }
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("App info") }
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Play Store") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Package name", app.packageName))
                        android.widget.Toast.makeText(context, "Package name copied.", android.widget.Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Copy package") }
                    OutlinedButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "${app.label} — https://play.google.com/store/apps/details?id=${app.packageName}")
                        runCatching { context.startActivity(Intent.createChooser(share, "Share app").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Share app") }
                }
                if (isSystemApp) {
                    Text("Android marks this as a system app. Removing it may be unavailable or restricted by your device.", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
                }
                else {
                    TextButton(onClick = { showUninstallConfirm = true }) { Text("Uninstall app…", color = Color(0xFFFFA4A4)) }
                }
                Text("These controls change the Thuku shortcut layout. Uninstall is handled by Android and can be cancelled.", color = Color.White.copy(alpha = 0.50f), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onPinToggle(!isPinned) }) { Text(if (isPinned) "Remove from workspace" else "Add to workspace") }
                TextButton(onClick = { onDockToggle(isDocked) }) { Text(if (isDocked) "Remove from dock" else "Add to dock") }
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.Start) {
                TextButton(onClick = onFolder) { Text("Add to folder") }
                TextButton(onClick = { onHideToggle(!isHidden) }) { Text(if (isHidden) "Unhide app" else "Hide app") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        containerColor = Color(0xFF171B24),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
    if (showUninstallConfirm) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = false },
            title = { Text("Uninstall ${app.label}?") },
            text = { Text("Android will ask you to confirm. Thuku cannot silently uninstall an app or bypass system protections.") },
            confirmButton = {
                TextButton(onClick = {
                    showUninstallConfirm = false
                    runCatching { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { android.widget.Toast.makeText(context, "Android couldn't open the uninstall screen.", android.widget.Toast.LENGTH_LONG).show() }
                    onDismiss()
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showUninstallConfirm = false }) { Text("Cancel") } },
            containerColor = Color(0xFF171B24), titleContentColor = Color.White, textContentColor = Color.White.copy(alpha = 0.7f)
        )
    }
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
