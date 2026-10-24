#!/usr/bin/env python3
"""Dependency-free sanity checks for the Thuku Launcher source package."""
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
    require("android.permission.INTERNET" not in permissions, "unexpected INTERNET permission in privacy-first build")
    application = manifest.find("application")
    require(application is not None, "application node missing from manifest")
    require(application.attrib.get(android_ns + "allowBackup") == "false", "automatic app data backup should be disabled")

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
    require('versionName = "0.3.0"' in app_gradle, "app version should be 0.3.0")

    widget_source = (ROOT / "app/src/main/java/com/thuku/launcher/CustomWidgetStudio.kt").read_text(encoding="utf-8")
    widget_names = re.findall(r'^    ([A-Z][A-Z0-9_]+)\("', widget_source, re.M)
    require(len(widget_names) >= 30, f"expected 30+ built-in widgets, found {len(widget_names)}")
    require(len(widget_names) == len(set(widget_names)), "duplicate custom widget IDs")
    require('thuku-theme-v1' in widget_source and 'raw.length <= 20_000' in widget_source, "bounded theme DSL validation missing")
    require('readThemeInputBounded' in widget_source and '20_001' in widget_source, "bounded theme file import missing")
    require('take(30)' in widget_source, "custom widget storage should enforce its 30-widget limit")
    require('ThemeCodeStudio(config = config, onApply = onConfig)' in (ROOT / "app/src/main/java/com/thuku/launcher/MainActivity.kt").read_text(encoding="utf-8"), "theme studio is not connected to customization UI")

    tests = (ROOT / "app/src/test/java/com/thuku/launcher/LauncherValidationTest.kt").read_text(encoding="utf-8")
    require("themeHexColorParserSupportsRgbAndArgbAndRejectsInvalidValues" in tests, "theme color unit test missing")
    model_source = (ROOT / "app/src/main/java/com/thuku/launcher/LauncherModel.kt").read_text(encoding="utf-8")
    main_source = (ROOT / "app/src/main/java/com/thuku/launcher/MainActivity.kt").read_text(encoding="utf-8")
    require('json.length <= 100_000' in model_source and 'readLauncherBackupBounded' in main_source, "bounded launcher backup import missing")
    require("widgetCatalogueHasAtLeastThirtyDistinctTypes" in tests, "widget catalogue unit test missing")

    print("PASS: Android XML parsing")
    print("PASS: manifest permissions/privacy checks")
    print("PASS: SDK package/workflow checks")
    print("PASS: Kotlin plugin version alignment")
    print(f"PASS: {len(widget_names)} unique built-in widget types")
    print("PASS: theme DSL and unit test presence")
    print("NOTE: this is a static sanity check, not an Android/Gradle compilation or device test")
except SystemExit:
    raise
except Exception as exc:
    print(f"FAIL: source validation could not complete: {exc}", file=sys.stderr)
    raise SystemExit(2)
