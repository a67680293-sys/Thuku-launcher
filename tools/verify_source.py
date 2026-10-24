#!/usr/bin/env python3
"""Dependency-free static sanity checks for the Thuku Launcher source package.

This is not a substitute for Gradle compilation, instrumentation tests, or phone QA.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


try:
    manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
    manifest = ET.parse(manifest_path).getroot()
    android_ns = "{http://schemas.android.com/apk/res/android}"
    permissions = {node.attrib.get(android_ns + "name", "") for node in manifest.findall("uses-permission")}
    require("android.permission.QUERY_ALL_PACKAGES" in permissions, "launcher app discovery permission missing")
    require("android.permission.ACCESS_NETWORK_STATE" in permissions, "connection status permission missing")
    require("android.permission.INTERNET" in permissions, "optional Open-Meteo weather needs INTERNET permission")
    require("android.permission.ACCESS_FINE_LOCATION" not in permissions, "weather must not silently request precise location")
    require("android.permission.ACCESS_COARSE_LOCATION" not in permissions, "weather must not silently request location")
    activities = manifest.findall("application/activity")
    home_filters = [f for a in activities for f in a.findall("intent-filter") if any(c.attrib.get(android_ns + "name") == "android.intent.category.HOME" for c in f.findall("category"))]
    require(bool(home_filters), "MAIN/HOME intent filter missing: app cannot be offered as a home replacement")
    require(any(any(c.attrib.get(android_ns + "name") == "android.intent.category.DEFAULT" for c in f.findall("category")) for f in home_filters), "HOME filter must include CATEGORY_DEFAULT")
    application = manifest.find("application")
    require(application is not None, "application node missing from manifest")
    require(application.attrib.get(android_ns + "allowBackup") == "false", "automatic app data backup should be disabled")
    media_services = [service for service in application.findall("service") if service.attrib.get(android_ns + "permission") == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"]
    require(bool(media_services), "optional media-session access service missing")

    for xml in (ROOT / "app/src/main/res/values/styles.xml", ROOT / "app/src/main/res/values/colors.xml"):
        ET.parse(xml)

    workflow = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
    require("android-actions/setup-android@v4" in workflow, "workflow must use setup-android v4")
    require(not re.search(r"packages\s*:\s*['\"]?tools(?:\s|['\"])", workflow), "workflow requests retired Android SDK tools package")
    require('sdkmanager --install "platforms;android-36" "build-tools;36.0.0"' in workflow, "explicit Android SDK packages missing")
    require("testDebugUnitTest" in workflow and "assembleDebug" in workflow, "test and APK build tasks must be blocking")
    require("if-no-files-found: error" in workflow, "artifact upload must fail when APK is missing")

    root_gradle = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    app_gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    kotlin_android = re.search(r'id\("org\.jetbrains\.kotlin\.android"\) version "([^"]+)"', root_gradle)
    kotlin_compose = re.search(r'id\("org\.jetbrains\.kotlin\.plugin\.compose"\) version "([^"]+)"', root_gradle)
    require(kotlin_android is not None and kotlin_compose is not None, "Kotlin Android/Compose plugins missing")
    require(kotlin_android.group(1) == kotlin_compose.group(1), "Android and Compose Kotlin plugin versions must match")
    require('compileSdk = 36' in app_gradle and 'targetSdk = 36' in app_gradle, "SDK level configuration is missing")
    require('compilerOptions' in app_gradle and 'JvmTarget.JVM_17' in app_gradle and 'kotlinOptions' not in app_gradle, "Kotlin JVM target should use the typed compilerOptions DSL")
    require('versionName = "0.5.0"' in app_gradle, "app version should be 0.5.0")
    require('compose-bom:2025.10.00' in app_gradle, "Compose BOM should stay compatible with the pinned AGP baseline")

    main_source = (ROOT / "app/src/main/java/com/thuku/launcher/MainActivity.kt").read_text(encoding="utf-8")
    require("WorkspaceDock(" in main_source and "WorkspaceAddTile(" in main_source, "real workspace grid and persistent dock missing")
    require("detectDragGestures" in main_source and "Swipe up: ${config.swipeUpAction}" in main_source, "workspace swipe gestures aren't configurable")
    require("onNewIntent(intent: Intent)" in main_source and "homeIntentSignal" in main_source, "system Home press should return to the workspace")
    require("AppWidgetHost" in main_source and "pickSystemWidget" in main_source, "Android widget host/picker missing")
    require("customWallpaperUri" in main_source and "WeatherCard(" in main_source and "MusicControlsCard(" in main_source, "wallpaper/weather/media integrations missing")
    require("dynamicDarkColorScheme" in main_source and "drawerSort" in main_source and "workspaceLocked" in main_source, "customization enhancements missing")

    model_source = (ROOT / "app/src/main/java/com/thuku/launcher/LauncherModel.kt").read_text(encoding="utf-8")
    require("homeOrder: List<String>" in model_source and "dockApps: List<String>" in model_source, "ordered workspace and dock persistence missing")
    require("seedWorkspaceIfNeeded" in model_source, "first-run workspace seeding missing")
    require("class AppUsageStore" in model_source and "fun launcherLabel" in model_source, "local recency and custom app names missing")
    require('"thuku-launcher-backup"' in model_source and '"customLabels"' in model_source, "versioned backup/restore support missing")

    widget_source = (ROOT / "app/src/main/java/com/thuku/launcher/CustomWidgetStudio.kt").read_text(encoding="utf-8")
    widget_names = re.findall(r'^    ([A-Z][A-Z0-9_]+)\("', widget_source, re.M)
    require(len(widget_names) >= 60, f"expected 60+ built-in widgets, found {len(widget_names)}")
    require(len(widget_names) == len(set(widget_names)), "duplicate custom widget IDs")
    require('thuku-theme-v1' in widget_source and 'raw.length <= 20_000' in widget_source, "bounded theme DSL validation missing")
    require('readThemeInputBounded' in widget_source and '20_001' in widget_source, "bounded theme file import missing")
    require('take(30)' in widget_source, "custom widget instance storage should enforce its 30-widget limit")
    require('ThemeCodeStudio(config = config, onApply = onConfig)' in main_source, "theme studio is not connected to customization UI")

    weather_source = (ROOT / "app/src/main/java/com/thuku/launcher/WeatherRepository.kt").read_text(encoding="utf-8")
    media_source = (ROOT / "app/src/main/java/com/thuku/launcher/MediaControls.kt").read_text(encoding="utf-8")
    require("geocoding-api.open-meteo.com" in weather_source and "api.open-meteo.com" in weather_source, "real city-based weather API integration missing")
    require("CACHE_TTL_MILLIS" in weather_source and "96 * 1024" in weather_source, "weather response caching/bounds missing")
    require("ACCESS_FINE_LOCATION" not in weather_source and "ACCESS_COARSE_LOCATION" not in weather_source, "weather client must not use location permissions")
    require("getActiveSessions" in media_source and "transportControls" in media_source, "optional media transport controls missing")
    require("onNotificationPosted" not in media_source and "onNotificationRemoved" not in media_source, "media service should not process/store notification events")

    tests = (ROOT / "app/src/test/java/com/thuku/launcher/LauncherValidationTest.kt").read_text(encoding="utf-8")
    require("themeHexColorParserSupportsRgbAndArgbAndRejectsInvalidValues" in tests, "theme color unit test missing")
    require("widgetCatalogueHasAtLeastSixtyDistinctTypes" in tests, "60+ widget catalogue unit test missing")
    require("weatherCodesAreMappedToReadableConditions" in tests, "weather condition mapping unit test missing")
    require('json.length <= 100_000' in model_source and 'readLauncherBackupBounded' in main_source, "bounded launcher backup import missing")

    must_exist = [
        "app/src/main/java/com/thuku/launcher/WeatherRepository.kt",
        "app/src/main/java/com/thuku/launcher/WeatherCard.kt",
        "app/src/main/java/com/thuku/launcher/MediaControls.kt",
        "app/src/main/java/com/thuku/launcher/WallpaperUtils.kt",
        "docs/FEATURE_MATRIX.md",
        "docs/OPEN_SOURCE_RESEARCH.md",
    ]
    for rel in must_exist:
        require((ROOT / rel).is_file(), f"required project file missing: {rel}")

    print("PASS: Android XML parsing")
    print("PASS: real HOME intent, widget host, and optional permission checks")
    print("PASS: SDK package/workflow checks")
    print("PASS: Kotlin plugin version alignment")
    print(f"PASS: {len(widget_names)} unique built-in widget types")
    print("PASS: local workspace, custom labels, backup validation and integrations")
    print("PASS: static test coverage markers")
    print("NOTE: this is a static sanity check, not an Android/Gradle compilation or device test")
except SystemExit:
    raise
except Exception as exc:
    print(f"FAIL: source validation could not complete: {exc}", file=sys.stderr)
    raise SystemExit(2)
