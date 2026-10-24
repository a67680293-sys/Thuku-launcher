package com.thuku.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.xmlpull.v1.XmlPullParser
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import org.json.JSONArray

internal enum class Screen { HOME, APPS, FOCUS, TOOLS, CUSTOMIZE }
internal enum class AppFilter(val title: String) { ALL("All apps"), FAVORITES("Pinned"), SOCIAL("Social"), MEDIA("Media"), GAMES("Games"), TOOLS("Tools"), HIDDEN("Hidden") }

internal data class LauncherApp(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val icon: Drawable,
    val systemCategory: Int
) {
    val key: String get() = componentName.flattenToString() + "@" + user.hashCode()
}

internal data class AccentPreset(val name: String, val argb: Long) {
    val color: Color get() = Color(argb)
}

internal val accentPresets = listOf(
    AccentPreset("Volt", 0xFFC9F56AL),
    AccentPreset("Ice", 0xFF8CEBFFL),
    AccentPreset("Violet", 0xFFC3A4FFL),
    AccentPreset("Rose", 0xFFFF9FC9L),
    AccentPreset("Amber", 0xFFFFC16AL),
    AccentPreset("Mint", 0xFF8CFFD0L),
    AccentPreset("Pixel blue", 0xFF9BB9FFL),
    AccentPreset("White", 0xFFF4F5F2L)
)

internal data class LauncherConfig(
    val accentArgb: Long = accentPresets.first().argb,
    val iconScale: Float = 1.0f,
    val columns: Int = 4,
    val showLabels: Boolean = true,
    val useGlass: Boolean = true,
    val useWallpaper: Boolean = true,
    val wallpaperDim: Float = 0.48f,
    val clockStyle: String = "Editorial",
    val iconShape: String = "Squircle",
    val iconPackPackage: String = "",
    val animateUi: Boolean = true,
    val showDate: Boolean = true,
    val compactHome: Boolean = false,
    val useDynamicColor: Boolean = false,
    val customWallpaperUri: String = "",
    val drawerLayout: String = "Grid",
    val drawerSort: String = "Alphabetical",
    val dockSlots: Int = 5,
    val labelScale: Float = 1.0f,
    val homeRows: Int = 3,
    val workspaceLocked: Boolean = false,
    val showRecentApps: Boolean = true,
    val weatherEnabled: Boolean = false,
    val weatherCity: String = "",
    val weatherUnit: String = "C",
    val showMusicControls: Boolean = false,
    val swipeUpAction: String = "App Library",
    val swipeDownAction: String = "Quick panel",
    val customLabels: Map<String, String> = emptyMap(),
    val favourites: Set<String> = emptySet(),
    /** Ordered app keys shown on home workspace pages. */
    val homeOrder: List<String> = emptyList(),
    /** Ordered app keys in the persistent bottom dock. */
    val dockApps: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val folders: Map<String, Set<String>> = emptyMap()
) {
    fun surfaceColor(): Color = if (useGlass) Color(0xAA151922) else Color(0xFF151922)
}

internal class LauncherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("thuku_launcher_preferences", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = LauncherConfig(
        accentArgb = prefs.getLong("accent", accentPresets.first().argb),
        iconScale = LauncherValidation.iconScale(prefs.getFloat("icon_scale", 1.0f)),
        columns = LauncherValidation.columns(prefs.getInt("columns", 4)),
        showLabels = prefs.getBoolean("show_labels", true),
        useGlass = prefs.getBoolean("glass", true),
        useWallpaper = prefs.getBoolean("wallpaper", true),
        wallpaperDim = LauncherValidation.wallpaperDim(prefs.getFloat("wallpaper_dim", 0.48f)),
        clockStyle = prefs.getString("clock_style", "Editorial") ?: "Editorial",
        iconShape = prefs.getString("icon_shape", "Squircle") ?: "Squircle",
        iconPackPackage = prefs.getString("icon_pack_package", "") ?: "",
        animateUi = prefs.getBoolean("animate_ui", true),
        showDate = prefs.getBoolean("show_date", true),
        compactHome = prefs.getBoolean("compact_home", false),
        useDynamicColor = prefs.getBoolean("dynamic_color", false),
        customWallpaperUri = prefs.getString("custom_wallpaper_uri", "") ?: "",
        drawerLayout = prefs.getString("drawer_layout", "Grid")?.takeIf { it in setOf("Grid", "List") } ?: "Grid",
        drawerSort = prefs.getString("drawer_sort", "Alphabetical")?.takeIf { it in setOf("Alphabetical", "Most used", "Recently used", "Package name") } ?: "Alphabetical",
        dockSlots = prefs.getInt("dock_slots", 5).coerceIn(3, 7),
        labelScale = prefs.getFloat("label_scale", 1.0f).coerceIn(0.8f, 1.4f),
        homeRows = prefs.getInt("home_rows", 3).coerceIn(2, 5),
        workspaceLocked = prefs.getBoolean("workspace_locked", false),
        showRecentApps = prefs.getBoolean("show_recent_apps", true),
        weatherEnabled = prefs.getBoolean("weather_enabled", false),
        weatherCity = prefs.getString("weather_city", "")?.take(80) ?: "",
        weatherUnit = prefs.getString("weather_unit", "C")?.takeIf { it == "C" || it == "F" } ?: "C",
        showMusicControls = prefs.getBoolean("show_music_controls", false),
        swipeUpAction = prefs.getString("swipe_up_action", "App Library")?.takeIf { it in launcherGestureActions } ?: "App Library",
        swipeDownAction = prefs.getString("swipe_down_action", "Quick panel")?.takeIf { it in launcherGestureActions } ?: "Quick panel",
        customLabels = decodeLabels(prefs.getString("custom_labels", "{}")),
        favourites = split(prefs.getString("favourites", "")),
        homeOrder = splitList(prefs.getString("home_order", "")),
        dockApps = splitList(prefs.getString("dock_apps", "")),
        hidden = split(prefs.getString("hidden", "")),
        folders = decodeFolders(prefs.getString("folders", "{}"))
    )

    fun save(config: LauncherConfig) {
        prefs.edit()
            .putLong("accent", config.accentArgb)
            .putFloat("icon_scale", config.iconScale)
            .putInt("columns", config.columns)
            .putBoolean("show_labels", config.showLabels)
            .putBoolean("glass", config.useGlass)
            .putBoolean("wallpaper", config.useWallpaper)
            .putFloat("wallpaper_dim", config.wallpaperDim)
            .putString("clock_style", config.clockStyle)
            .putString("icon_shape", config.iconShape)
            .putString("icon_pack_package", config.iconPackPackage)
            .putBoolean("animate_ui", config.animateUi)
            .putBoolean("show_date", config.showDate)
            .putBoolean("compact_home", config.compactHome)
            .putBoolean("dynamic_color", config.useDynamicColor)
            .putString("custom_wallpaper_uri", config.customWallpaperUri.take(2_000))
            .putString("drawer_layout", config.drawerLayout)
            .putString("drawer_sort", config.drawerSort)
            .putInt("dock_slots", config.dockSlots.coerceIn(3, 7))
            .putFloat("label_scale", config.labelScale.coerceIn(0.8f, 1.4f))
            .putInt("home_rows", config.homeRows.coerceIn(2, 5))
            .putBoolean("workspace_locked", config.workspaceLocked)
            .putBoolean("show_recent_apps", config.showRecentApps)
            .putBoolean("weather_enabled", config.weatherEnabled)
            .putString("weather_city", config.weatherCity.take(80))
            .putString("weather_unit", config.weatherUnit)
            .putBoolean("show_music_controls", config.showMusicControls)
            .putString("swipe_up_action", config.swipeUpAction)
            .putString("swipe_down_action", config.swipeDownAction)
            .putString("custom_labels", encodeLabels(config.customLabels))
            .putString("favourites", config.favourites.joinToString("\n"))
            .putString("home_order", config.homeOrder.distinct().joinToString("\n"))
            .putString("dock_apps", config.dockApps.distinct().take(config.dockSlots.coerceIn(3, 7)).joinToString("\n"))
            .putString("hidden", config.hidden.joinToString("\n"))
            .putString("folders", encodeFolders(config.folders))
            .apply()
        if (config.homeOrder.isNotEmpty() || config.favourites.isNotEmpty()) {
            prefs.edit().putBoolean("workspace_seeded", true).apply()
        }
    }

    /** Add a usable workspace on the first successful app scan instead of starting with an empty screen. */
    fun seedWorkspaceIfNeeded(apps: List<LauncherApp>): LauncherConfig? {
        if (prefs.getBoolean("workspace_seeded", false) || apps.isEmpty()) return null
        val storedFavourites = prefs.getString("favourites", "").orEmpty()
        val storedOrder = prefs.getString("home_order", "").orEmpty()
        if (storedFavourites.isNotBlank() || storedOrder.isNotBlank()) {
            // Migrate the previous release without overwriting the user's pins or visual preferences.
            prefs.edit().putBoolean("workspace_seeded", true).apply()
            val old = load()
            val migratedOrder = old.homeOrder.ifEmpty { old.favourites.toList() }
            val migratedDock = old.dockApps.ifEmpty { migratedOrder.take(5) }
            val migrated = old.copy(homeOrder = migratedOrder, dockApps = migratedDock)
            save(migrated)
            return migrated
        }
        val picked = linkedMapOf<String, LauncherApp>()
        val labels = listOf(
            listOf("phone", "dialer"),
            listOf("messages", "messaging", "sms"),
            listOf("chrome", "browser", "internet"),
            listOf("camera"),
            listOf("photos", "gallery"),
            listOf("settings"),
            listOf("play store", "app store"),
            listOf("whatsapp"),
            listOf("youtube"),
            listOf("maps"),
            listOf("gmail", "mail"),
            listOf("contacts"),
            listOf("calendar"),
            listOf("clock"),
            listOf("files", "file manager"),
            listOf("calculator")
        )
        labels.forEach { alternatives ->
            apps.firstOrNull { app ->
                val name = app.label.lowercase()
                alternatives.any { token -> name == token || name.contains(token) }
            }?.let { picked.putIfAbsent(it.key, it) }
        }
        apps.filter { it.key !in picked }.take((16 - picked.size).coerceAtLeast(0)).forEach { picked[it.key] = it }
        val home = picked.values.take(16).map { it.key }
        if (home.isEmpty()) return null
        val preferredDock = home.filter { key ->
            val app = apps.firstOrNull { it.key == key } ?: return@filter false
            listOf("phone", "messages", "chrome", "camera", "whatsapp").any { app.label.contains(it, ignoreCase = true) }
        }.take(5)
        val dock = (preferredDock + home.filterNot { it in preferredDock }).take(5)
        prefs.edit().putBoolean("workspace_seeded", true).apply()
        val seeded = load().copy(favourites = home.toSet(), homeOrder = home, dockApps = dock)
        save(seeded)
        return seeded
    }

    private fun splitList(value: String?): List<String> = value.orEmpty()
        .split('\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(2_000)

    private fun encodeFolders(folders: Map<String, Set<String>>): String = JSONObject().apply {
        folders.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) }
    }.toString()

    private fun encodeLabels(labels: Map<String, String>): String = JSONObject().apply {
        labels.entries.take(2_000).forEach { (key, label) ->
            val safe = label.filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(40)
            if (key.isNotBlank() && safe.isNotBlank()) put(key.take(512), safe)
        }
    }.toString()

    private fun decodeLabels(raw: String?): Map<String, String> = runCatching {
        val json = JSONObject(raw ?: "{}")
        val result = linkedMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext() && result.size < 2_000) {
            val key = keys.next().take(512)
            val value = json.optString(key).filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(40)
            if (key.isNotBlank() && value.isNotBlank()) result[key] = value
        }
        result
    }.getOrDefault(emptyMap())

    private fun decodeFolders(raw: String?): Map<String, Set<String>> = try {
        val json = JSONObject(raw ?: "{}")
        val result = linkedMapOf<String, Set<String>>()
        val names = json.keys()
        while (names.hasNext()) {
            val name = names.next()
            val array = json.optJSONArray(name) ?: JSONArray()
            val keys = buildSet { for (i in 0 until array.length()) { val item = array.optString(i); if (item.isNotBlank()) add(item) } }
            if (name.isNotBlank() && keys.isNotEmpty()) result[name] = keys
        }
        result
    } catch (_: Exception) { emptyMap() }

    private fun split(value: String?): Set<String> = value.orEmpty()
        .split('\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
}

internal val launcherGestureActions = setOf("App Library", "Quick panel", "Focus timer", "Customize", "Quick tools")

/** Local-only launch history. No Android usage-access permission is needed. */
internal class AppUsageStore(context: Context) {
    private val prefs = context.getSharedPreferences("thuku_app_usage", Context.MODE_PRIVATE)
    private val lock = Any()

    fun record(key: String) = synchronized(lock) {
        if (key.isBlank()) return@synchronized
        val root = runCatching { JSONObject(prefs.getString("usage", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val previous = root.optJSONObject(key) ?: JSONObject()
        root.put(key, JSONObject().put("count", (previous.optInt("count", 0) + 1).coerceAtMost(1_000_000)).put("last", System.currentTimeMillis()))
        // Keep the newest 2,000 records to bound local storage.
        if (root.length() > 2_000) {
            val pairs = mutableListOf<Pair<String, Long>>()
            val names = root.keys()
            while (names.hasNext()) { val name = names.next(); pairs += name to (root.optJSONObject(name)?.optLong("last", 0L) ?: 0L) }
            pairs.sortedByDescending { it.second }.drop(2_000).forEach { root.remove(it.first) }
        }
        prefs.edit().putString("usage", root.toString()).apply()
    }

    private fun metric(key: String, field: String): Long = runCatching {
        JSONObject(prefs.getString("usage", "{}") ?: "{}").optJSONObject(key)?.optLong(field, 0L) ?: 0L
    }.getOrDefault(0L)

    fun count(key: String): Int = metric(key, "count").coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    fun lastUsed(key: String): Long = metric(key, "last")
    fun recent(apps: List<LauncherApp>, limit: Int = 5): List<LauncherApp> = apps
        .filter { lastUsed(it.key) > 0L }
        .sortedByDescending { lastUsed(it.key) }
        .take(limit)
}

internal fun launcherLabel(app: LauncherApp, config: LauncherConfig): String =
    config.customLabels[app.key]?.takeIf { it.isNotBlank() } ?: app.label

internal fun classifyApp(app: LauncherApp): AppFilter {
    val name = app.label.lowercase()
    val pkg = app.packageName.lowercase()
    if (app.systemCategory == ApplicationInfo.CATEGORY_GAME || listOf("game", "minecraft", "roblox", "free fire", "pubg", "cod mobile").any { name.contains(it) }) {
        return AppFilter.GAMES
    }
    if (listOf("whatsapp", "telegram", "discord", "messenger", "signal", "snapchat", "instagram", "facebook", "reddit", "x.com", "twitter", "phone", "messages", "contacts").any { name.contains(it) || pkg.contains(it.replace(".", "")) }) {
        return AppFilter.SOCIAL
    }
    if (listOf("youtube", "spotify", "music", "photos", "camera", "gallery", "netflix", "prime video", "vlc", "mx player", "twitch", "pinterest").any { name.contains(it) || pkg.contains(it.replace(".", "")) }) {
        return AppFilter.MEDIA
    }
    return AppFilter.TOOLS
}

internal fun iconShapeNameValid(value: String): Boolean = LauncherValidation.isIconShape(value)

internal fun drawableToBitmap(source: Drawable, maxSize: Int = 256): Bitmap {
    val srcWidth = source.intrinsicWidth.takeIf { it > 0 } ?: 96
    val srcHeight = source.intrinsicHeight.takeIf { it > 0 } ?: 96
    val scale = minOf(1f, maxSize.toFloat() / maxOf(srcWidth, srcHeight).toFloat())
    val width = (srcWidth * scale).toInt().coerceAtLeast(1)
    val height = (srcHeight * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    source.setBounds(0, 0, width, height)
    source.draw(canvas)
    return bitmap
}

internal data class IconPackInfo(val packageName: String, val label: String)

/** Discover packs using the launcher-compatible intent actions used by common legacy icon packs. */
internal fun discoverIconPacks(context: Context): List<IconPackInfo> {
    val actions = listOf(
        "org.adw.ActivityStarter.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.dlto.atom.launcher.THEME",
        "com.atom.launcher.THEME"
    )
    val pm = context.packageManager
    return actions.flatMap { action ->
        runCatching {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
    }.mapNotNull { resolveInfo ->
        val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
        if (packageName == context.packageName) return@mapNotNull null
        val label = runCatching { resolveInfo.loadLabel(pm).toString() }.getOrDefault(packageName)
        IconPackInfo(packageName, label)
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private val iconPackMapCache = mutableMapOf<String, Map<String, String>>()

private fun loadIconPackMap(context: Context, packageName: String): Map<String, String> {
    val cacheKey = runCatching {
        "$packageName:${context.packageManager.getPackageInfo(packageName, 0).lastUpdateTime}"
    }.getOrDefault(packageName)
    synchronized(iconPackMapCache) { iconPackMapCache[cacheKey]?.let { return it } }
    val parsed = runCatching {
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId == 0) return@runCatching emptyMap()
        val parser = resources.getXml(xmlId)
        val result = linkedMapOf<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) result[component] = drawable
                }
                event = parser.next()
            }
        } finally {
            parser.close()
        }
        result
    }.getOrDefault(emptyMap())
    synchronized(iconPackMapCache) { iconPackMapCache[cacheKey] = parsed }
    return parsed
}

internal fun loadIconPackDrawable(context: Context, packageName: String, app: LauncherApp): Drawable? {
    if (packageName.isBlank()) return null
    return runCatching {
        val mapping = loadIconPackMap(context, packageName)
        val pkg = app.componentName.packageName
        val className = app.componentName.className
        val shortClass = if (className.startsWith(pkg)) className.removePrefix(pkg).let { if (it.startsWith(".")) it else ".$it" } else className
        val candidates = listOf(
            "ComponentInfo{$pkg/$className}",
            "ComponentInfo{$pkg/$shortClass}",
            "ComponentInfo{${app.componentName.flattenToShortString()}}",
            "ComponentInfo{$pkg}"
        )
        val drawableName = candidates.firstNotNullOfOrNull { mapping[it] } ?: return@runCatching null
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val id = resources.getIdentifier(drawableName, "drawable", packageName).takeIf { it != 0 }
            ?: resources.getIdentifier(drawableName, "mipmap", packageName)
        if (id == 0) return@runCatching null
        val drawable = resources.getDrawable(id, context.theme)
        drawable.constantState?.newDrawable(resources)?.mutate() ?: drawable.mutate()
    }.getOrNull()
}

internal fun backupConfig(config: LauncherConfig): String = JSONObject().apply {
    put("format", "thuku-launcher-backup")
    put("version", 2)
    put("accentArgb", config.accentArgb.toString())
    put("iconScale", config.iconScale.toDouble())
    put("columns", config.columns)
    put("showLabels", config.showLabels)
    put("useGlass", config.useGlass)
    put("useWallpaper", config.useWallpaper)
    put("wallpaperDim", config.wallpaperDim.toDouble())
    put("clockStyle", config.clockStyle)
    put("iconShape", config.iconShape)
    put("iconPackPackage", config.iconPackPackage)
    put("animateUi", config.animateUi)
    put("showDate", config.showDate)
    put("compactHome", config.compactHome)
    put("useDynamicColor", config.useDynamicColor)
    put("customWallpaperUri", config.customWallpaperUri.take(2_000))
    put("drawerLayout", config.drawerLayout)
    put("drawerSort", config.drawerSort)
    put("dockSlots", config.dockSlots)
    put("labelScale", config.labelScale.toDouble())
    put("homeRows", config.homeRows)
    put("workspaceLocked", config.workspaceLocked)
    put("showRecentApps", config.showRecentApps)
    put("weatherEnabled", config.weatherEnabled)
    put("weatherCity", config.weatherCity.take(80))
    put("weatherUnit", config.weatherUnit)
    put("showMusicControls", config.showMusicControls)
    put("swipeUpAction", config.swipeUpAction)
    put("swipeDownAction", config.swipeDownAction)
    put("customLabels", JSONObject().apply { config.customLabels.entries.take(2_000).forEach { (key, value) -> put(key.take(512), value.take(40)) } })
    put("favourites", config.favourites.toList())
    put("homeOrder", config.homeOrder.take(2_000))
    put("dockApps", config.dockApps.take(6))
    put("hidden", config.hidden.toList())
    put("folders", JSONObject().apply { config.folders.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) } })
}.toString(2)

internal fun restoreConfig(json: String, fallback: LauncherConfig): LauncherConfig {
    require(json.length <= 100_000) { "Backup file exceeds the 100 KB size limit." }
    val obj = JSONObject(json)
    require(obj.optString("format") == "thuku-launcher-backup") { "This file is not a Thuku Launcher backup." }
    fun stringSet(key: String): Set<String> {
        val arr = obj.optJSONArray(key) ?: return emptySet()
        return buildSet { for (i in 0 until minOf(arr.length(), 2_000)) { val item = arr.optString(i).take(512); if (item.isNotBlank()) add(item) } }
    }
    val foldersObject = obj.optJSONObject("folders")
    val folders = linkedMapOf<String, Set<String>>()
    if (foldersObject != null) {
        val names = foldersObject.keys()
        while (names.hasNext() && folders.size < 50) {
            val rawName = names.next()
            val name = LauncherValidation.folderName(rawName)
            val array = foldersObject.optJSONArray(rawName) ?: JSONArray()
            val keys = buildSet { for (i in 0 until minOf(array.length(), 200)) { val item = array.optString(i).take(512); if (item.isNotBlank()) add(item) } }
            if (name.isNotBlank() && keys.isNotEmpty()) folders[name] = keys
        }
    }
    return LauncherConfig(
        accentArgb = obj.optString("accentArgb", fallback.accentArgb.toString()).toLongOrNull()?.takeIf { it in 0L..0xFFFFFFFFL } ?: fallback.accentArgb,
        iconScale = LauncherValidation.iconScale(obj.optDouble("iconScale", fallback.iconScale.toDouble()).toFloat(), fallback.iconScale),
        columns = LauncherValidation.columns(obj.optInt("columns", fallback.columns)),
        showLabels = obj.optBoolean("showLabels", fallback.showLabels),
        useGlass = obj.optBoolean("useGlass", fallback.useGlass),
        useWallpaper = obj.optBoolean("useWallpaper", fallback.useWallpaper),
        wallpaperDim = LauncherValidation.wallpaperDim(obj.optDouble("wallpaperDim", fallback.wallpaperDim.toDouble()).toFloat(), fallback.wallpaperDim),
        clockStyle = obj.optString("clockStyle", fallback.clockStyle).takeIf { it in setOf("Editorial", "Minimal", "Mono") } ?: fallback.clockStyle,
        iconShape = obj.optString("iconShape", fallback.iconShape).takeIf(::iconShapeNameValid) ?: fallback.iconShape,
        iconPackPackage = obj.optString("iconPackPackage", fallback.iconPackPackage).take(255),
        animateUi = obj.optBoolean("animateUi", fallback.animateUi),
        showDate = obj.optBoolean("showDate", fallback.showDate),
        compactHome = obj.optBoolean("compactHome", fallback.compactHome),
        useDynamicColor = obj.optBoolean("useDynamicColor", fallback.useDynamicColor),
        customWallpaperUri = obj.optString("customWallpaperUri", fallback.customWallpaperUri).take(2_000),
        drawerLayout = obj.optString("drawerLayout", fallback.drawerLayout).takeIf { it in setOf("Grid", "List") } ?: fallback.drawerLayout,
        drawerSort = obj.optString("drawerSort", fallback.drawerSort).takeIf { it in setOf("Alphabetical", "Most used", "Recently used", "Package name") } ?: fallback.drawerSort,
        dockSlots = obj.optInt("dockSlots", fallback.dockSlots).coerceIn(3, 7),
        labelScale = obj.optDouble("labelScale", fallback.labelScale.toDouble()).toFloat().coerceIn(0.8f, 1.4f),
        homeRows = obj.optInt("homeRows", fallback.homeRows).coerceIn(2, 5),
        workspaceLocked = obj.optBoolean("workspaceLocked", fallback.workspaceLocked),
        showRecentApps = obj.optBoolean("showRecentApps", fallback.showRecentApps),
        weatherEnabled = obj.optBoolean("weatherEnabled", fallback.weatherEnabled),
        weatherCity = obj.optString("weatherCity", fallback.weatherCity).take(80),
        weatherUnit = obj.optString("weatherUnit", fallback.weatherUnit).takeIf { it == "C" || it == "F" } ?: fallback.weatherUnit,
        showMusicControls = obj.optBoolean("showMusicControls", fallback.showMusicControls),
        swipeUpAction = obj.optString("swipeUpAction", fallback.swipeUpAction).takeIf { it in launcherGestureActions } ?: fallback.swipeUpAction,
        swipeDownAction = obj.optString("swipeDownAction", fallback.swipeDownAction).takeIf { it in launcherGestureActions } ?: fallback.swipeDownAction,
        customLabels = runCatching {
            val labelsObject = obj.optJSONObject("customLabels") ?: JSONObject()
            buildMap { val keys = labelsObject.keys(); while (keys.hasNext() && size < 2_000) { val key = keys.next().take(512); val value = labelsObject.optString(key).filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(40); if (key.isNotBlank() && value.isNotBlank()) put(key, value) } }
        }.getOrDefault(emptyMap()),
        favourites = stringSet("favourites"),
        homeOrder = (obj.optJSONArray("homeOrder")?.let { arr -> (0 until minOf(arr.length(), 2_000)).mapNotNull { arr.optString(it).take(512).takeIf(String::isNotBlank) } } ?: emptyList()).distinct(),
        dockApps = (obj.optJSONArray("dockApps")?.let { arr -> (0 until minOf(arr.length(), 7)).mapNotNull { arr.optString(it).take(512).takeIf(String::isNotBlank) } } ?: emptyList()).distinct(),
        hidden = stringSet("hidden"),
        folders = folders
    )
}
