# Thuku Launcher v0.3.0 — complete Android source

Thuku Launcher is an Android home-screen replacement built with Kotlin and Jetpack Compose. This package upgrades the existing project with **31 built-in configurable home cards**, a safe JSON theme-code studio, more robust GitHub Actions build packaging, extra tests, and device-testing documentation.

> **Build honesty:** the uploaded failure logs stopped inside `android-actions/setup-android@v3` because it requested the removed Android SDK package `tools`. This project uses `setup-android@v4`, which no longer requests that obsolete package. I performed a source/archive/static review, added unit-test coverage and CI checks, and validated the ZIP structure. I could not run a complete Android Gradle build or launch the APK in an emulator in this environment; GitHub Actions must confirm the first real build succeeds. No source ZIP can truthfully guarantee device-perfect behavior without that successful build and real-device testing.

## New in v0.3.0

- **31 built-in custom widgets:** clocks, calendar, world clocks, countdown, timer, stopwatch, Pomodoro, battery, storage, memory snapshot, device info, connection state, shortcuts, notes, checklist, habit tracker, water counter, pinned-app jump, app counts and search.
- **Per-widget customization:** editable title, hex accent color, compact mode, persistent card settings, notes and tasks.
- **Timer behavior:** countdown timers and Pomodoro timers have presets, start/pause/reset, persistent running targets and completion feedback. The stopwatch supports start/pause/reset. Countdown targets use Android date/time pickers.
- **Theme Code Studio:** create/edit, validate, apply, import, export or copy theme JSON. Supported values include accent, icon size, grid columns, labels, glass surfaces, wallpaper visibility/dimming, clock style, icon shape, animations, date display and compact layout. Imported theme files are read with a strict size limit before parsing.
- **Safer theme system:** theme files are a documented, validated declarative JSON format, capped at 20 KB. Thuku never executes user-provided Kotlin/JavaScript or loads scripts/plugins from external storage.
- **Improved build pipeline:** modern Android SDK setup, explicit SDK platform/build-tools installation, Java 17, unit tests, build, APK existence check, SHA-256 checksum, build metadata and failure diagnostics artifact.
- **Privacy & resilience:** app data backup is disabled at the Android OS level, custom content is stored in private app preferences/files, no internet permission is added, and the launcher keeps its existing local error diagnostics.

## Widget inspiration and boundaries

The provided Nil Launcher APK includes categories such as clock/world-clock, calendar, weather, battery, quote and music widgets. Thuku implements its own clock/calendar/battery/quote/world-clock cards and a music/weather shortcut, without copying that APK's source code or assets. For actual live weather forecasts or media playback controls, add the weather/music app's own widget through the Android widget picker. Thuku also supports third-party Android widgets through `AppWidgetHost`; those widgets keep their own behavior and permissions.

The built-in Thuku cards are rendered inside Thuku's home screen; they are **not standalone AppWidgetProvider components** that can be placed on other launchers.

## Built-in widget catalogue

| Category | Widgets |
|---|---|
| Time | Digital clock, Analog clock, Date & week, World clocks, Month calendar, Countdown, Timer, Stopwatch |
| Focus | Pomodoro, Focus goal |
| Device | Battery level, Battery details, Storage, Memory snapshot, Device info |
| Shortcuts | Connection status, Wi-Fi, Bluetooth, Display settings, System settings, Weather search, Music app, Digital Wellbeing settings |
| Personal | Daily thought, Quick notes, Task list, Daily habit, Water tracker |
| Apps | Pinned apps, App library stats, App search |

## Theme code

Open **Lab → Theme Code Studio**. Paste JSON, select **Validate & apply**, or import/export a JSON theme file. The canonical schema and example are in [`docs/THEME_CODE.md`](docs/THEME_CODE.md). A theme changes only known launcher settings; it is not an executable plugin format.

## Project structure

- `app/src/main/java/com/thuku/launcher/MainActivity.kt` — navigation, app discovery, existing home UI and Android widget hosting.
- `app/src/main/java/com/thuku/launcher/LauncherModel.kt` — preference model, app categorization, icon-pack integration, settings backup/restore.
- `app/src/main/java/com/thuku/launcher/CustomWidgetStudio.kt` — 31 widget types, local persistence, custom widget UI, timers/stopwatch, JSON theme parser and theme editor.
- `app/src/main/java/com/thuku/launcher/LauncherValidation.kt` — bounded input validation.
- `app/src/main/java/com/thuku/launcher/CrashDiagnostics.kt` — local-only crash diagnostics.
- `app/src/test/.../LauncherValidationTest.kt` — validation and widget catalogue tests.
- `.github/workflows/android.yml` — automatic test/build/APK artifact and tag release workflow.
- `docs/DEVICE_TEST_CHECKLIST.md` — real-device acceptance tests.
- `docs/WIDGETS.md` — widget behavior and limitations.

## Phone-only GitHub upload flow

The ZIP contains the full project source tree. GitHub Actions cannot run a workflow while it's buried inside a ZIP, so upload the ZIP and extract its contents into the repository root through Codespaces.

1. Download `ThukuLauncher-v0.3.0-Full-Source.zip`.
2. Open your `Thuku-launcher` repository on GitHub and use **Add file → Upload files** to upload this ZIP at the repository root. Commit the uploaded ZIP.
3. Open the existing Codespace for the repo. In its terminal, check whether the ZIP is present: `ls -lh ThukuLauncher-v0.3.0-Full-Source.zip`.
4. Before replacing files, run the safe backup/sync commands below. They preserve an old local branch name and stash uncommitted local changes before syncing with GitHub's `main` branch:

```bash
# Run from the repository root. This does not delete your GitHub repository.
set -euo pipefail
git status --short
git branch "backup-before-thuku-$(date +%Y%m%d-%H%M%S)"
git stash push --include-untracked -m "Before Thuku Launcher v0.3.0 import"
git fetch origin
git checkout main
git reset --hard origin/main

# Extract all project files into the repo root (not into a nested project folder).
mkdir -p /tmp/thuku-v030
unzip -o ThukuLauncher-v0.3.0-Full-Source.zip -d /tmp/thuku-v030
cp -a /tmp/thuku-v030/. .
rm -f ThukuLauncher-v0.3.0-Full-Source.zip

git add -A
git commit -m "Upgrade Thuku Launcher to v0.3.0"
git push origin main
```

**What the sync commands do:** the named `backup-before-thuku-...` branch keeps a reference to the old local branch, and `git stash` retains uncommitted changes. `git reset --hard origin/main` intentionally makes the working branch match the GitHub `main` branch before installing the full replacement project. Do not run it if you need uncommitted changes on the current working tree but haven't checked that the stash succeeded. The upload above should mean the ZIP is already present in `origin/main` when `git reset` runs.

If `git checkout main` says the branch name doesn't exist, stop and send the terminal screenshot rather than trying random force-push commands. Never use `git push --force` to fix the previous non-fast-forward error.

## Download the APK

1. Open the repository's **Actions** tab.
2. Open **Build Thuku Launcher APK** and the newest run.
3. Confirm the **Build, test and package APK** job succeeded.
4. Download `ThukuLauncher-APK-<run number>` under **Artifacts** and extract that ZIP.
5. Install `ThukuLauncher-debug.apk` on the Android phone. `BUILD-INFO.txt` and `SHA256SUMS.txt` are included.

Every successful build makes an artifact available for 30 days. A pushed tag starting with `v` causes the workflow to attach the APK and checksum to a GitHub Release after build/test success.

## Build environment

- Android Gradle Plugin 8.13.2
- Kotlin 2.4.21 and the matching Compose compiler plugin
- Gradle 8.13
- JDK 17
- Android SDK Platform 36 / Build Tools 36.0.0
- Compose BOM 2026.09.00 (stable BOM)

The CI workflow installs Gradle 8.13 directly and therefore does not require a committed Gradle Wrapper JAR. The Gradle wrapper is the normal recommendation for local development; GitHub Actions uses the pinned Gradle installation explicitly.

## Signing / update behavior

The artifact is a **debug-signed testing APK**, not a Play Store production release. Debug signing is automatic, but signing identity may change between hosted CI runs. If Android refuses to install a newer debug build over an older one because of a signature mismatch, uninstall the older debug build first and then install the new APK. This can delete that install's local settings. A consistent production update path requires a private release keystore in GitHub Actions Secrets; never commit keystores or signing passwords to source control.

## Privacy notes

- The application manifest does not request internet access. The weather widget opens an external web search only when tapped.
- Settings, notes, tasks and widget configurations use private app storage. `allowBackup=false` stops Android's automatic app-data backup; exportable launcher settings and theme JSON remain user-initiated.
- `QUERY_ALL_PACKAGES` is needed for the launcher app drawer and is subject to Google Play policy review. The launcher uses `ACCESS_NETWORK_STATE` only to display a read-only connection snapshot.
- Third-party Android widgets run according to their own providers and permissions.

## Troubleshooting

If Actions fails, open the run and start with the first failing step. A failure in SDK setup is different from Kotlin compilation; share the new failed step/logs rather than repeating the same Git command. When source compilation or tests fail, the workflow attempts to upload `ThukuLauncher-build-diagnostics-<run number>` containing reports/test results. Read the real error and fix that error before changing versions again.
