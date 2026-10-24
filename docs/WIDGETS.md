# Widget Studio

Thuku provides two widget systems in the home screen:

1. **31 native Thuku cards** built into the launcher, stored locally, with editable title/accent and compact mode.
2. **Android app widgets** from providers installed on the phone, added with Android's widget picker. These retain the provider's own permissions, network use, setup flow, and behavior.

## Built-in catalogue

- **Time:** Digital clock, Analog clock, Date & week, World clocks, Month calendar, Countdown, Timer, Stopwatch.
- **Focus:** Pomodoro, Focus goal.
- **Device:** Battery level, Battery details, Storage, Memory snapshot, Device info.
- **Shortcuts:** Connection status, Wi-Fi shortcut, Bluetooth shortcut, Display shortcut, System settings, Weather shortcut, Music shortcut, Digital wellbeing.
- **Personal:** Daily thought, Quick notes, Task list, Daily habit, Water tracker.
- **Apps:** Pinned apps, App library stats, App search.

## Behavior and limitations

- Countdown dates use Android's native date/time pickers. Timer and stopwatch state is saved with widget configuration and calculated against wall-clock timestamps, so displayed remaining time catches up after you return. These are in-launcher timers, not Android alarm/notification services; an alert is not guaranteed if Android stops the app process.
- Notes, tasks, widget layout choices, water counts, habit state, and theme settings are stored in private app preferences/files. Daily counters reset when the launcher next loads their widget data after a local date change.
- The weather card intentionally opens an external weather search rather than showing made-up conditions. For live forecasts, add a weather provider's Android widget from the Android widget picker.
- The music card opens a music app. It does not claim to display track details or control playback because those capabilities require additional media-session/notification access and provider/device-specific testing. You can add your music app's official Android widget instead.
- Connection state is read-only. Wi-Fi, Bluetooth, display, and usage settings cards open the relevant Android Settings screen; they do not silently change protected system settings.
- Storage and RAM are informational snapshots, not cleaners or task killers.
- The custom widget cards are components of Thuku's Compose home screen, not stand-alone `AppWidgetProvider` widgets that can be added to other launchers.
