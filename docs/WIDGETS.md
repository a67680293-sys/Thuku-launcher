# Widget Studio — v0.5.0

Thuku has two different widget systems.

1. **67 native Thuku cards** rendered inside the launcher workspace, with local settings/content, editable title/accent/compact mode where supported, and a hard cap of 30 card instances saved at once.
2. **Android app widgets** supplied by apps installed on the phone, hosted via Android's `AppWidgetHost` and the system widget picker. These retain each provider's own permissions, network use, configuration activity and behavior.

The 67-card catalog count is the number of selectable Thuku card types—not 67 independent Android widget providers.

## Built-in catalogue (67 types)

### Time and planning
Digital clock, Analog clock, Date & week, World clocks, Month calendar, Countdown, Timer, Stopwatch, Pomodoro, Focus goal.

### Device information
Battery level, Battery details, Storage, Memory snapshot, Device info, Connection status.

### Personal and launcher tools
Daily thought, Quick notes, Task list, Daily habit, Water tracker, Pinned apps, App library stats, App search.

### Everyday app launch targets
Phone dialer, Messages app, Contacts app, Calendar app, Email app, Browser app, Maps app, Calculator app, Camera app, Clock app, App store, File picker.

### System settings and panels
Wi-Fi shortcut, Bluetooth shortcut, Display shortcut, System settings, Weather shortcut, Music shortcut, Digital wellbeing, Notification settings, Installed apps, Thuku notifications, Battery settings, Storage settings, Privacy settings, Security settings, Accessibility settings, Location settings, Language settings, Date & time settings, Sound settings, Default Home app, Internet panel, Display panel, Volume panel, Usage access, Wallpaper picker, Do Not Disturb, VPN settings, Cast settings, Keyboard settings, NFC settings, Mobile network.

## Behavior and limitations

- Countdown, timer and stopwatch state is saved with each card and calculated against stored wall-clock timestamps. These are in-launcher timers, not Android alarm services; a notification/alert is not guaranteed if Android stops the app process.
- Notes, tasks, water count, habit state, focus goal and card settings are stored in private app preferences/files. Daily counters refresh based on the local date when the card data is loaded.
- City weather is a separate home card, not a built-in widget catalog entry. Enter a city in Customize and enable it; Thuku requests current conditions from Open-Meteo over HTTPS. No location permission or GPS is used. Open-Meteo is linked as data provider, and its current free API terms limit the free service to non-commercial use.
- The music card is an optional home component, not the same as a provider widget. It requires explicit notification-listener access for Android media-session metadata and transport controls. The service does not parse/store notification events.
- Connection state and device/storage/RAM metrics are informational. Wi-Fi, Bluetooth, privacy/security, display, battery and other system shortcut cards open Android's own settings; they do not silently modify protected settings.
- App-launching shortcut cards use implicit or Settings intents and have a fallback; available screens depend on the phone/OEM and installed apps.
- Standard app widgets are separate provider-owned views. They can have their own network behavior, permissions and configuration flow. The custom Thuku cards are Compose components of Thuku's home screen, not stand-alone `AppWidgetProvider` components for another launcher.
