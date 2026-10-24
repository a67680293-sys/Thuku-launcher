package com.thuku.launcher

/** Pure validation rules shared by settings import, UI controls, and unit tests. */
internal object LauncherValidation {
    private val iconShapes = setOf("Circle", "Squircle", "Rounded", "Square")

    fun iconScale(value: Float, fallback: Float = 1.0f): Float =
        if (value.isFinite()) value.coerceIn(0.7f, 1.35f) else fallback.coerceIn(0.7f, 1.35f)

    fun columns(value: Int): Int = value.coerceIn(3, 6)

    fun wallpaperDim(value: Float, fallback: Float = 0.48f): Float =
        if (value.isFinite()) value.coerceIn(0.0f, 0.8f) else fallback.coerceIn(0.0f, 0.8f)

    fun isIconShape(value: String): Boolean = value in iconShapes

    fun folderName(raw: String): String = raw
        .filter { it.code >= 0x20 && it.code != 0x7F }
        .trim()
        .take(32)
        .trim()
}
