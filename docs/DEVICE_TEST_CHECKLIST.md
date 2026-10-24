# Device testing checklist

The GitHub Actions workflow verifies project layout, runs JVM unit tests, compiles a debug APK, checks that the APK exists, and uploads it with a checksum. Use this checklist on a real Android phone after the first successful CI build; CI compilation cannot test OEM-specific launcher behavior by itself.

## Install / default Home app
- [ ] Download `ThukuLauncher-debug.apk` from the successful workflow artifact and install it.
- [ ] Launch Thuku from the app drawer.
- [ ] Choose Thuku in Android Settings → Default apps → Home app.
- [ ] Press Home, lock/unlock the phone, rotate it, and resume after process recreation.
- [ ] Confirm the system launcher can be selected again from Android Settings.

## App library
- [ ] App labels, icons, work profile apps (if present), search, filters, pinned apps, hidden apps, and folders.
- [ ] Try launching a disabled/uninstalled app and confirm an error is shown instead of a crash.
- [ ] Confirm hidden apps can be unhidden and folders can be edited/deleted.

## Widgets
- [ ] Open Widget Studio and add at least one item from every category.
- [ ] Change a card title/accent/compact mode, navigate away, and verify it persists.
- [ ] Check notes/tasks survive force-stop/relaunch.
- [ ] Test timer start, pause, resume, reset, expiration, and switching apps while a timer is active.
- [ ] Test stopwatch pause/resume/reset and countdown date selection.
- [ ] Add a third-party Android widget through the system picker, configure it, then remove it.
- [ ] Try cancellation or provider failure while adding a system widget; no orphaned widget ID should remain.

## Theme code and reliability
- [ ] Apply the supplied theme JSON template.
- [ ] Apply a modified accent, icon size and column count.
- [ ] Try invalid JSON, invalid hex colors, unsupported format and overlarge content; the app should show an error without crashing.
- [ ] Export/import settings and a theme JSON file.
- [ ] Open Tools → Reliability after a deliberately recoverable error and test diagnostic export/clear.
- [ ] Verify no network permission appears in the installed app manifest; weather opens only an external browser/app on tap.

## Accessibility / OEM checks
- [ ] Use system font scaling, dark mode, gesture navigation and three-button navigation.
- [ ] Test at narrow screen width and with keyboard open in notes/tasks/theme editor.
- [ ] Test on Android 8 (API 26) if possible and one current Android version.
- [ ] Verify Android's widget picker and Default Home settings action on the target OEM skin.
