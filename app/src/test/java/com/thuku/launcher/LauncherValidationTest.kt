package com.thuku.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherValidationTest {
    @Test fun iconScaleIsClampedAndNanFallsBack() {
        assertEquals(0.7f, LauncherValidation.iconScale(0.1f), 0.0001f)
        assertEquals(1.35f, LauncherValidation.iconScale(4.0f), 0.0001f)
        assertEquals(1.0f, LauncherValidation.iconScale(Float.NaN), 0.0001f)
        assertEquals(1.2f, LauncherValidation.iconScale(Float.POSITIVE_INFINITY, 1.2f), 0.0001f)
    }

    @Test fun gridColumnsStayWithinSupportedRange() {
        assertEquals(3, LauncherValidation.columns(-4))
        assertEquals(4, LauncherValidation.columns(4))
        assertEquals(6, LauncherValidation.columns(20))
    }

    @Test fun wallpaperDimStaysWithinSupportedRange() {
        assertEquals(0.0f, LauncherValidation.wallpaperDim(-1f), 0.0001f)
        assertEquals(0.55f, LauncherValidation.wallpaperDim(0.55f), 0.0001f)
        assertEquals(0.8f, LauncherValidation.wallpaperDim(9f), 0.0001f)
        assertEquals(0.48f, LauncherValidation.wallpaperDim(Float.NaN), 0.0001f)
    }

    @Test fun iconShapeValidationOnlyAcceptsKnownShapes() {
        assertTrue(LauncherValidation.isIconShape("Squircle"))
        assertTrue(LauncherValidation.isIconShape("Circle"))
        assertFalse(LauncherValidation.isIconShape("Triangle"))
        assertFalse(LauncherValidation.isIconShape(""))
    }

    @Test fun themeHexColorParserSupportsRgbAndArgbAndRejectsInvalidValues() {
        assertEquals(0xFFC9F56AL, parseThemeHexColor("#C9F56A"))
        assertEquals(0x80C9F56AL, parseThemeHexColor("#80C9F56A"))
        assertNull(parseThemeHexColor("lime"))
        assertNull(parseThemeHexColor("#12345"))
        assertNull(parseThemeHexColor("#GGGGGG"))
    }

    @Test fun widgetCatalogueHasAtLeastSixtyDistinctTypes() {
        assertTrue(CustomWidgetKind.entries.size >= 60)
        assertEquals(CustomWidgetKind.entries.size, CustomWidgetKind.entries.map { it.name }.distinct().size)
    }

    @Test fun weatherCodesAreMappedToReadableConditions() {
        assertEquals("Clear sky", weatherDescription(0, true))
        assertEquals("Clear night", weatherDescription(0, false))
        assertEquals("Rain", weatherDescription(63))
        assertEquals("Thunderstorm with hail", weatherDescription(99))
        assertEquals("Weather unavailable", weatherDescription(-1))
    }

    @Test fun folderNamesAreTrimmedBoundedAndControlFree() {
        assertEquals("School", LauncherValidation.folderName("  School  "))
        assertEquals("SafeName", LauncherValidation.folderName("Safe\u0000Name"))
        assertEquals(32, LauncherValidation.folderName("x".repeat(80)).length)
        assertEquals("", LauncherValidation.folderName(" \u0000 \t "))
    }
}
