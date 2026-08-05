# Changelog

## Unreleased

### Added

- Added TiDB-backed cloud accounts, daily/monthly/yearly personal statistics, and an all-user traffic leaderboard.
- Added WebDAV backup and restore for links, User-Agent profiles, runtime settings, and today's local traffic statistics.
- Added S3-compatible backup and restore using AWS Signature Version 4 with path-style bucket access.
- Added encrypted local storage for WebDAV passwords and S3 access credentials.

### Changed

- Preconfigured the hosted cloud API address for new account sessions.
- Backup files exclude cloud login tokens, account passwords, WebDAV passwords, and S3 secret keys.

## 1.0.7 - 2026-08-04

### Added

- Added a background-service notification as soon as **后台常驻** is enabled and the app starts.
- Added direct **暂停** and **继续** actions to the notification. These actions control the running traffic task without opening `MainActivity`.
- Added a **GitHub 更新** button beside **检查更新** to open the project's GitHub Releases page.

### Changed

- The notification remains visible while background keep-alive is enabled, including when traffic is paused or has not started.
- The notification title and text now distinguish an active traffic task from an idle background service.
- Updated the app version to `1.0.7` (`versionCode 8`).

## 1.0.6

- Improved background traffic reliability and foreground-service notification handling.
- Added total traffic consumption to the notification.
- Added named User-Agent management with a built-in Android Chrome User-Agent.

## 1.0.5

- Refined traffic controls, settings, and the online update interface.

## 1.0.4

- Added the signed GitHub Actions release workflow and release version verification.
