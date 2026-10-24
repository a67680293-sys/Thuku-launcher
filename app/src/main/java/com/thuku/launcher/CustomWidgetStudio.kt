@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.thuku.launcher

import android.app.ActivityManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.delay

/** A deliberately finite widget catalogue: users can compose useful home cards without running downloaded code. */
internal enum class CustomWidgetKind(val title: String, val symbol: String, val description: String, val group: String) {
    DIGITAL_CLOCK("Digital clock", "◷", "Live clock with seconds", "Time"),
    ANALOG_CLOCK("Analog clock", "◉", "A live analog clock face", "Time"),
    DATE_WEEK("Date & week", "▦", "Today, weekday and week number", "Time"),
    WORLD_CLOCK("World clocks", "◎", "Four live city times", "Time"),
    MONTH_CALENDAR("Month calendar", "▤", "Compact monthly date grid", "Time"),
    COUNTDOWN("Countdown", "⌛", "Countdown to a chosen date and time", "Time"),
    TIMER("Timer", "◴", "Configurable countdown timer", "Time"),
    STOPWATCH("Stopwatch", "⏱", "Start, pause and reset stopwatch", "Time"),
    POMODORO("Pomodoro", "◉", "25-minute focus timer", "Focus"),
    FOCUS_GOAL("Focus goal", "✦", "Track today's focus sessions", "Focus"),
    BATTERY("Battery level", "▰", "Live battery percentage", "Device"),
    BATTERY_DETAILS("Battery details", "ϟ", "Charging state and temperature", "Device"),
    STORAGE("Storage", "▥", "Internal storage usage", "Device"),
    MEMORY("Memory snapshot", "▦", "Available and total RAM", "Device"),
    DEVICE_INFO("Device info", "▣", "Model, Android version and API", "Device"),
    NETWORK("Connection status", "⌁", "Wi-Fi, mobile data or offline", "Shortcuts"),
    WIFI_SETTINGS("Wi-Fi shortcut", "◉", "Open Android Wi-Fi settings", "Shortcuts"),
    BLUETOOTH_SETTINGS("Bluetooth shortcut", "ᛒ", "Open Android Bluetooth settings", "Shortcuts"),
    DISPLAY_SETTINGS("Display shortcut", "☼", "Open Android display settings", "Shortcuts"),
    QUICK_SETTINGS("System settings", "⚙", "Open Android settings", "Shortcuts"),
    WEATHER_SHORTCUT("Weather shortcut", "☁", "Open a weather search or app", "Shortcuts"),
    MUSIC_SHORTCUT("Music shortcut", "♫", "Open an installed music app", "Shortcuts"),
    DIGITAL_WELLBEING("Digital wellbeing", "◷", "Open usage-access settings", "Shortcuts"),
    QUOTE("Daily thought", "❝", "An offline rotating quote", "Personal"),
    NOTES("Quick notes", "✎", "Private notes saved on this phone", "Personal"),
    TASKS("Task list", "☑", "A persistent tap-to-complete checklist", "Personal"),
    HABIT("Daily habit", "◆", "Check off one daily habit", "Personal"),
    WATER("Water tracker", "≈", "Keep a simple daily water count", "Personal"),
    FAVOURITES("Pinned apps", "★", "Shortcut to your pinned apps", "Apps"),
    APP_COUNT("App library stats", "▦", "Count visible apps and folders", "Apps"),
    APP_SEARCH("App search", "⌕", "Jump straight to the app library", "Apps")
}

internal data class CustomWidgetItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: CustomWidgetKind,
    val title: String = kind.title,
    val accentArgb: Long = 0xFFC9F56AL,
    val content: String = "",
    val counter: Int = 0,
    val doneToday: Boolean = false,
    val dailyStamp: String = LocalDate.now().toString(),
    val targetAtMillis: Long = 0L,
    val timerSeconds: Int = when (kind) { CustomWidgetKind.POMODORO -> 25 * 60; CustomWidgetKind.TIMER -> 5 * 60; else -> 0 },
    val elapsedSeconds: Int = 0,
    val running: Boolean = false,
    val compact: Boolean = false
)

internal class CustomWidgetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("thuku_custom_widgets", Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<CustomWidgetItem> {
        val raw = prefs.getString("items", "[]").orEmpty()
        var dailyResetNeeded = false
        val parsed = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until minOf(array.length(), 30)) {
                    val obj = array.optJSONObject(index) ?: continue
                    val kind = runCatching { CustomWidgetKind.valueOf(obj.optString("kind")) }.getOrNull() ?: continue
                    val stamp = obj.optString("dailyStamp", "")
                    var item = CustomWidgetItem(
                        id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                        kind = kind,
                        title = safeWidgetTitle(obj.optString("title", kind.title)),
                        accentArgb = parseThemeHexColor(obj.optString("accent", "#C9F56A")) ?: 0xFFC9F56AL,
                        content = obj.optString("content", "").take(2_000),
                        counter = obj.optInt("counter", 0).coerceIn(0, 999),
                        doneToday = obj.optBoolean("doneToday", false),
                        dailyStamp = stamp,
                        targetAtMillis = obj.optLong("targetAtMillis", 0L).coerceAtLeast(0L),
                        timerSeconds = obj.optInt("timerSeconds", defaultSeconds(kind)).coerceIn(0, 24 * 60 * 60),
                        elapsedSeconds = obj.optInt("elapsedSeconds", 0).coerceIn(0, 7 * 24 * 60 * 60),
                        running = obj.optBoolean("running", false),
                        compact = obj.optBoolean("compact", false)
                    )
                    if ((kind == CustomWidgetKind.HABIT || kind == CustomWidgetKind.WATER || kind == CustomWidgetKind.FOCUS_GOAL) && stamp != LocalDate.now().toString()) {
                        item = item.copy(counter = 0, doneToday = false, dailyStamp = LocalDate.now().toString())
                        dailyResetNeeded = true
                    }
                    add(item)
                }
            }
        }.getOrDefault(emptyList())
        if (dailyResetNeeded) save(parsed)
        return parsed
    }

    @Synchronized
    fun save(items: List<CustomWidgetItem>) {
        val array = JSONArray()
        items.take(30).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id.take(80))
                put("kind", item.kind.name)
                put("title", safeWidgetTitle(item.title))
                put("accent", colorToHex(item.accentArgb))
                put("content", item.content.take(2_000))
                put("counter", item.counter.coerceIn(0, 999))
                put("doneToday", item.doneToday)
                put("dailyStamp", item.dailyStamp)
                put("targetAtMillis", item.targetAtMillis.coerceAtLeast(0L))
                put("timerSeconds", item.timerSeconds.coerceIn(0, 24 * 60 * 60))
                put("elapsedSeconds", item.elapsedSeconds.coerceIn(0, 7 * 24 * 60 * 60))
                put("running", item.running)
                put("compact", item.compact)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun add(kind: CustomWidgetKind): List<CustomWidgetItem> {
        val current = load()
        if (current.size >= 30) return current
        val created = CustomWidgetItem(kind = kind, targetAtMillis = if (kind == CustomWidgetKind.COUNTDOWN) nextNewYearMillis() else 0L)
        return (current + created).also(::save)
    }

    fun update(item: CustomWidgetItem): List<CustomWidgetItem> = load().map { if (it.id == item.id) item else it }.also(::save)
    fun remove(id: String): List<CustomWidgetItem> = load().filterNot { it.id == id }.also(::save)

    companion object {
        fun defaultSeconds(kind: CustomWidgetKind): Int = when (kind) {
            CustomWidgetKind.POMODORO -> 25 * 60
            CustomWidgetKind.TIMER -> 5 * 60
            else -> 0
        }
        private fun nextNewYearMillis(): Long = Calendar.getInstance().apply {
            add(Calendar.YEAR, 1)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

private fun safeWidgetTitle(raw: String): String = raw.filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(36).ifBlank { "Widget" }

/** Reads no more than 20,001 bytes so a huge selected file cannot exhaust memory. */
private fun readThemeInputBounded(input: InputStream): String = input.use { stream ->
    val output = ByteArrayOutputStream(20_001)
    val buffer = ByteArray(4_096)
    var total = 0
    while (total <= 20_000) {
        val count = stream.read(buffer, 0, minOf(buffer.size, 20_001 - total))
        if (count <= 0) break
        output.write(buffer, 0, count)
        total += count
    }
    require(total <= 20_000) { "Theme file exceeds 20 KB." }
    output.toByteArray().toString(Charsets.UTF_8)
}

internal fun parseThemeHexColor(raw: String): Long? {
    val text = raw.trim().removePrefix("#")
    if (text.length != 6 && text.length != 8) return null
    val parsed = text.toLongOrNull(16) ?: return null
    return if (text.length == 6) (0xFF000000L or parsed) else parsed
}

internal fun colorToHex(argb: Long): String {
    val value = argb.toInt()
    return if ((value ushr 24) == 0xFF) "#%06X".format(Locale.US, value and 0xFFFFFF)
    else "#%08X".format(Locale.US, value)
}

internal object ThemeCodeCodec {
    fun encode(config: LauncherConfig, name: String = "My Thuku Theme"): String = JSONObject().apply {
        put("format", "thuku-theme-v1")
        put("name", safeWidgetTitle(name))
        put("accent", colorToHex(config.accentArgb))
        put("iconScale", config.iconScale.toDouble())
        put("columns", config.columns)
        put("showLabels", config.showLabels)
        put("glass", config.useGlass)
        put("wallpaper", config.useWallpaper)
        put("wallpaperDim", config.wallpaperDim.toDouble())
        put("clockStyle", config.clockStyle)
        put("iconShape", config.iconShape)
        put("animations", config.animateUi)
        put("showDate", config.showDate)
        put("compactHome", config.compactHome)
    }.toString(2)

    fun apply(raw: String, fallback: LauncherConfig): LauncherConfig {
        require(raw.length <= 20_000) { "Theme code is too large (maximum 20 KB)." }
        val json = JSONObject(raw)
        require(json.optString("format") == "thuku-theme-v1") { "Unsupported theme code. Start with the thuku-theme-v1 template." }
        val accent = parseThemeHexColor(json.optString("accent", colorToHex(fallback.accentArgb)))
            ?: throw IllegalArgumentException("accent must be a hex color such as #C9F56A or #FFC9F56A.")
        val clock = json.optString("clockStyle", fallback.clockStyle).takeIf { it in setOf("Editorial", "Minimal", "Mono") } ?: fallback.clockStyle
        val shape = json.optString("iconShape", fallback.iconShape).takeIf { LauncherValidation.isIconShape(it) } ?: fallback.iconShape
        val scale = json.optDouble("iconScale", fallback.iconScale.toDouble()).toFloat()
        val dim = json.optDouble("wallpaperDim", fallback.wallpaperDim.toDouble()).toFloat()
        require(scale.isFinite() && dim.isFinite()) { "iconScale and wallpaperDim must be finite numbers." }
        return fallback.copy(
            accentArgb = accent,
            iconScale = LauncherValidation.iconScale(scale, fallback.iconScale),
            columns = LauncherValidation.columns(json.optInt("columns", fallback.columns)),
            showLabels = json.optBoolean("showLabels", fallback.showLabels),
            useGlass = json.optBoolean("glass", fallback.useGlass),
            useWallpaper = json.optBoolean("wallpaper", fallback.useWallpaper),
            wallpaperDim = LauncherValidation.wallpaperDim(dim, fallback.wallpaperDim),
            clockStyle = clock,
            iconShape = shape,
            animateUi = json.optBoolean("animations", fallback.animateUi),
            showDate = json.optBoolean("showDate", fallback.showDate),
            compactHome = json.optBoolean("compactHome", fallback.compactHome)
        )
    }
}

@Composable
internal fun ThemeCodeStudio(config: LauncherConfig, onApply: (LauncherConfig) -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(ThemeCodeCodec.encode(config)) }
    var appliedName by remember { mutableStateOf("My Thuku Theme") }
    val exportTheme = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            val output = context.contentResolver.openOutputStream(uri) ?: error("Could not create the theme file.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(code) }
        }.onFailure { Toast.makeText(context, it.message ?: "Could not export theme.", Toast.LENGTH_LONG).show() }
    }
    val importTheme = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: error("Could not read that file.")
            readThemeInputBounded(input)
        }.onSuccess { imported -> code = imported }
            .onFailure { Toast.makeText(context, it.message ?: "Could not import theme.", Toast.LENGTH_LONG).show() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Write a theme as JSON settings code. Thuku validates and applies the values; it never executes Kotlin, JavaScript, or downloaded code.", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 20_000) code = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
            minLines = 8,
            maxLines = 15,
            label = { Text("Theme code · JSON") },
            supportingText = { Text("Format: thuku-theme-v1 · max 20 KB", color = Color.White.copy(alpha = 0.5f)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                runCatching {
                    val parsed = JSONObject(code)
                    appliedName = safeWidgetTitle(parsed.optString("name", "My Thuku Theme"))
                    ThemeCodeCodec.apply(code, config)
                }.onSuccess { updated ->
                    onApply(updated)
                    code = ThemeCodeCodec.encode(updated, appliedName)
                    Toast.makeText(context, "Theme '$appliedName' applied.", Toast.LENGTH_SHORT).show()
                }.onFailure { Toast.makeText(context, it.message ?: "Theme code is invalid.", Toast.LENGTH_LONG).show() }
            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Validate & apply") }
            OutlinedButton(onClick = { importTheme.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Import JSON") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportTheme.launch("thuku-theme.json") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Export JSON") }
            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Thuku theme code", code))
                Toast.makeText(context, if (clipboard != null) "Theme code copied." else "Clipboard unavailable.", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Copy code") }
        }
        TextButton(onClick = { code = ThemeCodeCodec.encode(config); appliedName = "My Thuku Theme" }) { Text("Reset to current settings") }
    }
}

@Composable
internal fun CustomWidgetCatalogDialog(onDismiss: () -> Unit, onAdd: (CustomWidgetKind) -> Unit, currentCount: Int) {
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("All") }
    val groups = listOf("All", "Time", "Focus", "Device", "Shortcuts", "Personal", "Apps")
    val kinds = CustomWidgetKind.entries.filter { (group == "All" || it.group == group) && (query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Widget studio"); Text("${CustomWidgetKind.entries.size} built-in widgets · $currentCount added", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f)) } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 550.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it.take(80) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Find a widget") })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups.forEach { g ->
                        val selected = group == g
                        Surface(onClick = { group = g }, shape = RoundedCornerShape(30.dp), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))) {
                            Text(g, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    kinds.forEach { kind ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.04f), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(kind.symbol, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp) }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(kind.title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(kind.description, color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(enabled = currentCount < 30, onClick = { onAdd(kind) }) { Text("Add") }
                            }
                        }
                    }
                    if (kinds.isEmpty()) Text("No widgets match that search.", color = Color.White.copy(alpha = 0.65f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        containerColor = Color(0xFF151922), titleContentColor = Color.White, textContentColor = Color.White
    )
}

@Composable
internal fun CustomWidgetCard(
    item: CustomWidgetItem,
    config: LauncherConfig,
    appCount: Int,
    favouriteCount: Int,
    folderCount: Int,
    onUpdate: (CustomWidgetItem) -> Unit,
    onRemove: () -> Unit,
    onOpenApps: () -> Unit
) {
    val context = LocalContext.current
    var tick by remember(item.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var editing by remember(item.id) { mutableStateOf(false) }
    var titleDraft by remember(item.id) { mutableStateOf(item.title) }
    var colorDraft by remember(item.id) { mutableStateOf(colorToHex(item.accentArgb)) }
    var taskDraft by remember(item.id) { mutableStateOf("") }

    val countdownActive = item.kind == CustomWidgetKind.COUNTDOWN && item.targetAtMillis > System.currentTimeMillis()
    val tickEverySecond = item.kind in setOf(CustomWidgetKind.DIGITAL_CLOCK, CustomWidgetKind.ANALOG_CLOCK) || countdownActive || item.running
    val periodicRefresh = tickEverySecond || item.kind in setOf(
        CustomWidgetKind.DATE_WEEK, CustomWidgetKind.WORLD_CLOCK, CustomWidgetKind.MONTH_CALENDAR,
        CustomWidgetKind.BATTERY, CustomWidgetKind.BATTERY_DETAILS, CustomWidgetKind.STORAGE,
        CustomWidgetKind.MEMORY, CustomWidgetKind.NETWORK
    )
    LaunchedEffect(item.id, item.running, item.targetAtMillis, item.kind, periodicRefresh) {
        tick = System.currentTimeMillis()
        if (!periodicRefresh) return@LaunchedEffect
        while (true) {
            tick = System.currentTimeMillis()
            if (item.running && item.kind in setOf(CustomWidgetKind.TIMER, CustomWidgetKind.POMODORO) && item.targetAtMillis > 0L && tick >= item.targetAtMillis) {
                onUpdate(item.copy(running = false, timerSeconds = 0, targetAtMillis = 0L))
                Toast.makeText(context, "${item.title} complete.", Toast.LENGTH_LONG).show()
                break
            }
            delay(if (tickEverySecond) 1_000L else 30_000L)
        }
    }

    val accent = Color(item.accentArgb)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (item.compact) 20.dp else 26.dp),
        colors = CardDefaults.cardColors(containerColor = if (config.useGlass) config.surfaceColor() else Color(0xFF151922)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.23f))
    ) {
        Column(Modifier.fillMaxWidth().padding(if (item.compact) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) { Text(item.kind.symbol, color = accent, fontSize = 20.sp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.kind.group.uppercase(Locale.ROOT), color = accent, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                }
                TextButton(onClick = { titleDraft = item.title; colorDraft = colorToHex(item.accentArgb); editing = true }) { Text("Edit", color = Color.White.copy(alpha = 0.72f)) }
                TextButton(onClick = onRemove) { Text("×", color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp) }
            }

            when (item.kind) {
                CustomWidgetKind.DIGITAL_CLOCK -> {
                    val fmt = SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm:ss" else "h:mm:ss a", Locale.getDefault())
                    Text(fmt.format(Date(tick)), color = Color.White, fontSize = if (item.compact) 30.sp else 38.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Monospace)
                    Text(SimpleDateFormat("EEEE · d MMMM", Locale.getDefault()).format(Date(tick)), color = Color.White.copy(alpha = 0.6f))
                }
                CustomWidgetKind.ANALOG_CLOCK -> AnalogClockFace(nowMillis = tick, accent = accent)
                CustomWidgetKind.DATE_WEEK -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = tick }
                    Text(SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(tick)), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(tick)), color = Color.White, fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.Light)
                        Column(Modifier.padding(bottom = 7.dp)) { Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(tick)), color = Color.White); Text("Week ${cal.get(Calendar.WEEK_OF_YEAR)}", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall) }
                    }
                }
                CustomWidgetKind.WORLD_CLOCK -> {
                    listOf("New Delhi" to "Asia/Kolkata", "London" to "Europe/London", "Tokyo" to "Asia/Tokyo", "New York" to "America/New_York").forEach { (city, zone) ->
                        val cityTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone(zone) }.format(Date(tick))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(city, color = Color.White.copy(alpha = 0.75f)); Text(cityTime, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium) }
                    }
                }
                CustomWidgetKind.MONTH_CALENDAR -> MonthCalendarContent(tick = tick, accent = accent)
                CustomWidgetKind.COUNTDOWN -> {
                    val target = item.targetAtMillis.takeIf { it > 0L } ?: nextNewYear()
                    val hasPassed = target <= tick
                    val remaining = ((target - tick).coerceAtLeast(0L) / 1_000L)
                    Text(if (item.content.isBlank()) "Countdown to target" else item.content.take(36), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
                    Text(if (hasPassed) "00:00:00" else formatCountdown(remaining), color = accent, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                    if (hasPassed) Text("That target date has passed. Choose a new date.", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        val initial = Calendar.getInstance().apply { timeInMillis = target }
                        DatePickerDialog(context, { _, y, m, d ->
                            TimePickerDialog(context, { _, h, min ->
                                val selected = Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                                onUpdate(item.copy(targetAtMillis = selected, content = item.content.ifBlank { "Countdown target" }))
                            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(context)).show()
                        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Choose date & time") }
                }
                CustomWidgetKind.TIMER, CustomWidgetKind.POMODORO -> {
                    val remaining = if (item.running && item.targetAtMillis > 0L) ((item.targetAtMillis - tick).coerceAtLeast(0L) / 1_000L).toInt() else item.timerSeconds
                    Text(formatDuration(remaining), color = accent, fontSize = 39.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                    if (item.kind == CustomWidgetKind.TIMER) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(60, 5 * 60, 15 * 60, 25 * 60, 45 * 60, 60 * 60).forEach { seconds ->
                                TextButton(onClick = { onUpdate(item.copy(timerSeconds = seconds, running = false, targetAtMillis = 0L)) }) { Text(if (seconds < 3600) "${seconds / 60}m" else "1h", color = accent) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Custom length", modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { onUpdate(item.copy(timerSeconds = (remaining - 60).coerceAtLeast(60), running = false, targetAtMillis = 0L)) }, enabled = remaining > 60) { Text("−1m") }
                            OutlinedButton(onClick = { onUpdate(item.copy(timerSeconds = (remaining + 60).coerceAtMost(24 * 60 * 60), running = false, targetAtMillis = 0L)) }, enabled = remaining < 24 * 60 * 60) { Text("+1m") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (item.running) onUpdate(item.copy(timerSeconds = remaining, running = false, targetAtMillis = 0L))
                            else {
                                val seconds = if (remaining > 0) remaining else CustomWidgetStore.defaultSeconds(item.kind)
                                onUpdate(item.copy(timerSeconds = seconds, targetAtMillis = tick + seconds * 1_000L, running = true))
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF10130B))) { Text(if (item.running) "Pause" else "Start") }
                        OutlinedButton(onClick = { onUpdate(item.copy(timerSeconds = CustomWidgetStore.defaultSeconds(item.kind), running = false, targetAtMillis = 0L)) }) { Text("Reset") }
                    }
                }
                CustomWidgetKind.STOPWATCH -> {
                    val elapsed = item.elapsedSeconds + if (item.running && item.targetAtMillis > 0L) ((tick - item.targetAtMillis).coerceAtLeast(0L) / 1_000L).toInt() else 0
                    Text(formatDuration(elapsed), color = accent, fontSize = 39.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (item.running) onUpdate(item.copy(elapsedSeconds = elapsed, targetAtMillis = 0L, running = false)) else onUpdate(item.copy(targetAtMillis = tick, running = true)) }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF10130B))) { Text(if (item.running) "Pause" else "Start") }
                        OutlinedButton(onClick = { onUpdate(item.copy(elapsedSeconds = 0, targetAtMillis = 0L, running = false)) }) { Text("Reset") }
                    }
                }
                CustomWidgetKind.BATTERY, CustomWidgetKind.BATTERY_DETAILS -> {
                    val battery = readBattery(context)
                    Text(if (battery.first >= 0) "${battery.first}%" else "—", color = accent, fontSize = 42.sp, fontWeight = FontWeight.Light)
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) { Box(Modifier.fillMaxWidth((battery.first / 100f).coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(accent)) }
                    if (item.kind == CustomWidgetKind.BATTERY_DETAILS) Text("${battery.second} · ${battery.third?.let { "$it°C" } ?: "temperature unavailable"}", color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                    else Text(battery.second, color = Color.White.copy(alpha = 0.63f), style = MaterialTheme.typography.bodySmall)
                }
                CustomWidgetKind.STORAGE -> {
                    val stat = runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull()
                    val total = stat?.totalBytes ?: 0L
                    val free = stat?.availableBytes ?: 0L
                    val used = (total - free).coerceAtLeast(0L)
                    Text(if (total > 0) "${formatBytes(used)} used" else "Storage unavailable", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                    Text("${formatBytes(free)} available of ${formatBytes(total)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) { Box(Modifier.fillMaxWidth(if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f).height(7.dp).clip(CircleShape).background(accent)) }
                    TextButton(onClick = { launchIntent(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }) { Text("Open storage settings", color = accent) }
                }
                CustomWidgetKind.MEMORY -> {
                    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    val mi = ActivityManager.MemoryInfo().also { runCatching { manager?.getMemoryInfo(it) } }
                    Text("${formatBytes(mi.availMem)} free", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text("of ${formatBytes(mi.totalMem)} total RAM", color = Color.White.copy(alpha = 0.6f))
                    Text("System snapshot · not a task killer", color = accent, style = MaterialTheme.typography.labelSmall)
                }
                CustomWidgetKind.DEVICE_INFO -> {
                    Text(Build.MODEL.ifBlank { "Android device" }, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                    Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", color = Color.White.copy(alpha = 0.65f))
                    Text("${Build.MANUFACTURER} · ${Build.DEVICE}", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                CustomWidgetKind.NETWORK -> {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val capabilities = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
                    val status = when {
                        capabilities == null -> "Offline or unavailable"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected · Wi-Fi"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected · Mobile data"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected · Ethernet"
                        else -> "Connected · Other network"
                    }
                    Text(status, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text("Status may take a moment to refresh after system changes.", color = Color.White.copy(alpha = 0.57f), style = MaterialTheme.typography.bodySmall)
                }
                CustomWidgetKind.WIFI_SETTINGS -> ShortcutBody("Open Android Wi-Fi controls", "Open Wi-Fi settings") { launchIntent(context, Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.BLUETOOTH_SETTINGS -> ShortcutBody("Pair accessories and manage Bluetooth", "Open Bluetooth settings") { launchIntent(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.DISPLAY_SETTINGS -> ShortcutBody("Adjust brightness, text and display options", "Open display settings") { launchIntent(context, Intent(Settings.ACTION_DISPLAY_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.QUICK_SETTINGS -> ShortcutBody("Use Android's own permission-protected controls", "Open system settings") { launchIntent(context, Intent(Settings.ACTION_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.WEATHER_SHORTCUT -> ShortcutBody("No fake forecast or network data is shown here.", "Find local weather") { launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather")), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.MUSIC_SHORTCUT -> ShortcutBody("Jump to a music app installed on your phone.", "Open music app") { launchIntent(context, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC), Intent(Settings.ACTION_SOUND_SETTINGS)) }
                CustomWidgetKind.DIGITAL_WELLBEING -> ShortcutBody("Android may ask you to grant Usage Access.", "Open usage access") { launchIntent(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), Intent(Settings.ACTION_SETTINGS)) }
                CustomWidgetKind.QUOTE -> {
                    val quotes = listOf("Small steps still move you forward.", "Make your systems stronger than your moods.", "Consistency makes difficult things feel lighter.", "Protect your attention; it shapes your days.", "Progress is built from ordinary repetitions.", "You can begin again with the next choice.", "Clarity often comes after you start.", "Rest is part of a good plan.")
                    Text("“${quotes[(LocalDate.now().dayOfYear % quotes.size)]}”", color = Color.White, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Light)
                    Text("OFFLINE · DAILY THOUGHT", color = accent, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                }
                CustomWidgetKind.NOTES -> {
                    Text("Saved only in Thuku's private app storage.", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = item.content, onValueChange = { onUpdate(item.copy(content = it.take(2_000))) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 6, label = { Text("Your note") })
                }
                CustomWidgetKind.TASKS -> {
                    val tasks = item.content.lines().filter { it.isNotBlank() }.take(20)
                    if (tasks.isEmpty()) Text("Add a small task to get started.", color = Color.White.copy(alpha = 0.6f))
                    tasks.forEachIndexed { index, task ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            val updated = tasks.toMutableList()
                            updated[index] = if (task.startsWith("[x] ", true)) "[ ] ${task.drop(4)}" else "[x] ${task.removePrefix("[ ] ").removePrefix("[x] ")}"
                            onUpdate(item.copy(content = updated.joinToString("\n")))
                        }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = task.startsWith("[x] ", true), onCheckedChange = {
                                val updated = tasks.toMutableList()
                                updated[index] = if (it) "[x] ${task.removePrefix("[ ] ").removePrefix("[x] ")}" else "[ ] ${task.removePrefix("[ ] ").removePrefix("[x] ")}"
                                onUpdate(item.copy(content = updated.joinToString("\n")))
                            })
                            Text(task.removePrefix("[ ] ").removePrefix("[x] "), color = Color.White.copy(alpha = if (task.startsWith("[x] ", true)) 0.45f else 0.88f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = taskDraft, onValueChange = { taskDraft = it.take(80) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("New task") })
                        Button(enabled = taskDraft.isNotBlank() && tasks.size < 20, onClick = { onUpdate(item.copy(content = (tasks + "[ ] ${taskDraft.trim()}").joinToString("\n"))); taskDraft = "" }) { Text("Add") }
                    }
                }
                CustomWidgetKind.HABIT -> {
                    Text(item.content.ifBlank { "Move your daily habit forward." }, color = Color.White.copy(alpha = 0.68f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.doneToday, onCheckedChange = { onUpdate(item.copy(doneToday = it, dailyStamp = LocalDate.now().toString())) })
                        Text(if (item.doneToday) "Done for today ✓" else "Mark today's habit complete", color = if (item.doneToday) accent else Color.White)
                    }
                    OutlinedButton(onClick = { val replacement = if (item.content.isBlank()) "My daily habit" else ""; onUpdate(item.copy(content = replacement)) }) { Text(if (item.content.isBlank()) "Set default habit label" else "Clear habit label") }
                }
                CustomWidgetKind.WATER -> {
                    Text("${item.counter} glasses today", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onUpdate(item.copy(counter = (item.counter - 1).coerceAtLeast(0), dailyStamp = LocalDate.now().toString())) }, enabled = item.counter > 0) { Text("−") }
                        Button(onClick = { onUpdate(item.copy(counter = (item.counter + 1).coerceAtMost(99), dailyStamp = LocalDate.now().toString())) }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF10130B))) { Text("+ Add glass") }
                        TextButton(onClick = { onUpdate(item.copy(counter = 0, dailyStamp = LocalDate.now().toString())) }) { Text("Reset", color = Color.White.copy(alpha = 0.7f)) }
                    }
                    Text("A manual tracker, not a medical recommendation.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
                CustomWidgetKind.FOCUS_GOAL -> {
                    Text("${item.counter} focus sessions today", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                    Text("Log a completed session manually.", color = Color.White.copy(alpha = 0.6f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdate(item.copy(counter = (item.counter + 1).coerceAtMost(99), dailyStamp = LocalDate.now().toString())) }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF10130B))) { Text("+ Completed") }
                        OutlinedButton(onClick = { onUpdate(item.copy(counter = 0, dailyStamp = LocalDate.now().toString())) }) { Text("Reset") }
                    }
                }
                CustomWidgetKind.FAVOURITES -> {
                    Text("$favouriteCount pinned app${if (favouriteCount == 1) "" else "s"}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text("Keep your go-to apps close.", color = Color.White.copy(alpha = 0.62f))
                    FilledTonalButton(onClick = onOpenApps) { Text("Manage pinned apps") }
                }
                CustomWidgetKind.APP_COUNT -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        MetricPill("$appCount", "Visible apps", accent)
                        MetricPill("$folderCount", "Folders", accent)
                    }
                    Text("Counts update when Thuku refreshes the app library.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                CustomWidgetKind.APP_SEARCH -> ShortcutBody("Search apps, package names and installed launchers.", "Open app library") { onOpenApps() }
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Customize widget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = titleDraft, onValueChange = { titleDraft = it.take(36) }, modifier = Modifier.fillMaxWidth(), label = { Text("Widget title") }, singleLine = true)
                    OutlinedTextField(value = colorDraft, onValueChange = { colorDraft = it.take(9) }, modifier = Modifier.fillMaxWidth(), label = { Text("Accent color") }, singleLine = true, supportingText = { Text("Use #RRGGBB or #AARRGGBB, e.g. #C9F56A") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.compact, onCheckedChange = { onUpdate(item.copy(compact = it)) })
                        Text("Compact card", color = Color.White)
                    }
                    Text("Compact mode changes widget padding; it does not change the Android widget grid.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = {
                val color = parseThemeHexColor(colorDraft)
                if (color == null) Toast.makeText(context, "Enter a valid #RRGGBB or #AARRGGBB color.", Toast.LENGTH_LONG).show()
                else { onUpdate(item.copy(title = safeWidgetTitle(titleDraft), accentArgb = color)); editing = false }
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
            containerColor = Color(0xFF151922), titleContentColor = Color.White, textContentColor = Color.White
        )
    }
}

@Composable
private fun ShortcutBody(description: String, buttonText: String, action: () -> Unit) {
    Text(description, color = Color.White.copy(alpha = 0.65f))
    FilledTonalButton(onClick = action, shape = RoundedCornerShape(14.dp)) { Text(buttonText) }
}

@Composable
private fun MetricPill(value: String, label: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = accent, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Text(label, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AnalogClockFace(nowMillis: Long, accent: Color) {
    val time = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour = time.get(Calendar.HOUR).toFloat() + time.get(Calendar.MINUTE) / 60f
    val minute = time.get(Calendar.MINUTE).toFloat() + time.get(Calendar.SECOND) / 60f
    val second = time.get(Calendar.SECOND).toFloat()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(150.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.43f
            drawCircle(Color.White.copy(alpha = 0.08f), radius = radius + 7.dp.toPx())
            drawCircle(accent.copy(alpha = 0.75f), radius = radius, style = Stroke(width = 2.dp.toPx()))
            for (i in 0 until 12) {
                val angle = Math.toRadians(i * 30.0 - 90.0)
                val outer = radius * 0.90f
                val inner = radius * if (i % 3 == 0) 0.75f else 0.83f
                drawLine(accent.copy(alpha = if (i % 3 == 0) 0.95f else 0.55f), androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner), androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer), strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.dp.toPx())
            }
            fun hand(value: Float, scale: Float, width: Float, color: Color) {
                val angle = Math.toRadians(value * 30.0 - 90.0)
                drawLine(color, center, androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * radius * scale, center.y + kotlin.math.sin(angle).toFloat() * radius * scale), strokeWidth = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            hand(hour, 0.48f, 4.dp.toPx(), Color.White)
            hand(minute / 5f, 0.72f, 2.5.dp.toPx(), Color.White)
            hand(second / 5f, 0.80f, 1.2.dp.toPx(), accent)
            drawCircle(accent, radius = 4.dp.toPx(), center = center)
        }
    }
    Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(nowMillis)), modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.65f), fontFamily = FontFamily.Monospace)
}

@Composable
private fun MonthCalendarContent(tick: Long, accent: Color) {
    val now = remember(tick / 60_000L) { LocalDate.now() }
    val month = YearMonth.from(now)
    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) { days.forEach { Text(it, Modifier.weight(1f), color = accent, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
    val blanks = month.atDay(1).dayOfWeek.value - 1
    val cellDates: List<Int?> = List(blanks) { null } + (1..month.lengthOfMonth()).map { it }
    cellDates.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            (week + List(7 - week.size) { null }).forEach { day ->
                val isToday = day == now.dayOfMonth
                Box(Modifier.weight(1f).height(29.dp).padding(1.dp).clip(if (isToday) CircleShape else RoundedCornerShape(8.dp)).background(if (isToday) accent.copy(alpha = 0.22f) else Color.Transparent), contentAlignment = Alignment.Center) {
                    if (day != null) Text(day.toString(), color = if (isToday) accent else Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

private data class BatterySnapshot(val first: Int, val second: String, val third: Int?)
private fun readBattery(context: Context): BatterySnapshot {
    val intent = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val label = when (status) { BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"; BatteryManager.BATTERY_STATUS_FULL -> "Full"; BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"; BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"; else -> "Battery status unavailable" }
    val temperatureRaw = intent?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
    val temp = temperatureRaw?.div(10)
    return BatterySnapshot(pct, label, temp)
}

private fun launchIntent(context: Context, intent: Intent, fallback: Intent) {
    runCatching { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
        .onFailure {
            runCatching { fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(fallback) }
                .onFailure { Toast.makeText(context, "That screen isn't supported on this device.", Toast.LENGTH_SHORT).show() }
        }
}

private fun formatDuration(secondsRaw: Int): String {
    val seconds = secondsRaw.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(Locale.US, seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

private fun formatCountdown(secondsRaw: Long): String {
    val seconds = secondsRaw.coerceAtLeast(0L)
    val days = seconds / 86_400L
    val hours = (seconds % 86_400L) / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remainder = seconds % 60L
    return if (days > 0L) "%dd %02d:%02d:%02d".format(Locale.US, days, hours, minutes, remainder)
    else "%02d:%02d:%02d".format(Locale.US, hours, minutes, remainder)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) { value /= 1024.0; index++ }
    return "%.1f %s".format(Locale.US, value, units[index])
}

private fun nextNewYear(): Long = Calendar.getInstance().apply {
    add(Calendar.YEAR, 1); set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
