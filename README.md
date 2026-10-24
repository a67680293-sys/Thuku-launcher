# Thuku Launcher

Thuku Launcher is an Android home-screen launcher project. This ZIP is arranged so that you can upload the **contents** to a GitHub repository and GitHub Actions will automatically build an installable debug APK.

> **Build status:** The workflow is configured, but the APK is not confirmed to compile until GitHub completes the first successful build. If the first run fails, open its log and use the error output to fix the project.

## What is included

- Android launcher/Home app integration.
- Installed-app list and app search.
- App-library filters, pinning and launcher-level hiding.
- Wallpaper-aware background and appearance controls.
- Accent colors, icon-pack support, icon sizing/shape, grid and label options.
- Android app widgets through the system widget picker.
- Quick actions for supported Android settings panels.
- Focus timer and local settings export/import.
- GitHub Actions workflow to build and upload an APK after a push.

## Upload this project to GitHub (phone-friendly)

1. Download and extract `ThukuLauncher-GitHub-Auto-APK.zip`.
2. Open GitHub and create a new repository. `Thuku-launcher` is a suitable name. You can make it public or private.
3. Open the new repository and choose **Add file → Upload files**.
4. Upload the **contents inside** the extracted folder—not the ZIP itself. The repository's top level should show files/folders such as `settings.gradle.kts`, `build.gradle.kts`, `app`, and `.github`.
5. Commit the upload to the repository.
6. Open the repository's **Actions** tab. The workflow named **Build Thuku Launcher APK** should start automatically after the push. If GitHub asks you to enable Actions, enable them.
7. Open the newest workflow run and wait for it to finish successfully.
8. Under that run's **Artifacts** section, tap **ThukuLauncher-debug-APK** to download the APK ZIP. Extract that downloaded artifact ZIP to get `app-debug.apk`.
9. Open `app-debug.apk` on your Android phone and follow Android's installation prompts. You may need to allow your browser or file manager to install apps from that source.

## Where the APK download appears

- **Every successful push:** Repository → **Actions** → **Build Thuku Launcher APK** → latest successful run → **Artifacts** → `ThukuLauncher-debug-APK`.
- **Optional permanent release:** When you later create and push a version tag such as `v0.1.1`, the workflow also attaches the APK to a GitHub Release. A tag can be created from GitHub's Releases page or with Git.

GitHub Actions does not put the APK directly in the source-code file list. Download it from the workflow artifact or Releases page.

## Build details

- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Android SDK Platform 36 / Build Tools 36.0.0

GitHub Actions installs the required build tools; Android Studio is not required just to obtain the APK.

## Important notes

- This workflow builds a **debug APK**, which Android signs automatically for testing and sideloading. It is not a Play Store-ready release.
- The first build has not been verified in this environment. GitHub Actions' build log is the authoritative check.
- Workflow artifacts expire after 30 days. Use a version tag to publish the APK as a GitHub Release if you want a more persistent download.
- Do not upload signing keys, passwords, or API secrets into source files or commit them to a public repository.
- Hidden apps are hidden only within Thuku Launcher; this does not disable or secure them elsewhere on the phone.
- Widget availability and some system actions depend on Android version and device manufacturer.

## Research references

- [Lawnchair](https://github.com/LawnchairLauncher/lawnchair)
- [Neo Launcher](https://github.com/NeoApplications/Neo-Launcher)
- [KISS Launcher](https://github.com/Neamar/KISS)
- [Niagara Launcher feature overview](https://niagaralauncher.com/niagara-pro-features/)
- [Smart Launcher feature overview](https://www.smartlauncher.net/features)
- [Android app-widget documentation](https://developer.android.com/develop/ui/views/appwidgets/overview)

These projects informed the feature checklist only. Their proprietary assets and branded source code are not bundled into Thuku Launcher.
