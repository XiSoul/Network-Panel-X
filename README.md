# NetworkPanelX (网络面板X)

Android app for batch traffic consumption testing with user-defined URLs.

## Features

- Add multiple tasks: each task has `URL + target GB`
- One-click start, tasks run sequentially
- Auto stop when each task reaches target bytes
- Manual stop support
- Per-task progress and consumed data display

## Open In Android Studio

1. Open Android Studio
2. Click `Open`
3. Select this folder: `D:\document\codextest`
4. Wait for Gradle sync
5. Run on emulator or physical device

## Notes

- Please use URLs that you have permission to test.
- The app continuously downloads and discards data until target is reached.
- Keep your device on power and stable network for long-running tasks.
