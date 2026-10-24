# Thuku Launcher v0.5.0 — implemented feature matrix

This is an inventory of features present in the source, not a list of planned or guaranteed device behaviors. Android/OEM versions, installed providers and user permissions can affect some integrations.

## Workspace and Android Home behavior

1. Android `MAIN` + `HOME` + `DEFAULT` eligibility.
2. Separate launcher-app-list intent filter.
3. System Home intent returns to Thuku's workspace.
4. First-run starter layout from installed app labels with fallback selection.
5. Actual installed launcher-activity discovery using `LauncherApps`.
6. Tap-to-launch real installed activities.
7. Launch fallback for devices where the primary activity launch fails.
8. Saved workspace shortcut order.
9. Workspace grid over multiple pages.
10. Horizontal workspace page switching.
11. Page-indicator navigation.
12. Configurable row count (2–5).
13. Configurable column count (3–6).
14. Empty shortcut slots link to the app library.
15. Persistent bottom dock.
16. Dock capacity setting (3–7 shortcuts).
17. Dedicated app-library button.
18. Dedicated customization entry point.
19. Home-screen clock.
20. Optional date display.
21. Three clock styles (Editorial, Minimal, Mono).
22. Compact home spacing.
23. Configurable swipe-up action.
24. Configurable swipe-down action.
25. Workspace edit lock.
26. Visible default-Home setup action when Thuku is not the current Home app.

## App library and shortcut management

27. Search by original app label.
28. Search by custom label.
29. Search by package name.
30. Grid app drawer.
31. List app drawer.
32. Alphabetical sorting.
33. Package-name sorting.
34. Most-used sorting based on local Thuku launch counts.
35. Recently-used sorting based on local Thuku launch timestamps.
36. Pinned-first sorting control.
37. All-apps filter.
38. Pinned/favorites filter.
39. Hidden-apps filter.
40. Social category heuristic.
41. Media category heuristic.
42. Games category heuristic.
43. Tools category.
44. Pin/unpin a workspace shortcut.
45. Add/remove a dock shortcut.
46. Move shortcut earlier/later.
47. Add an app to a folder.
48. Remove/hide/unhide apps inside Thuku's UI.
49. Rename/reset a shortcut label locally.
50. Open Android App Info for an app.
51. Open Play Store lookup for an app.
52. Copy an app's package name.
53. Share a Play Store URL for an app.
54. Start Android's uninstall-confirmation flow for non-system apps.
55. Recent-launch chip row based on local data.
56. No Android Usage Access permission required for Thuku's own history.

## Visual system and personalization

57. Eight built-in accent presets.
58. Android dynamic dark colors on compatible Android versions.
59. Glass-like translucent surfaces.
60. Opaque surface option.
61. Animated screen transitions toggle.
62. Circle icon mask.
63. Squircle icon mask.
64. Rounded icon mask.
65. Square icon mask.
66. Adjustable icon scale.
67. Adjustable label scale.
68. Show/hide app labels.
69. System wallpaper backdrop option.
70. User-selected custom photo wallpaper.
71. Bounded-resolution custom image decode.
72. Wallpaper dim slider.
73. Icon pack intent-action discovery.
74. `appfilter.xml` mappings for common icon packs.
75. Original-icon fallback when an icon-pack mapping is missing.
76. Workspace and dock preferences survive relaunch.
77. Versioned JSON configuration export.
78. Bounded JSON configuration import.
79. Declarative `thuku-theme-v1` JSON code studio.
80. Bounded theme input and validation.
81. No execution of imported Kotlin/JavaScript/shell source.

## Built-in card and system shortcut catalogue (67 distinct types)

82. Digital clock.
83. Analog clock.
84. Date and week card.
85. World clocks.
86. Month calendar.
87. Countdown.
88. Timer.
89. Stopwatch.
90. Pomodoro.
91. Focus goal.
92. Battery level.
93. Battery details.
94. Storage usage.
95. Memory snapshot.
96. Device info.
97. Network connection status.
98. Wi-Fi settings shortcut.
99. Bluetooth settings shortcut.
100. Display settings shortcut.
101. General system settings shortcut.
102. Weather search shortcut.
103. Music app shortcut.
104. Digital wellbeing/usage settings shortcut.
105. Daily thought.
106. Private quick notes.
107. Persistent task list.
108. Daily habit check.
109. Water counter.
110. Pinned apps card.
111. App library stats.
112. App search shortcut.
113. Phone dialer shortcut.
114. Messaging app shortcut.
115. Contacts app shortcut.
116. Calendar app shortcut.
117. Email app shortcut.
118. Browser app shortcut.
119. Maps app shortcut.
120. Calculator app shortcut.
121. Camera app shortcut.
122. Clock/alarm shortcut.
123. App marketplace shortcut.
124. Android document picker.
125. Notification settings shortcut.
126. Installed app management shortcut.
127. Thuku notification settings shortcut.
128. Battery settings shortcut.
129. Storage settings shortcut.
130. Privacy settings shortcut.
131. Security settings shortcut.
132. Accessibility settings shortcut.
133. Location settings shortcut (opens settings; does not read location).
134. Language/locale settings shortcut.
135. Date/time settings shortcut.
136. Sound settings shortcut.
137. Default Home app settings shortcut.
138. Internet connectivity panel shortcut.
139. Display panel shortcut where the device supports it.
140. Volume panel shortcut where supported.
141. Usage-access settings shortcut.
142. Wallpaper selection shortcut.
143. Do Not Disturb settings shortcut.
144. VPN configuration shortcut.
145. Cast settings shortcut.
146. Keyboard/input-method settings shortcut.
147. NFC settings shortcut.
148. Mobile-network settings shortcut.

## Integrations, recovery and security

149. Android `AppWidgetHost` with system widget picker.
150. Widget ID persistence and provider configuration flow inherited from Android.
151. Local timers/countdowns derived from saved wall-clock timestamps.
152. Local note/task/habit/water data.
153. Optional city-based Open-Meteo geocoding.
154. Optional Open-Meteo current weather and temperature-unit setting.
155. Network timeout/response size bounds for weather requests.
156. No GPS/fine/coarse location permission for the weather feature.
157. Clickable Open-Meteo provider attribution.
158. Optional media-session metadata and previous/play-pause/next controls.
159. User-controlled notification-listener permission for media sessions.
160. Minimal notification listener which does not process/store notification events.
161. Bounded app launch-history records stored on device.
162. App-private crash diagnostics with bounded report size.
163. Diagnostics view/clear action.
164. `allowBackup=false` in the manifest (launcher preference export is explicit).
165. Bounded JSON imports and validation.
166. Bounded photo-wallpaper decoding.
167. Explicit fallback paths for many system Settings intents.
168. GitHub Actions source checks, tests, APK build, build metadata, checksums and artifact upload.

## Not claimed in v0.5.0

- Full drag-and-drop/free-position placement between workspace pages and dock.
- Native dynamic/pinned app-shortcut popups/pinning, contact or web universal search, private Quickstep/recent-task integration, notification-shade replacement, automatic task killing, privileged system toggles, or live video wallpaper engine.
- Full OEM icon-font editors or complete compatibility with every proprietary icon-pack extension.
- All 67 cards as separately publishable Android `AppWidgetProvider`s; they are built into the launcher UI.
- Production signing, Play Store policy approval, successful Gradle/device testing before the project runs its required workflow.
