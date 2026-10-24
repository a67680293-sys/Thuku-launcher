# Build / source audit notes — v0.5.0

## Recent build issues addressed in source

Earlier Actions logs showed Compose 1.12.1 and AndroidX Core 1.19.1 requiring Android Gradle Plugin 9.1.0 or newer while the project uses AGP 8.13.2. The current source pins Compose BOM `2025.10.00` and AndroidX Core `1.17.0`. Dependabot is configured to ignore AndroidX Core 1.19.x until the project intentionally upgrades AGP. Kotlin compiler errors in earlier commits (a public method exposing an internal app type and smart-casting a delegated nullable wallpaper property) were corrected in the v0.4 baseline. V0.5 adds custom wallpaper decoding; its bounds-only decode correctly treats the decode result as intentionally null and checks the output dimensions before allocating a bounded bitmap.

This file records prior known errors; it does **not** mean the new v0.5.0 source has passed Gradle. Run GitHub Actions after uploading it to compile the actual project.

## Architecture

- `AndroidManifest.xml`: `MAIN` + `HOME` + `DEFAULT` eligibility and separate `MAIN` + `LAUNCHER` discoverability.
- `MainActivity.kt`: installed activities from `LauncherApps`, actual app launch, HOME intent routing, paged workspace, dock, app library, system widget host, settings, app management and layout state wiring.
- `LauncherModel.kt`: local preferences, workspace/dock ordering, custom labels, bounded launch history, icon pack loading, and bounded/versioned user backup/restore.
- `CustomWidgetStudio.kt`: 67 native card definitions, local storage, timers/checklists and a declarative/validated theme JSON format; never evaluates user-supplied source code.
- `WeatherRepository.kt`: manually entered city via Open-Meteo HTTPS geocoding/current-weather endpoints; bounded timeout and response size; no location permission.
- `MediaControls.kt`: opt-in media session metadata/control. The `NotificationListenerService` does not process notification events or persist notification content.
- `WallpaperUtils.kt`: two-pass decode at a bounded resolution to prevent decoding a huge source image at full pixel size for the launcher backdrop.
- `.github/workflows/android.yml`: install explicit SDK packages, run source checks/tests and APK assembly, ensure the APK exists, then upload APK/checksum/build information. Failure diagnostics are attempted even if the main build fails.

## Validation scope

Local static checks parse Android XML and look for critical source/workflow/security invariants. They are not a Kotlin compiler or Gradle build. This container does not have a complete Android SDK/dependency cache for the module, so compile/test results must come from the repository's GitHub Actions run. Physical device QA remains a separate required step; see `DEVICE_TEST_CHECKLIST.md`.

## Required compatibility review before production

- `QUERY_ALL_PACKAGES` can face Google Play policy review for launchers; validate eligibility before publishing.
- Open-Meteo's free API terms require non-commercial use and CC-BY 4.0 acceptance; assess current terms before commercial distribution.
- Media controls depend on the user granting notification-listener access and the target music app exposing a media session.
- Icon packs vary in metadata, naming and icon-mask support; the current parser covers common `appfilter.xml` mappings, not every pack extension.
- Android widget provider layout, profile restrictions and configuration screens vary by provider/device. App widgets are not custom Thuku cards and carry their own behavior.
- `LauncherApps`, OS Settings intents and app uninstall flows should be checked on real devices/OEM builds.

## Official references

- Android LauncherApps API: https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps
- Android widget-host guide: https://developer.android.com/develop/ui/views/appwidgets/host
- AOSP app shortcuts: https://source.android.com/docs/core/display/app-shortcuts
- Android SDK setup action: https://github.com/android-actions/setup-android
- Kotlin Gradle compiler options: https://kotlinlang.org/docs/gradle-compiler-options.html
- Open-Meteo terms: https://open-meteo.com/en/terms
