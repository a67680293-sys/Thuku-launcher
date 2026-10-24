# Thuku Launcher v0.5.0 — customizable Android Home replacement

Thuku is an Android **Home-app project** built with Kotlin and Jetpack Compose. It advertises `MAIN` + `HOME` + `DEFAULT`, discovers real installed launcher activities through Android's `LauncherApps` API, launches the selected activity, remembers a workspace and dock, and hosts provider-owned Android app widgets through `AppWidgetHost`. To use it as the Home app, open **Customize → Set as default launcher** and choose Thuku in Android Settings.

This release is a substantial step forward from the v0.4 workspace build. It is still an independent launcher with known limitations; it is not claiming feature parity with Launcher3/Lawnchair/ARK or the OEM launchers.

## v0.5.0 highlights

### Real home screen and app management
- Paged workspace backed by installed app activities, first-run starter shortcuts and a persistent dock.
- Swipe actions configurable independently for up and down; horizontal swipe and page indicators navigate workspace pages.
- Workspace lock, configurable home rows/columns, 3–7 dock slots and app-label scaling.
- Actual app drawer with search by original/custom label and package name, grid/list layout, filters, alphabetical/package/most-used/recently-used sorting, pinned-first sorting and a Hidden view.
- Shortcut management to pin/unpin, dock/undock, create or add to a folder, hide/unhide, move earlier/later, rename/reset a shortcut, open Android App Info, find the app in the Play Store, copy a package ID, share a store URL and invoke Android's normal uninstall confirmation for non-system apps.
- Local recent-launch chips and local launch counts; does not ask for Android Usage Access.
- Android Home-button handling, default-launcher settings entry point and fallback UI for devices with limited system support.

### Pixel Lab customization
- Eight accent presets, Android dynamic colors where supported, glass-like or opaque surfaces and transition animation toggle.
- Circle/squircle/rounded/square masks, icon size and installed compatible icon-pack discovery/appfilter mappings.
- System wallpaper backdrop, bounded-size custom photo wallpaper, wallpaper dim, clock style/date, compact layout, grid/row count and labels.
- Versioned JSON theme code with bounded input, schema validation and no execution of user code.
- Bounded JSON export/import for theme/workspace/dock/order/hidden apps/custom labels/folders and supported settings.

### Third-party integrations and useful cards
- **67 native Thuku widget/card types**: time, focus, battery/device info, storage/RAM/network, personal notes/tasks/habits, and over 35 app/system shortcuts. The catalogue is not the same as 67 separate Android `AppWidgetProvider` components.
- Native Android widget host/picker for standard third-party widgets installed on the phone.
- Optional city weather using Open-Meteo geocoding and current-conditions APIs over HTTPS. The user types a city; Thuku does not request GPS/location permission. Attribution is linked in the card. Open-Meteo's free API terms currently limit use to non-commercial purposes, so review its current terms before distributing Thuku commercially.
- Optional now-playing/media transport card. To show title/artist and transport controls, the user must explicitly grant Android notification-listener access. The listener service does not process, store, or upload notification text; the UI reads active media-session metadata when the card is enabled.
- Local-only crash diagnostics, no advertising SDK, no analytics endpoint and no remote code/plugin loader.

## Useful feature inventory

See [`docs/FEATURE_MATRIX.md`](docs/FEATURE_MATRIX.md) for a categorized checklist of more than 80 current capabilities and their scope. See [`docs/WIDGETS.md`](docs/WIDGETS.md) for every built-in card and the limitations that matter when using it.

## Source structure

- `app/src/main/java/com/thuku/launcher/MainActivity.kt` — Android Home activity, real app discovery/launch, workspace, dock, app drawer, settings and standard system-widget host.
- `app/src/main/java/com/thuku/launcher/LauncherModel.kt` — persisted settings, workspace/dock state, custom labels, local launch history, icon pack mapping and bounded import/export.
- `app/src/main/java/com/thuku/launcher/CustomWidgetStudio.kt` — 67-card catalogue, timers/tasks/notes and declarative `thuku-theme-v1` JSON studio.
- `WeatherRepository.kt`, `WeatherCard.kt` — manually configured Open-Meteo request and presentation.
- `MediaControls.kt` — optional media-session card and minimal notification listener service.
- `WallpaperUtils.kt` — bounded decode for a user-selected wallpaper image.
- `CrashDiagnostics.kt` — bounded diagnostics saved only in app-private storage.
- `tools/verify_source.py` — dependency-free static project/workflow/security checks.
- `.github/workflows/android.yml` — GitHub Actions source checks, JVM tests, APK build, checksum and artifact upload.

## Build APK using GitHub Actions (phone-friendly)

1. Upload `ThukuLauncher-v0.5.0-Ultimate-Launcher.zip` into the root of the GitHub repository and commit that upload. In Codespaces, confirm the working tree is clean before syncing.
2. Pull the committed upload in your Codespace and extract into the current repo root only after `ls` confirms the exact ZIP name exists:
   ```bash
   git fetch origin
   git pull --ff-only origin main
   ls -lh ThukuLauncher-v0.5.0-Ultimate-Launcher.zip
   unzip -o ThukuLauncher-v0.5.0-Ultimate-Launcher.zip -d .
   rm -f ThukuLauncher-v0.5.0-Ultimate-Launcher.zip \
     ThukuLauncher-v0.3.0-Full-Source.zip ThukuLauncher-v0.3.0-Full-Source.sha256 \
     ThukuLauncher-v0.3.0-Full-Source.zip.sha256 \
     ThukuLauncher-v0.4.0-Real-Launcher.zip ThukuLauncher-v0.4.0-Real-Launcher.zip.sha256
   git add -A
   git commit -m "Upgrade Thuku Launcher to v0.5.0"
   git push origin main
   ```
   Do not use `git push --force`. If `git status --short` shows uncommitted changes, inspect them before pulling or deleting anything.
3. Open the repository's **Actions** tab and wait for `Build Thuku Launcher APK` to finish. A successful run uploads `ThukuLauncher-debug.apk`, `BUILD-INFO.txt` and `SHA256SUMS.txt` as a workflow artifact. Open the run's **Artifacts** section to download it.
4. Install the debug APK on your phone and select Thuku under Android's **Default apps → Home app**. The debug package ID ends in `.debug`, so it can coexist with a differently signed/provisioned development install. The artifact is a debug-signed test build, not a Play Store release.

## Validation and honest status

The ZIP generation pass runs `python3 tools/verify_source.py`, XML parsing, archive integrity checking and source-structure checks. This environment does not have a configured Android SDK/Gradle dependency cache to compile the complete app locally. Therefore **no claim is made that this exact v0.5.0 ZIP has passed Gradle compilation or physical-device tests**. GitHub Actions is the required real build gate. If it fails, read the first actual Kotlin/Gradle error rather than the later "no artifact found" warning.

## Privacy and Android platform notes

- The app uses `QUERY_ALL_PACKAGES` because a launcher needs broad discovery of installed launcher activities; check Google Play policy eligibility before publishing there.
- `INTERNET` is used for the optional city-weather feature. It does not request fine/coarse location and is disabled until the user enables it and enters a city.
- Media controls request no access automatically. Android requires the user to explicitly enable the declared notification-listener service in system settings; Thuku does not persist notification text.
- Standard third-party Android widgets retain their provider's own code, permissions, network behavior and setup flow.
- Thuku does not claim privileged Quickstep/recents integration, system-wide gesture interception, background task killing, or unauthorized control of protected Android settings.
- `QUERY_ALL_PACKAGES` and the optional weather/network integration may affect store-review eligibility. Check the current policies and Open-Meteo terms for the distribution model you choose.

## Open-source and supplied APK notes

The project is MIT-licensed; see [`LICENSE`](LICENSE). Open-source launcher repositories can be reused only under the applicable license and notice requirements. For this release I took feature/design lessons from public documentation rather than copying whole source files from Kvaesitso, KISS, Lawnchair, Fossify or ARK. The uploaded `Nil Launcher v18.0 - Premium.apk` was treated as a feature reference, not proven source code: an APK alone does not establish that it is open source, and its archive listing did not expose a separate source tree or license/notice file. I did not run it, bundle it, or decompile its binaries. The feature inspirations were an explicit Home setup, installed-system-app shortcuts, system widget hosting and opt-in music controls.

See [`docs/OPEN_SOURCE_RESEARCH.md`](docs/OPEN_SOURCE_RESEARCH.md) for links, licensing notes and design decisions.
