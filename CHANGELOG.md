# Changelog

## Unreleased

## 1.0.26 — 2026-10-30

### 改动三项（用户真机反馈）

**1. 流量单位扩展到 TB / PB / EB，精度提升到 3 位小数**

`formatBytes` 原来只到 GB（`10244 GB` 显示 `10244.00 GB`，数字太长且不好读）。
现在自动进位：

```
5 GB        → 5.00 GB
10244 GB    → 10.004 TB
100 TB      → 100.000 TB
1.5 PB      → 1.500 PB
```

TB 及以上用 **3 位小数**：因为 10244 GB 实际是 10.004 TB，两位小数会显示成 10.00，
看不出与 10.0 TB 的区别。GB 及以下保持 2 位。
`TrafficKeepAliveService` 里的同名函数同步修改，避免通知栏数字不一致。

**2. 链接状态点重新配色（两套主题各用各的）**

原来 `doneGreen = 0xFF5E6E5A` 是**深色/浅色主题共用的暗灰绿**，所以在任何主题下都偏暗不显眼。

新配色（深色主题 / 浅色主题）：

| 状态 | 深色 | 浅色 | 说明 |
|---|---|---|---|
| 运行中 | `#3E9EFF` 蓝 | `#1565C0` 蓝 | 正在刷 |
| 勾选等待 | `#FFC53D` 黄 | `#B8860B` 暗金 | 已勾选待执行 |
| 完成 | `#34C759` 绿 | `#1E8E3E` 深绿 | 刷完了 |
| 失败/网络不通 | `#E5484D` 红 | `#C62828` 深红 | 报错 |
| 未勾选/已停止 | `#6B6B6B` 灰 | `#9E9E9E` 灰 | 不执行 |

圆点从 9dp 单层改为 **14dp 光晕 + 9dp 实心**（光晕同色 22% 透明），
在两种主题下都足够醒目。运行中的速度数字也从橙色改为蓝色，与状态点一致。

**3. 测速界面链接任务排序：勾选的优先**

原来按 `links` 原始顺序展示。刷第 4/5/6 条时，它们排在列表后面，要往下翻才能看到。

现在 `filter { enabled } + filter { !enabled }`：勾选的按原顺序排在前面，
未勾选的按原顺序排在后面。刷 4/5/6 条时，显示顺序就是 4、5、6、1、2、3、7、8、9。

`filter` 是稳定操作，不会打乱同组内的原始顺序。
链接管理页（`NpxNewUi2.kt`）保持原顺序不动，避免管理时顺序跳来跳去。

## 1.0.25 — 2026-10-30

### 新增：运行中可调并发线程

之前「并发线程」滑块在任务运行中是禁用的，标注「运行中锁定」，要改线程数必须先停止任务，
对刷流量场景很不方便（停下来就断了）。

改动两处：

1. **UI 解除锁定**（`NpxNewUi.kt`）
   滑块 `enabled = !vm.isRunning` → `enabled = true`，标注改为「运行中可调」。
   旧版 MainActivity 里的滑块本来就是 `enabled = true`，只有新 UI 禁用了。

2. **「活跃线程」运行中实时同步**（`MainActivity.kt` 的 `updateThreadCount`）
   原来只在 `!isRunning` 时更新 `activeThreadCount`，运行中调整后界面数字不变，
   看起来像没生效。现在始终同步。

底层本来就支持动态扩缩，无需改下载逻辑：worker 循环每轮都会
`computeActiveThreadCount(...)` 重算 `desiredThreads`，
`if (index >= desiredThreads) { delay(200); continue }` 在发请求前就跳过——
调大时新 worker 立刻加入，调小时超出部分自动停止发请求。

## 1.0.24 — 2026-10-30

### 优化：小文件动态切片

**问题**：0.6MB 的 gif 刷 5GB，速度只有 1.9-8 MB/s。用 curl 实测单次请求开销：

| 方式 | 耗时 | 有效吞吐 |
|---|---|---|
| 整文件 GET（0.6MB） | 0.11-0.18s | **3.8-6.2 MB/s** |
| 32KB Range 分片 | 0.06-0.08s | 0.39-0.58 MB/s |

分片每次只传 32KB 却要付完整的 TTFB（约 50ms），**整文件 GET 的单次吞吐是分片的 9-10 倍**。

**三处改动**：

1. **小文件不分片**（`SMALL_FILE_NO_SLICE_BYTES = 2MB`）
   `contentLength ≤ 2MB` 时 `nextRangeHeader` 返回 `null`，走整文件 GET。
   每个 worker 独立下载一份完整文件，吞吐量按并发数线性放大。

2. **动态切片按 TTFB 反推**（替代按文件大小拍脑袋分档）
   ```
   chunk = 实测速度 × 3 × TTFB(50ms)
   ```
   每片传输时长约为 TTFB 的 3 倍，请求建立开销占比降到 25% 以下。
   速度慢时自动缩小分片（保证多线程铺得开），速度快时自动放大（减少请求数）。
   上限仍限制在文件大小的 1/8，避免分片过大导致并发铺不开。

3. **小文件循环不再空等 250ms**
   原代码下载成功后 `retryBackoffMs = 250ms` 然后 `delay(250)`，
   对 80ms 就能下完的文件等于白等 3 倍时间，实测效率只有 24%。
   小文件走 `SMALL_FILE_LOOP_DELAY_MS = 5ms`，下完立刻发起下一轮。

### 实测记录

- curl 单次：整文件 GET 3.8-6.2 MB/s vs 32KB 分片 0.39-0.58 MB/s
- 容器内 1.0.23 版本同一场景 7.99 MB/s（受咪咕单文件限速）

## 1.0.23 — 2026-10-30

### 修复

- **真机速度慢（P0，1.0.21 引入的回归）**
  两个原因叠加：

  1. **OkHttp Dispatcher 并发被压到 5**：1.0.21 把「每 worker 一个 OkHttpClient」改成共享单例，
     但漏配 Dispatcher，用的是 OkHttp 默认 `maxRequestsPerHost = 5`。64 个 worker 共用
     5 个并发额度，真机上咪咕单流约 10.6MB/s × 5 ≈ 53MB/s。
     修复：`Dispatcher` 设 `maxRequests / maxRequestsPerHost = 64 × 4 = 256`。
     容器里测不出来，因为 5 个连接就能跑满容器的 138MB/s 天花板。

  2. **小文件被错误降并发**：自适应按文件大小收敛，0.6MB 的 gif 被降到 4 线程。
     但「小文件重复刷大流量」场景下，并发数就是吞吐量本身——实测 0.6MB gif 刷 5GB
     只有 **1.91MB/s**，用户体感就是「网速很慢」。
     修复：新增 `SMALL_FILE_REPEAT_THRESHOLD = 100`，当「目标 ÷ 文件大小」的重复次数
     ≥ 100 时不降并发，按用户设定跑。咪咕悦看重复 7925 次 → 放开到 64 线程；
     96MB 以上的文件重复次数少，仍按原规则收敛不受影响。

  3. 顺带把小文件分片从 128KB 降到 32KB（`MIN_ADAPTIVE_CHUNK_SIZE_BYTES`），
     0.6MB 文件原本只有 5 个分片，64 线程里只有 5 个能干活；现在有 21 个分片。

### 实测记录

| 场景 | 线程 | 速率 |
|---|---|---|
| 0.6MB gif 刷 5GB（1.0.22 及以前） | 4（被压） | 1.91 MB/s |
| 同上，1.0.23 | 64（放开） | 待真机验证 |

## 1.0.22 — 2026-10-30

### 修复

- **点击「开始」没反应（P0）**
  `parseTasks()` 中 `if (!isUnlimitedTarget(targetBytes) && initialConsumed >= targetBytes) continue`
  会跳过「已消耗 ≥ 目标」的链接。月初刷过之后 `consumedBytes` 一直保留，用户把目标设成
  比已消耗小的值（或目标不变但已达）时，`parsed` 为空，`start()` 只把状态栏改成
  「所选链接已完成，可修改目标后重跑」——而状态栏在总进度下方、滚动位置不对时根本看不见，
  用户看到的就是「点了完全没反应」。
  修复：`start(resetCompleted: Boolean = true)`，点击开始时把**已达标**链接的
  `consumedBytes` 归零（未完成的保留断点）；自动续跑调用 `start(resetCompleted = false)` 保住断点。
  实测：目标 50GB / 已消耗 100GB 的链接，修复前点开始无任何反应，修复后正常执行
  `385.07 MB / 50.00 GB`。

## 1.0.21 — 2026-10-30

### 修复

- **明文 http 链接无法下载**（P0）
  `AndroidManifest.xml` 中 `usesCleartextTraffic` 由 `false` 改为 `true`。
  修复前所有 `http://` 链接（如咪咕音乐 CDN）被系统直接拦截，且异常被 worker 静默吞掉，
  界面只显示「运行中」但速率恒为 0，无法自查。修复后该类链接可正常下载。

- **下载失败原因不可见**
  新增 `lastErrorText` 状态与 `friendlyErrorText()` 异常翻译层，在状态栏下方以红字显示
  `⚠ <原因>`，覆盖：明文 http 被拦、连接超时、域名解析失败、TLS/证书错误、
  403 拒绝访问、404 链接失效、连接被重置、无法建立连接。
  错误推送做 3 秒节流，避免高频重组。任务成功完成时自动清除。

- **连接池配置过小**
  `buildDownloadClient()` 原为每个 worker 新建一个 `OkHttpClient`，且
  `ConnectionPool(1, 1, 1s)` —— 每个池只保留 1 条连接、1 秒即过期，
  64 并发时每个 Range 请求都要重新建立 TCP 连接。
  改为进程内 `@Volatile` 共享单例 + `ConnectionPool(128, 5min)`，Range 请求复用长连接。

### 优化

- **小文件自适应并发**
  新增 `adaptiveConcurrencyFor(contentLength, requested)`，按文件大小收敛并发数：
  `<2MB → 4`、`<32MB → 8`、`<128MB → 16`、`<512MB → 32`、`≥512MB → 用户设定`。
  部分 CDN 按连接数限流，小文件开满线程反而更慢（实测 101MB 文件 64 线程 21MB/s、
  16 线程 44MB/s）。A/B 实测同一 195MB 文件 8/16/32 线程均为 ~100MB/s，
  降并发不掉速，仅减少连接开销。

### 新增

- **应用宝 CDN 直链 8 条**（`docs/applinks-yyb.json`，均支持 Range/206）
  王者荣耀 1667MB、QQ 375MB、钉钉 349MB、抖音 346MB、微博 226MB、
  小红书 173MB、淘宝 90MB、京东 79MB。
  取自 `a.app.qq.com/o/simple.jsp?pkgname=<包名>` 页面中的 `imtt.dd.qq.com` 直链
  （`imtt2.dd.qq.com` 为 EdgeOne 防爬空壳，不可用）。
  单流 41MB/s、64 并发可达 203MB/s，显著优于咪咕 CDN 的 10.6MB/s / 138MB/s。

### 已知限制

- 连接池修复的实际提速效果**未在真机验证**。测试环境（redroid 容器）网络先饱和在
  138MB/s（`%sirq` 76%，4 核跑满，veth 软中断瓶颈），两个不同 CDN 均精确卡在该值，
  因此无法区分连接池改动的贡献。需在真机复测确认。

### 实测记录

- 应用宝链接 8 条全部可用，连续 9 个 10 秒窗口稳定 138.5MB/s，零错误。
- 404 链接实测正确显示 `⚠ 链接已失效 404（请更换下载地址）`。
- 195MB 文件日志确认 `adaptiveThreads=32 (requested=64)`。
- 断点续跑：`am force-stop` 后重启，日志 `auto-resuming interrupted run (age=57s)`，
  consumedBytes 从 8.02GB 续上，未归零。

## 1.0.20 - 2026-10-16

### Fixed

- 后台存活修复：下载任务在应用被系统后台回收后不再中断 — 下载改到 `applicationScope` 协程域运行，Activity 销毁（`onCleared`）不终止进行中的任务。
- 前台服务改用 `specialUse` 类型（原 `dataSync`），规避 Android 14+ 对 dataSync 类前台服务的 6 小时强制超时；并新增 `onTimeout` 自重启兜底（单参签名，兼容 SDK 34）。
- `TrafficKeepAliveService` / `MainActivity` / `AndroidManifest` 三处协同调整，提升后台保活能力（国内 ROM 后台查杀场景）。

## 1.0.19 - 2026-10-13

### Added

- Added an optional encrypted **保存密码** setting on the cloud login screen.
- The last cloud username is restored automatically when opening the login screen.

### Fixed

- **鸿蒙白屏修复**：HarmonyOS 环境无法加载 Google Cronet provider 导致启动白屏 — 增加 Cronet 初始化回退（fallback），Cronet 不可用时回退到系统网络栈。
- Extended cloud login tokens to keep long-running statistics sessions signed in.
- Preserved saved login credentials when logging out, while still clearing the active session token.


## 1.0.18 - 2026-09-08

### Fixed

- Migrated saved cloud sessions from the retired HTTP/IP endpoint to the HTTPS NAS API endpoint after upgrading.

## 1.0.17 - 2026-09-08

### Added

- Added public IP information on the speed test page, including IP address, location, ASN, and ISP.
- Added the NAS cloud API endpoint to the release configuration.

### Fixed

- Kept the runtime metrics in a fixed three-column row and truncated long values cleanly.

## 1.0.16 - 2026-08-16

### Fixed

- Automatically refresh the selected cloud ranking period when an authenticated user opens the statistics tab. Signed-out users do not trigger a cloud statistics request.

## 1.0.15 - 2026-08-16

### Fixed

- Fixed the monthly and yearly cloud ranking tabs continuing to display the previously loaded daily result. Switching ranking periods now immediately loads the selected period without triggering the manual synchronization cooldown.

## 1.0.14 - 2026-08-06

### Changed

- Routed in-app APK update downloads through `https://ghproxylist.com/` before the GitHub asset URL.
- Reordered settings: cloud account and statistics, User-Agent, local JSON import, background operation, backup and restore, then online update.

## 1.0.13 - 2026-08-05

### Added

- Added startup recovery from the app-owned `Android/media/com.example.networkpanelx/Network Panel X/network-panel-x.json` copy when local links are empty.
- Moved **导入本地 JSON** to **设置** -> **本地 JSON**.
- Kept the Download-folder import fallback for Android installations where scoped storage blocks automatic access to an old public file.

## 1.0.12 - 2026-08-05

### Added

- Added an automatic public local backup at `Download/Network Panel X/network-panel-x.json`. The folder and initial JSON are created on first app launch, then links and runtime settings update the same file.
- Added **导入本地 JSON** in **链接管理** for recovering the public local backup after an uninstall or cleared app data.

## 1.0.11 - 2026-08-05

### Added

- Added automatic cloud synchronization every 5 minutes while a traffic task is running.
- Added per-account JSON configuration snapshots for links, UA profiles, runtime settings, and today's local statistics. Empty reinstalled clients restore the snapshot after login.

### Changed

- Manual leaderboard synchronization now has a 60-second cooldown and prevents overlapping sync requests.

## 1.0.10 - 2026-08-05

### Fixed

- Fixed cloud ranking synchronization dropping new local traffic whenever the existing cloud daily total was larger than the device's unsynced total.
- Added per-installation daily contributions. A sync now adds only traffic newly generated by that app installation; repeating sync is idempotent.

### Changed

- Added the `traffic_device_daily` TiDB migration used to preserve per-device daily sync state.

## 1.0.9 - 2026-08-05

### Added

- Added TiDB-backed cloud accounts, daily/monthly/yearly personal statistics, and an all-user traffic leaderboard.
- Added versioned WebDAV backup and restore with directory listing through `PROPFIND`.
- Added versioned S3-compatible backup and restore using AWS Signature Version 4 and `ListObjectsV2`.
- Added an online backup browser sorted newest-first with 10 items per page and per-version restore confirmation.
- Added encrypted local storage for WebDAV passwords and S3 access credentials.

### Changed

- Built the hosted cloud API address into login and registration, removing the end-user API address field.
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
