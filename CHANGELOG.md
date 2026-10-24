# Changelog

## 0.5.0 — Launcher customization and integrations

- Expanded the built-in widget/card catalogue from 31 to 67 unique card types, adding launch targets for dialer, messaging, contacts, calendar, email, browser, maps, calculator, camera, clock, app marketplace, document picker, notification settings, app management, battery/storage/privacy/security/accessibility/location/language/date-time/sound/Home/network/display/volume/usage/wallpaper/DND/VPN/cast/keyboard/NFC/mobile network screens. These shortcut cards open Android-owned activities; they do not silently toggle protected settings.
- Added independent configurable swipe-up/down actions, grid/list drawer, drawer sort modes, custom app labels, local recently/most-used ordering, local recency row, app info/store/share/package-copy management actions and Android's uninstall confirmation flow.
- Added app layout controls: home-row count, dock capacity, app label size, workspace lock and optional Android dynamic color.
- Added bounded custom photo wallpaper picker, Open-Meteo city weather (manual city, no location permission, user-controlled/optional) and media-session controls behind explicit notification-listener access.
- Extended backup data to include new workspace, drawer, gestures, weather and custom-label preferences; added static checks and tests for 60+ widget types and weather-code mapping.
- Fixed the bounds pass in custom wallpaper image decoding and added clickable Open-Meteo attribution.
- Reviewed open-source launcher patterns and documented licensing boundaries and non-parity limits.

## 0.4.0 — Workspace launcher rebuild

- Reworked the first screen into a real workspace with installed-app shortcuts, persistent dock, paged icon grid, page indicators and first-run starter layout.
- Added swipe-up app library, horizontal page switching, long-press app management, home order and dock actions, Home-intent routing and backup support for workspace/dock state.
- Kept the Android widget host, custom widget studio, icon-pack support, hidden apps, folders, theme code studio, focus timer, diagnostics and GitHub Actions APK workflow.

## 0.3.0 — Widget Studio + Theme Code

- Added 31 built-in custom home cards across time, focus, device, shortcuts, personal and apps.
- Added persistent timer, Pomodoro, stopwatch, countdown, notes, checklist, habit and water counter behavior.
- Added a declarative `thuku-theme-v1` JSON editor with validation, apply, import, export and copy actions.
- Updated GitHub Actions for Android SDK v4, explicit SDK package installation, build/test checks, APK metadata, checksums and failure diagnostics.

## 0.2.0 — Previous source baseline

- Existing Kotlin/Compose launcher with app drawer, pinned apps, folders, focus timer, icon-pack support, appearance controls, Android widget host and local diagnostics.
