# Build / source audit notes

## Confirmed from the supplied GitHub Actions log

The failing run ended in the Android SDK setup step before Gradle had been invoked. Its `setup-android@v3` inputs explicitly included `packages: tools platform-tools`, and the remote package repository did not provide the deprecated `tools` package. This is the SDK setup failure being fixed here.

## Changes in this source package

- Migrated to `android-actions/setup-android@v4`, whose documented default package set does not request the removed `tools` package.
- Installs Android Platform 36 and Build Tools 36.0.0 explicitly.
- Uses Java 17, Gradle 8.13, AGP 8.13.2, and Kotlin/Compose plugin 2.4.21. The Kotlin compatibility table lists KGP 2.4.20–2.4.21 as fully supported with Gradle 7.6.3–9.7.0 and AGP 8.5.2–9.3.1; this project's pinned versions fit those ranges.
- Adds a dependency-free `tools/verify_source.py` script. CI runs this before Gradle to detect missing files, stale SDK setup, incorrect plugin version alignment, unexpected internet permission, missing theme/widget code, and incorrect app version.
- Keeps unit tests and the APK build blocking. APK upload uses `if-no-files-found: error`; lint is retained as advisory so its report doesn't hide whether compile/tests/APK creation succeeded.
- Produces checksum/build metadata and tries to upload Gradle reports when a build fails.
- Adds unit checks for theme hex parsing and catalogue uniqueness/count, plus device acceptance checklist.

## Scope and unverified items

The static checks, source inspection and ZIP-integrity check are not a substitute for compiling the Android project. This environment doesn't have Gradle/Android SDK caches and cannot resolve internet hosts to fetch them, so I cannot honestly claim that `assembleDebug`, `testDebugUnitTest`, UI rendering, Android's widget picker, timer persistence, or OEM-specific default-launcher settings have been exercised here. GitHub Actions and the real-phone checklist are included to verify those remaining points.

## Official references reviewed

- Android SDK setup action and retired `tools` package: https://github.com/android-actions/setup-android
- Typed Kotlin compiler options and Kotlin/AGP/Gradle compatibility: https://kotlinlang.org/docs/gradle-compiler-options.html and https://kotlinlang.org/docs/gradle-configure-project.html
- Android app-widget-host lifecycle, binding and configuration requirements: https://developer.android.com/develop/ui/views/appwidgets/host
- Android guidance against loading executable code from external files: https://developer.android.com/privacy-and-security/security-tips
