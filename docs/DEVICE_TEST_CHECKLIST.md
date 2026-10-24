# Device acceptance checklist — v0.5.0

Run on a physical phone in gesture-navigation and three-button navigation modes. The list is a test plan, not a claim that tests have passed.

## Home role and application launching
- [ ] Install the Actions debug APK and open it from the app list.
- [ ] Workspace populates from real installed launcher activities; tap shortcuts and confirm each opens the corresponding app.
- [ ] Use Android's Default apps → Home app picker to choose Thuku; press Home from another app and verify Thuku returns.
- [ ] Swipe up/down to each configured action and use horizontal page switching/page dots.
- [ ] App drawer search matches original/custom labels and package strings.
- [ ] Test Grid/List, each sorting mode, pinned-first sorting and All/Social/Media/Games/Tools/Hidden filters.
- [ ] Rename/reset a shortcut, pin/unpin, add/remove from dock, add to folder, hide/unhide, move earlier/later, copy package, share, open App Info and test Android's uninstall confirmation for a non-system app.
- [ ] Confirm launch history is local, remains after process restart, and no Usage Access prompt is required.
- [ ] Enable Workspace lock and confirm long-press editing is blocked until unlocked.

## Customization and saved state
- [ ] Change accent, Android dynamic color (Android 12+), surface style, animation toggle, icon shape, icon scale, labels, label size, columns, row count, clock style, date, compact mode and dock slot count.
- [ ] Choose a custom photo wallpaper, restart the launcher and verify it remains readable; clear the photo URI and confirm the system wallpaper path works.
- [ ] Test a very large source image, inaccessible/revoked image URI, wallpaper off, display rotation and app resume.
- [ ] Export/import setup JSON; verify new settings round-trip; reject malformed, oversized and wrong-format input.
- [ ] Import theme JSON and test invalid colors, unknown values, oversized input and supported schema marker.
- [ ] Test old v0.4 backup migration to ensure existing workspace order/favorites don't get overwritten.

## Built-in cards, system widgets and network features
- [ ] Open the 67-card catalogue and add one item from every group; check it can be configured, persisted and removed.
- [ ] Test timer/countdown after leaving/returning; remember timers are not guaranteed background alarms.
- [ ] Add/configure/remove a provider-owned Android app widget using the system picker.
- [ ] Enter a real city, enable weather and verify conditions, Fahrenheit/Celsius, refresh and network error handling. Turn the card off and confirm no further weather requests are made while home is visible. Check no location permission was requested.
- [ ] Confirm weather attribution opens Open-Meteo; review its free API terms before commercial use.
- [ ] Enable the media card without listener access: it should offer the system settings link. Grant access explicitly, start media playback and test title/artist/previous/play-pause/next; revoke access and verify the card falls back without a crash.
- [ ] Verify the notification listener does not save or upload notification text.

## Reliability, accessibility and phone compatibility
- [ ] Confirm Actions `clean testDebugUnitTest assembleDebug` succeeds and the APK artifact includes a SHA-256 checksum.
- [ ] Install on Android 8.0+ (min SDK 26) and test on the user's Android version/OEM; also test Android 12+ dynamic color.
- [ ] Test 360 dp width, large font/accessibility display size, long/non-Latin app labels, large app lists, empty workspace pages and folders containing removed apps.
- [ ] Test a paused work profile, a removed/disabled app, unavailable widget provider, missing Play Store, weather offline and no active media session.
- [ ] Review local crash diagnostic export and clear it after the test.

**Do not mark v0.5.0 device-tested until the checklist is performed on actual target hardware.**
