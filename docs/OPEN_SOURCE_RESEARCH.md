# Open-source launcher research and implementation notes

Research date: 24 October 2026. The projects below were reviewed through their repository READMEs/licenses and Android's official documentation. Their feature sets and licenses can change; re-check their current `LICENSE`, notices and submodules before any direct code reuse.

## Projects reviewed

| Source | What it demonstrates | License / reuse note | Thuku decision |
|---|---|---|---|
| [ARK-launcher](https://github.com/jrs8205/ARK-launcher) | Kotlin, Jetpack Compose, Material 3; paged workspace, dock, free placement/drag-drop, app drawer search, hidden/renamed apps, system widgets, shortcuts and backup/restore. The README identifies Apache-2.0. | Apache-2.0 has notice and license conditions. | Used as a design checklist for workspace/dock, customizable drawer, app management, safe theme flow and backup; no ARK source files were copied. |
| [Kvaesitso](https://github.com/MM2-0/Kvaesitso) | Search-focused home experience; places discovery before multi-page browsing. Repository states GPL-3.0. | A direct code derivative is subject to GPL terms; preserve notices and provide corresponding source as required. | Applied the product lesson of strong app search/category entry, without copying GPL code. |
| [KISS Launcher](https://github.com/Neamar/KISS) | Fast local prefix/search launch, remembers frequently selected results, reduces visual clutter. Repository states GPL-3.0-or-later. | GPL-3.0-or-later conditions matter if code is copied/modified/distributed. | Added bounded on-device launch counts/recent history and most-used/recently-used drawer sorts without importing KISS code. |
| [Lawnchair](https://github.com/LawnchairLauncher/lawnchair) | Launcher3-based launcher with Material You/system color theming, search, icon packs/fonts and widget support. Repo reports an “Other” license classification, so inspect the exact branch/component files and third-party notices. | Do not assume the whole repository is under one permissive license. | Added Android dynamic colors where supported and kept widget hosting through public Android APIs; no Lawnchair or Launcher3 implementation was copied. |
| [Fossify Launcher](https://github.com/FossifyOrg/Launcher) | Customizable, privacy-oriented launcher. Repository states GPL-3.0. | Same GPL reuse/attribution obligations for direct derivations. | Kept launch history and settings local, made network features opt-in, and avoided ads/analytics SDKs. |
| [Official `LauncherApps` API](https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps) | `getActivityList` and `startMainActivity` provide real launcher activity discovery and launch support, with visible managed profiles where available. | Android public API documentation. | Use the real Android launcher API instead of sample cards or a fixed hard-coded app list. |
| [Android widget-host guide](https://developer.android.com/develop/ui/views/appwidgets/host) | Defines `AppWidgetHost`, persistent widget IDs, host views, provider configuration and sizing obligations. | Android public API documentation. | Retain native third-party Android widgets as a separate system from Thuku's built-in Compose cards. |
| [AOSP app-shortcut guide](https://source.android.com/docs/core/display/app-shortcuts) | Explains manifest/dynamic/pinned shortcuts; AOSP Launcher3 is a reference. | AOSP code/docs licensing applies to any copied code. | Kept actual `LauncherApps` discovery/launch; full long-press dynamic shortcut surfacing/pinning is not claimed as implemented yet. |
| [Open-Meteo terms](https://open-meteo.com/en/terms) | Current-condition and geocoding HTTP APIs; free API terms describe a non-commercial limit and CC-BY 4.0 acceptance. | Re-check before redistributing; do not assume the free API is suitable for an ad/subscription business. | Optional manual-city lookup; the UI identifies/links provider, uses HTTPS, avoids GPS, bounds timeouts/responses. |

## How the uploaded Nil Launcher APK was handled

The user-provided file was `Nil Launcher v18.0 - Premium.apk`. I inspected its ZIP structure and searched its resource strings; the archive contains text such as “Add Widget”, “Music widget”, “System Widgets”, “Get Premium” and default-launcher flows. The archive contains compiled manifest/DEX/resources and strings, along with duplicate/unusual resource paths; its file listing did not expose a separate source tree, `LICENSE`, `NOTICE` or `COPYING` file. An APK by itself does not prove open-source licensing or supply corresponding source and license notices. For those reasons, it was treated as a **feature reference only**: it was not run, installed, bundled, or decompiled into this source package. The high-level inspirations were Home-app onboarding, provider-owned Android widget hosting, and optional music controls; the commercial/premium paywall was not copied.

## License-aware reuse policy

The Thuku project source in this archive carries the MIT license in `LICENSE`. That does not override the licenses of projects it references. “Open source” does not mean every source file may be copied without complying with its license. This build uses independent Kotlin/Compose implementations and platform APIs; it does not bundle source/assets from the named launchers. If code is directly imported later, keep the original copyright/license, satisfy any attribution/source-offer requirements, track the exact origin/commit, and review dependencies and artwork individually before release.

## What v0.5.0 implements now

- A genuine `HOME`/`DEFAULT` intent filter, discovery of installed launcher activities using `LauncherApps`, tap-to-launch, persisted multi-page workspace and dock, app-library search and local recency sorting.
- App pin/unpin, dock management, folders, hide/unhide, shortcut rename, app info, Play Store lookup, share and system-mediated uninstall confirmation.
- Grid/list drawer, alphabetical/package/most-used/recently-used sorting, configurable gestures, workspace lock, icon pack mapping and dynamic color.
- Standard Android widget host/picker plus a separate catalog of 67 native in-launcher card types.
- Optional manual-city Open-Meteo weather, an explicit-grant media-session card, custom wallpaper, bounded JSON configuration import/export, theme JSON without code execution and local crash reports.

## Deliberate limits

- No private Android APIs, root requirement, unauthorized setting toggles, forced app uninstall, or claim of Quickstep/system-recents integration.
- No full free-position drag/drop engine across pages/dock yet; the current workspace has explicit move-earlier/later management and page navigation.
- No implemented native contacts/web universal search, dynamic/pinned launcher-shortcut popup, icon-font editor, notification shade replacement or live notification mirroring.
- Custom Thuku cards are not provider-owned `AppWidgetProvider` components and cannot be placed into other launchers; standard provider widgets use Android's host flow.
- Weather uses network only while its card is shown and enabled; manually entered city is sent to Open-Meteo. The free service terms are non-commercial; consider another provider/terms before any commercial distribution.
- Media-session controls require a user-granted notification-listener permission. Track metadata appears only from active media sessions; some devices/players may not expose it.
- The current project hasn't passed a v0.5.0 Gradle build or physical phone test in the source-generation environment. GitHub Actions and the device checklist are required gates before calling it production-ready.
