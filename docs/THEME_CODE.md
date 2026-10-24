# Thuku Theme Code v1

Theme code is a small declarative JSON format. It changes settings that Thuku already supports; it is **not a program**. The app does not evaluate Kotlin, JavaScript, shell commands, downloaded scripts, or dynamic libraries. This design avoids giving user-supplied text the launcher's Android permissions.

## Example

```json
{
  "format": "thuku-theme-v1",
  "name": "Midnight Volt",
  "accent": "#C9F56A",
  "iconScale": 1.1,
  "columns": 5,
  "showLabels": true,
  "glass": true,
  "wallpaper": true,
  "wallpaperDim": 0.3,
  "clockStyle": "Mono",
  "iconShape": "Squircle",
  "animations": true,
  "showDate": true,
  "compactHome": false
}
```

## Supported keys

| Key | Supported values | Effect |
|---|---|---|
| `format` | Must be `thuku-theme-v1` | Required schema marker |
| `name` | Plain text, max 36 characters | Name shown in the apply confirmation; not saved as a theme library entry |
| `accent` | `#RRGGBB` or `#AARRGGBB` | Accent color for highlights and controls |
| `iconScale` | Number from 0.7 through 1.35 | App icon size; values are clamped safely |
| `columns` | Integer from 3 through 6 | App drawer grid |
| `showLabels` | Boolean | App names under icons |
| `glass` | Boolean | Glass-like/translucent surfaces |
| `wallpaper` | Boolean | Show the current system wallpaper in the background |
| `wallpaperDim` | Number from 0.0 through 0.8 | Background dimming |
| `clockStyle` | `Editorial`, `Minimal`, or `Mono` | Home clock style |
| `iconShape` | `Circle`, `Squircle`, `Rounded`, or `Square` | App icon mask |
| `animations` | Boolean | Screen transitions |
| `showDate` | Boolean | Date under the home clock |
| `compactHome` | Boolean | Tighter home layout |

Missing optional keys keep the currently selected setting. Invalid colors and unsupported schema versions are rejected. Numeric settings are validated and bounded. Files and editor input are capped at 20 KB.

## Use it

Open **Lab → Theme Code Studio**, paste/edit JSON, then tap **Validate & apply**. You can import a JSON file, export the editor contents, or copy the code. The template is a good starting point for custom themes. You can create multiple files with different names and colors; Thuku doesn't load executable plugins from them.
