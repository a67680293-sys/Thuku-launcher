# Changelog

## 0.3.0 — Widget Studio + Theme Code

- Added 31 built-in custom home cards across Time, Focus, Device, Shortcuts, Personal and Apps.
- Added persistent timer, Pomodoro, stopwatch, countdown, notes, checklist, habit and water counter behavior.
- Added per-widget title, accent color and compact layout customization.
- Added a safe `thuku-theme-v1` JSON editor with validation, apply, import, export and copy actions.
- Added theme-color parser/catalogue unit tests and widget behavior documentation.
- Disabled Android automatic backup for local notes/settings and added read-only network state permission for the connection card.
- Updated Kotlin to 2.4.21 and upgraded the launcher app version to 0.3.0.
- Kept third-party Android widget hosting and added explicit distinction between live provider widgets and local shortcut cards.
- Updated GitHub Actions for `android-actions/setup-android@v4` so it no longer uses the retired `tools` SDK package, added layout checks, build metadata, checksums, failure diagnostics and release uploads.

## 0.2.0 — Previous source baseline

- Existing Kotlin/Compose launcher with app drawer, pinned apps, folders, focus timer, icon pack support, appearance controls, system widget host and local diagnostics.
