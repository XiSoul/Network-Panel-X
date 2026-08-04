# NetworkPanelX (网络面板X)

Android app for batch traffic consumption and network speed testing with user-defined download URLs.

## Version

Current version: `1.0.7`

## Features

- Add multiple links/tasks: each task has `URL + target GB`.
- Select links in **链接管理** and run them sequentially in priority order.
- **一键测流**: in **链接管理**, click `一键测流` to automatically start speed testing for selected links with a temporary target of `0.1GB` per link.
  - This does **not** overwrite the target traffic value saved in each link.
  - Each selected link starts the quick test from `0` for this run.
  - Links without a URL will be marked as `请填写URL`.
- Normal `开始` still uses the target GB configured in each link.
- Target traffic `0GB` means **unlimited traffic**: the link keeps downloading until manually stopped or failed.
- Auto stop when each finite task reaches target bytes.
- Manual stop support.
- Per-task progress, total progress, real-time speed, active thread count, and current chunk size display.
- Adjustable thread count limit for concurrent download workers.
- Optional background keep-alive while running.
- Background service notification appears as soon as background keep-alive is enabled.
- Pause and resume traffic directly from the notification without opening the app.
- Check GitHub Releases and open the project release list from **在线更新**.
- Light/dark theme switching.

## Settings

The **设置** tab provides runtime tuning options:

- **Fixed chunk size**: set Range request chunk size in MB.
  - Supports decimal values such as `0.05`, `0.1`, `0.5`, `1`, `32`.
  - Valid range: `0.05MB` to `256MB`.
- **Dynamic tuning**: automatically adjusts chunk size and active thread count.
  - The app first probes the target file size via `HEAD` or `Range: bytes=0-0`.
  - Small files, such as image URLs around `0.4MB`, use KB-level chunks instead of large 32MB chunks.
  - Larger files gradually use larger chunks.
- **Global User-Agent**:
  - Saved in settings and used by download/probe requests.
  - Empty UA means no default Chrome UA is forced.
  - Multiple UA values can be saved and selected from a dropdown.
- **Per-link User-Agent**:
  - Each link can choose one saved UA in **链接管理**.
  - Empty per-link UA means the link follows the global UA.
  - If both per-link UA and global UA are empty, requests use an empty UA.

## Build

Debug build:

```bash
./gradlew assembleDebug
```

Release build:

```bash
./gradlew assembleRelease
```

The release package is signed with the project release keystore:

```text
app/signing/networkpanelx-release.jks
alias: networkpanelx
```

Signing passwords should be kept only in local ignored files such as `local.properties` or `signing-secrets.txt`.
Do not commit signing passwords or keystore files to Git.

## Release Output

The latest signed release APK is generated at:

```text
app/build/outputs/apk/release/app-release.apk
```

For the current version, a copied release artifact can also be named:

```text
app/build/outputs/apk/release/1.0.7.apk
```

## Open In Android Studio

1. Open Android Studio.
2. Click `Open`.
3. Select this folder: `E:\document\Network-Panel-X`.
4. Wait for Gradle sync.
5. Run on emulator or physical device.

## Notes

- Please use URLs that you have permission to test.
- The app continuously downloads and discards data until the target is reached, or forever for unlimited tasks.
- Keep your device on power and stable network for long-running tasks.
- The `一键测流` feature is intended for quick 0.1GB-per-link speed testing and will not change saved link target values.
