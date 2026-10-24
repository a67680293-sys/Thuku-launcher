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
    val favourites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val folders: Map<String, Set<String>> = emptyMap()
) {
    fun surfaceColor(): Color = if (useGlass) Color(0xAA151922) else Color(0xFF151922)
}

internal class LauncherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("thuku_launcher_preferences", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = LauncherConfig(
        accentArgb = prefs.getLong("accent", accentPresets.first().argb),
        iconScale = prefs.getFloat("icon_scale", 1.0f).coerceIn(0.7f, 1.35f),
        columns = prefs.getInt("columns", 4).coerceIn(3, 6),
        showLabels = prefs.getBoolean("show_labels", true),
        useGlass = prefs.getBoolean("glass", true),
        useWallpaper = prefs.getBoolean("wallpaper", true),
        wallpaperDim = prefs.getFloat("wallpaper_dim", 0.48f).coerceIn(0f, 0.8f),
        clockStyle = prefs.getString("clock_style", "Editorial") ?: "Editorial",
        iconShape = prefs.getString("icon_shape", "Squircle") ?: "Squircle",
        iconPackPackage = prefs.getString("icon_pack_package", "") ?: "",
        animateUi = prefs.getBoolean("animate_ui", true),
        showDate = prefs.getBoolean("show_date", true),
        compactHome = prefs.getBoolean("compact_home", false),
        favourites = split(prefs.getString("favourites", "")),
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
            .putString("favourites", config.favourites.joinToString("\n"))
            .putString("hidden", config.hidden.joinToString("\n"))
            .putString("folders", encodeFolders(config.folders))
            .apply()
    }

    private fun encodeFolders(folders: Map<String, Set<String>>): String = JSONObject().apply {
        folders.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) }
    }.toString()

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

internal fun iconShapeNameValid(value: String): Boolean = value in setOf("Circle", "Squircle", "Rounded", "Square")

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
    put("version", 1)
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
    put("favourites", config.favourites.toList())
    put("hidden", config.hidden.toList())
    put("folders", JSONObject().apply { config.folders.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) } })
}.toString(2)

internal fun restoreConfig(json: String, fallback: LauncherConfig): LauncherConfig {
    val obj = JSONObject(json)
    require(obj.optString("format") == "thuku-launcher-backup") { "This file is not a Thuku Launcher backup." }
    fun stringSet(key: String): Set<String> {
        val arr = obj.optJSONArray(key) ?: return emptySet()
        return buildSet { for (i in 0 until arr.length()) { val item = arr.optString(i); if (item.isNotBlank()) add(item) } }
    }
    val foldersObject = obj.optJSONObject("folders")
    val folders = linkedMapOf<String, Set<String>>()
    if (foldersObject != null) {
        val names = foldersObject.keys()
        while (names.hasNext()) {
            val name = names.next()
            val array = foldersObject.optJSONArray(name) ?: JSONArray()
            val keys = buildSet { for (i in 0 until array.length()) { val item = array.optString(i); if (item.isNotBlank()) add(item) } }
            if (name.isNotBlank() && keys.isNotEmpty()) folders[name] = keys
        }
    }
    return LauncherConfig(
        accentArgb = obj.optString("accentArgb", fallback.accentArgb.toString()).toLongOrNull() ?: fallback.accentArgb,
        iconScale = obj.optDouble("iconScale", fallback.iconScale.toDouble()).toFloat().coerceIn(0.7f, 1.35f),
        columns = obj.optInt("columns", fallback.columns).coerceIn(3, 6),
        showLabels = obj.optBoolean("showLabels", fallback.showLabels),
        useGlass = obj.optBoolean("useGlass", fallback.useGlass),
        useWallpaper = obj.optBoolean("useWallpaper", fallback.useWallpaper),
        wallpaperDim = obj.optDouble("wallpaperDim", fallback.wallpaperDim.toDouble()).toFloat().coerceIn(0f, 0.8f),
        clockStyle = obj.optString("clockStyle", fallback.clockStyle),
        iconShape = obj.optString("iconShape", fallback.iconShape).takeIf(::iconShapeNameValid) ?: fallback.iconShape,
        iconPackPackage = obj.optString("iconPackPackage", fallback.iconPackPackage),
        animateUi = obj.optBoolean("animateUi", fallback.animateUi),
        showDate = obj.optBoolean("showDate", fallback.showDate),
        compactHome = obj.optBoolean("compactHome", fallback.compactHome),
        favourites = stringSet("favourites"),
        hidden = stringSet("hidden"),
        folders = folders
    )
}
