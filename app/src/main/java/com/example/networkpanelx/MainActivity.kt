package com.example.networkpanelx

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import com.google.android.gms.net.CronetProviderInstaller
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ConnectionPool
import okhttp3.CacheControl
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.InputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume
import kotlin.math.max

data class PublicIpInfo(
    val ip: String,
    val location: String,
    val asn: String,
    val isp: String,
)

private const val PREFS_NAME = "traffic_prefs"
private const val PREF_LINKS = "links_json"
private const val PREF_THEME_DARK = "theme_dark"
private const val PREF_THREAD_COUNT = "thread_count"
private const val PREF_USER_AGENT = "user_agent"
private const val PREF_USER_AGENT_LIST = "user_agent_list"
private const val PREF_KEEP_ALIVE = "keep_alive"
private const val PREF_KEEP_SCREEN_AWAKE = "keep_screen_awake"
// P0-8h 修复③：进程被系统杀后的自动续跑标记（正常结束/用户停止会清除）
private const val PREF_AUTO_RESUME = "auto_resume_run"
private const val PREF_AUTO_RESUME_AT = "auto_resume_started_at"
private const val AUTO_RESUME_MAX_AGE_MS = 24 * 60 * 60 * 1000L
private const val PREF_PROGRESS_NOTIFICATION = "progress_notification"
/** 流量显示单位制：GB=十进制(1000，与运营商口径一致) / GiB=二进制(1024，传统习惯) */
private const val PREF_TRAFFIC_UNIT = "traffic_unit"
const val TRAFFIC_UNIT_GB = "gb"
const val TRAFFIC_UNIT_GIB = "gib"
/** 默认 GiB：保持老版本 1024 进制显示不变，避免升级后数字突然变小 */
const val DEFAULT_TRAFFIC_UNIT = TRAFFIC_UNIT_GIB
private const val PREF_DAILY_TRAFFIC_DATE = "daily_traffic_date"
private const val PREF_DAILY_TRAFFIC_BYTES = "daily_traffic_bytes"
private const val PREF_DAILY_TASK_COUNT = "daily_task_count"
private const val PREF_CLOUD_INSTALLATION_ID = "cloud_installation_id"
private const val PREF_CLOUD_DEVICE_CONTRIBUTION_PREFIX = "cloud_device_contribution_"
private const val AUTO_CLOUD_SYNC_INTERVAL_MS = 5 * 60 * 1000L
private const val MANUAL_CLOUD_SYNC_COOLDOWN_SECONDS = 60
private const val PUBLIC_BACKUP_FILE_NAME = "network-panel-x.json"
private const val PUBLIC_BACKUP_RELATIVE_PATH = "Download/Network Panel X/"
private const val PREF_PUBLIC_BACKUP_URI = "public_backup_uri"
private const val LEGACY_ANDROID_APP_USER_AGENT = "Dalvik/2.1.0 (Linux; U; Android 14; Network-Panel-X Build/UP1A.231005.007)"
private const val COMMON_ANDROID_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Mobile Safari/537.36"
private const val DEFAULT_USER_AGENT = COMMON_ANDROID_USER_AGENT
const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0  // 兼容保留；实际换算请用 bytesPerGigabyte()
private const val READ_BUFFER_SIZE = 1024 * 1024
private const val DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES = 2L * 1024L * 1024L

/**
 * 动态切片阈值：小于此大小的文件不做 Range 切片，走「整文件 GET → 立即重试」循环。
 *
 * 原因（2026-10-30 实测）：0.6MB gif 的 TTFB 是 50ms，而整文件传输只要 30ms。
 * 切成 32KB 分片后，每个分片都要付一次 50ms 的 TTFB，分片反而比整文件慢。
 * 整文件 GET 让 64 个 worker 各自下完整个文件再重来，吞吐量直接按并发数线性放大。
 */
private const val SMALL_FILE_NO_SLICE_BYTES = 2L * 1024L * 1024L

/**
 * 小文件循环下载成功后的间隔。原来是 250ms 退避（RETRY_BACKOFF_BASE_MS），
 * 对 80ms 就能下完的文件等于白等 3 倍时间，实测效率只有 24%。
 * 小文件循环不涉及「压测 CDN」风险，给个极短间隔让 worker 立刻发起下一轮。
 */
private const val SMALL_FILE_LOOP_DELAY_MS = 5L

/**
 * 动态切片的时间预算：分片的传输时长应至少是 TTFB 的 3 倍，
 * 否则请求建立开销主导，切片越细越慢。
 */
private const val TTFB_BUDGET_MS = 50L
private const val MIN_ADAPTIVE_CHUNK_SIZE_BYTES = 1024L * 1024L
private const val MAX_ADAPTIVE_CHUNK_SIZE_BYTES = 32L * 1024L * 1024L
private const val MAX_EFFECTIVE_CONCURRENCY = 64

/**
 * 「小文件重复刷流量」的判据：目标字节数 ÷ 文件大小 ≥ 此值，就认为用户是靠反复下载小文件凑量。
 * 0.6MB gif 刷 5GB ≈ 8800 次，远超阈值；而 200MB 文件刷 5GB 只有 25 次，不触发。
 */
private const val SMALL_FILE_REPEAT_THRESHOLD = 100L
private const val CRONET_FAILURE_THRESHOLD = 3
// M-b 修复：错误重试从零延迟改为指数退避（250ms 起，连续失败翻倍，封顶 10s）
private const val RETRY_BACKOFF_BASE_MS = 250L
private const val RETRY_BACKOFF_MAX_MS = 10_000L
// M-f 修复：Range 支持探测（HEAD + 1 字节 GET）总预算 10 秒
private const val PROBE_TOTAL_BUDGET_MS = 10_000L
private const val WORKER_STAGGER_MS = 8L
private const val QUICK_TRAFFIC_TARGET_GB_TEXT = "0.1"
// 应用内更新走公开壳仓库 Network-Panel-X-Releases（而非本仓库）：
// 本仓库历史曾包含未公开信息，壳仓库只存 APK 资产，更新链路与之隔离。
private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/XiSoul/Network-Panel-X-Releases/releases/latest"
const val GITHUB_RELEASES_URL = "https://github.com/XiSoul/Network-Panel-X-Releases/releases"
private const val UPDATE_DOWNLOAD_PROXY_PREFIX = "https://ghproxylist.com/"
const val ACTION_START_FROM_NOTIFICATION = "com.example.networkpanelx.action.START_FROM_NOTIFICATION"
const val ACTION_STOP_FROM_NOTIFICATION = "com.example.networkpanelx.action.STOP_FROM_NOTIFICATION"
const val ACTION_PAUSE_FROM_NOTIFICATION = "com.example.networkpanelx.action.PAUSE_FROM_NOTIFICATION"
const val ACTION_RESUME_FROM_NOTIFICATION = "com.example.networkpanelx.action.RESUME_FROM_NOTIFICATION"

data class SavedUserAgent(
    val name: String,
    val value: String,
    val builtIn: Boolean = false,
)

internal data class DeviceTrafficContribution(
    val consumedBytes: Long,
    val taskCount: Long,
)

private data class CloudSyncResult(
    val selectedStats: CloudTrafficStats,
    val entries: List<LeaderboardEntry>,
    val snapshotError: Throwable?,
)

internal fun inferDeviceTrafficContribution(
    localBytes: Long,
    localTasks: Int,
    cloudBytes: Long,
    cloudTasks: Long,
): DeviceTrafficContribution {
    fun infer(local: Long, cloud: Long): Long = when {
        local <= 0L -> 0L
        local < cloud -> local
        local == cloud -> 0L
        else -> local - cloud
    }

    return DeviceTrafficContribution(
        consumedBytes = infer(localBytes.coerceAtLeast(0L), cloudBytes.coerceAtLeast(0L)),
        taskCount = infer(localTasks.toLong().coerceAtLeast(0L), cloudTasks.coerceAtLeast(0L)),
    )
}

private class DeviceTrafficContributionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun installationId(): String {
        val existing = preferences.getString(PREF_CLOUD_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        return UUID.randomUUID().toString().also {
            preferences.edit().putString(PREF_CLOUD_INSTALLATION_ID, it).apply()
        }
    }

    fun contributionForSync(
        session: CloudSession,
        date: String,
        localBytes: Long,
        localTasks: Int,
        cloudBytes: Long,
        cloudTasks: Long,
    ): DeviceTrafficContribution {
        load(session, date)?.let { return it }
        return inferDeviceTrafficContribution(localBytes, localTasks, cloudBytes, cloudTasks)
            .also { save(session, date, it) }
    }

    fun recordLocalTraffic(session: CloudSession, date: String, bytes: Long, tasks: Int) {
        if (bytes <= 0L && tasks <= 0) return
        val existing = load(session, date) ?: DeviceTrafficContribution(0L, 0L)
        save(
            session,
            date,
            DeviceTrafficContribution(
                consumedBytes = (existing.consumedBytes + bytes.coerceAtLeast(0L)).coerceAtLeast(existing.consumedBytes),
                taskCount = (existing.taskCount + tasks.coerceAtLeast(0)).coerceAtLeast(existing.taskCount),
            ),
        )
    }

    private fun load(session: CloudSession, date: String): DeviceTrafficContribution? {
        val key = key(session)
        if (preferences.getString("${key}_date", null) != date) return null
        return DeviceTrafficContribution(
            consumedBytes = preferences.getLong("${key}_bytes", 0L).coerceAtLeast(0L),
            taskCount = preferences.getLong("${key}_tasks", 0L).coerceAtLeast(0L),
        )
    }

    private fun save(session: CloudSession, date: String, contribution: DeviceTrafficContribution) {
        val key = key(session)
        preferences.edit()
            .putString("${key}_date", date)
            .putLong("${key}_bytes", contribution.consumedBytes.coerceAtLeast(0L))
            .putLong("${key}_tasks", contribution.taskCount.coerceAtLeast(0L))
            .apply()
    }

    private fun key(session: CloudSession): String {
        val scope = "${session.apiBaseUrl.trimEnd('/').lowercase()}\u0000${session.username.lowercase()}"
        return PREF_CLOUD_DEVICE_CONTRIBUTION_PREFIX + UUID.nameUUIDFromBytes(scope.toByteArray()).toString()
    }
}

private object PublicJsonBackup {
    private fun mediaFile(context: Context): File? {
        return context.getExternalMediaDirs().firstOrNull()?.let { File(it, "Network Panel X/$PUBLIC_BACKUP_FILE_NAME") }
    }

    private fun readFile(file: File?): JSONObject? {
        if (file == null || !file.isFile) return null
        return runCatching {
            file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }?.let(::JSONObject)
        }.getOrNull()
    }

    fun read(context: Context): JSONObject? {
        val mediaBackupFile = mediaFile(context)
        readFile(mediaBackupFile)?.let { return it }
        return runCatching {
            val resolver = context.contentResolver
            val preferences = context.getSharedPreferences(PREFS_NAME, 0)
            val savedUri = preferences.getString(PREF_PUBLIC_BACKUP_URI, null)?.let(Uri::parse)
            val candidates = buildList {
                if (savedUri != null) add(savedUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    resolver.query(
                        collection,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                        arrayOf(PUBLIC_BACKUP_FILE_NAME, PUBLIC_BACKUP_RELATIVE_PATH),
                        "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
                        }
                    }
                } else {
                    add(
                        Uri.fromFile(
                            File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "Network Panel X/$PUBLIC_BACKUP_FILE_NAME",
                            ),
                        ),
                    )
                }
            }
            for (uri in candidates.distinct()) {
                val raw = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!raw.isNullOrBlank()) return@runCatching JSONObject(raw)
            }
            null
        }.onFailure {
            Log.i("NetworkPanelX", "没有可读取的公共 JSON 备份", it)
        }.getOrNull()
    }

    fun write(context: Context, document: String) {
        runCatching {
            val file = mediaFile(context) ?: throw IllegalStateException("无法定位共享媒体目录")
            file.parentFile?.mkdirs()
            file.writeText(document, Charsets.UTF_8)
        }.onFailure {
            Log.w("NetworkPanelX", "写入 Android/media JSON 备份失败", it)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val preferences = context.getSharedPreferences(PREFS_NAME, 0)
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val savedUri = preferences.getString(PREF_PUBLIC_BACKUP_URI, null)?.let(Uri::parse)
            val isNewFile = savedUri == null
            val uri = savedUri ?: resolver.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, PUBLIC_BACKUP_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, PUBLIC_BACKUP_RELATIVE_PATH)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                },
            ) ?: throw IllegalStateException("无法创建公共 JSON 备份")
            resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(document) }
                ?: throw IllegalStateException("无法写入公共 JSON 备份")
            if (isNewFile) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            preferences.edit().putString(PREF_PUBLIC_BACKUP_URI, uri.toString()).apply()
        }.onFailure {
            context.getSharedPreferences(PREFS_NAME, 0).edit().remove(PREF_PUBLIC_BACKUP_URI).apply()
            Log.w("NetworkPanelX", "写入公共 JSON 备份失败", it)
        }
    }
}

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkDownloadUrl: String?,
    val releasePageUrl: String,
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpToDate(val latestVersion: String) : UpdateState()
    data class Available(val release: ReleaseInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

data class LinkItem(
    val id: Long,
    val name: String = "",
    val url: String = "",
    val targetGbText: String = "",
    val userAgent: String = "",
    val enabled: Boolean = true,
    val consumedBytes: Long = 0L,
    val status: String = "未开始",
)

internal fun serializeLinksForBackup(links: List<LinkItem>): JSONArray = JSONArray().apply {
    links.forEach { link ->
        put(JSONObject().apply {
            put("id", link.id)
            put("name", link.name)
            put("url", link.url)
            put("targetGbText", link.targetGbText)
            put("userAgent", link.userAgent)
            put("enabled", link.enabled)
        })
    }
}

internal fun mergeDailyTrafficWithCloud(
    localBytes: Long,
    localTaskCount: Int,
    cloudBytes: Long,
    cloudTaskCount: Long,
): Pair<Long, Int> = maxOf(localBytes, cloudBytes.coerceAtLeast(0L)) to maxOf(
    localTaskCount,
    cloudTaskCount.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
)

data class ParsedTask(
    val id: Long,
    val name: String,
    val url: String,
    val targetBytes: Long,
    val initialConsumedBytes: Long,
    val userAgent: String?,
)

private fun isUnlimitedTarget(targetBytes: Long): Boolean = targetBytes <= 0L

private fun hasReachedTarget(consumedBytes: Long, targetBytes: Long): Boolean {
    return !isUnlimitedTarget(targetBytes) && consumedBytes >= targetBytes
}

private fun compareVersions(left: String, right: String): Int {
    val leftParts = left.split(Regex("[^0-9]+"))
        .filter { it.isNotEmpty() }
        .map { it.toLongOrNull() ?: 0L }
    val rightParts = right.split(Regex("[^0-9]+"))
        .filter { it.isNotEmpty() }
        .map { it.toLongOrNull() ?: 0L }
    val partCount = max(leftParts.size, rightParts.size)

    for (index in 0 until partCount) {
        val comparison = (leftParts.getOrElse(index) { 0L })
            .compareTo(rightParts.getOrElse(index) { 0L })
        if (comparison != 0) return comparison
    }
    return 0
}

private fun defaultUserAgentOptions(): List<SavedUserAgent> = listOf(
    SavedUserAgent(
        name = "Android Chrome",
        value = COMMON_ANDROID_USER_AGENT,
        builtIn = true,
    ),
)

class MainActivity : ComponentActivity() {
    private lateinit var trafficViewModel: TrafficViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trafficViewModel = ViewModelProvider(
            this,
            TrafficViewModel.Factory(application),
        )[TrafficViewModel::class.java]
        handleNotificationAction(intent)
        setContent {
            val vm = trafficViewModel
            NpxTheme(dark = vm.isDarkTheme) {
                SystemBarsEffect(isDarkTheme = vm.isDarkTheme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NpxPalette.bg,
                ) {
                    TrafficScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationAction(intent)
    }

    private fun handleNotificationAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_START_FROM_NOTIFICATION -> trafficViewModel.start()
            ACTION_STOP_FROM_NOTIFICATION -> trafficViewModel.stop()
        }
    }
}

object TrafficNotificationCommandBus {
    @Volatile
    private var viewModel: TrafficViewModel? = null

    fun register(viewModel: TrafficViewModel) {
        this.viewModel = viewModel
    }

    fun unregister(viewModel: TrafficViewModel) {
        if (this.viewModel === viewModel) this.viewModel = null
    }

    fun pause() {
        viewModel?.stop()
    }

    fun resume() {
        viewModel?.start()
    }
}

class TrafficViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "TrafficViewModel"
        private const val BACKUP_PAGE_SIZE = 10
        // P0-8h 修复③：进程级标志——FGS 存活期间 VM 重建（用户划走再打开）不得触发
        // 第二次下载循环，否则同批链接双重下载
        @Volatile
        private var processRunActive = false
    }

    var links by mutableStateOf(emptyList<LinkItem>())
        private set

    var isRunning by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("在管理页配置链接后开始测速")
        private set

    /**
     * 最近一次下载错误的可读原因（空 = 无错误）。
     * 修复前 worker 的异常只写进 consumeTask 内的 firstError，UI 上完全不可见——
     * 用户只看到「运行中」但 0 B/s，无从自查（明文 http 被 CLEARTEXT 拦掉就是这样被误诊成 CDN 失效的）。
     */
    var lastErrorText by mutableStateOf("")
        private set

    var threadCount by mutableIntStateOf(8)
        private set
    var activeThreadCount by mutableIntStateOf(8)
        private set
    var userAgentText by mutableStateOf(DEFAULT_USER_AGENT)
        private set
    var userAgentOptions by mutableStateOf(defaultUserAgentOptions())
        private set
    var keepAliveEnabled by mutableStateOf(true)
        private set

    // P0-8h 修复③：恢复检查只跑一次，避免 init 期间 Compose 重组触发重复启动
    private var autoResumeChecked = false
    var keepScreenAwakeEnabled by mutableStateOf(true)
        private set
    var progressNotificationEnabled by mutableStateOf(true)
        private set
    /** 流量单位制：gb（十进制，对齐运营商）/ gib（二进制，传统）。切换后全局 formatBytes 立即生效。 */
    var trafficUnit by mutableStateOf(DEFAULT_TRAFFIC_UNIT)
        private set
    var notificationPermissionGranted by mutableStateOf(true)
        private set
    var ignoringBatteryOptimizations by mutableStateOf(false)
        private set
    var isDarkTheme by mutableStateOf(false)
        private set

    var publicIpInfo by mutableStateOf<PublicIpInfo?>(null)
        private set
    var publicIpInfoLoading by mutableStateOf(false)
        private set
    var publicIpInfoError by mutableStateOf<String?>(null)
        private set

    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    private val cloudSessionStore = CloudSessionStore(app)
    private var cloudSession: CloudSession? = cloudSessionStore.load()
    // 云端链接同步守卫：只有先成功读取过云端快照，才允许回传本机快照，
    // 否则登录/首次同步时会把本机少量链接覆盖掉账号里的链接（数据丢失）。
    @Volatile
    private var cloudSnapshotReady = false
    private val deviceTrafficContributionStore = DeviceTrafficContributionStore(app)
    private val backupConnectionStore = BackupConnectionStore(app)
    var backupConnection by mutableStateOf(backupConnectionStore.load())
        private set
    var backupStatus by mutableStateOf("")
        private set
    var backupBusy by mutableStateOf(false)
        private set
    var remoteBackups by mutableStateOf(emptyList<RemoteBackup>())
        private set
    var backupPage by mutableIntStateOf(0)
        private set
    val backupPageCount: Int
        get() = ((remoteBackups.size + BACKUP_PAGE_SIZE - 1) / BACKUP_PAGE_SIZE).coerceAtLeast(1)
    val backupPageItems: List<RemoteBackup>
        get() = remoteBackups.drop(backupPage * BACKUP_PAGE_SIZE).take(BACKUP_PAGE_SIZE)
    var cloudUsername by mutableStateOf(cloudSession?.username.orEmpty())
        private set
    val savedCloudUsername: String
        get() = cloudSessionStore.savedUsername().ifBlank { cloudUsername }
    val savedCloudPassword: String
        get() = cloudSessionStore.savedPassword()
    var cloudStatus by mutableStateOf(if (cloudSession != null) "已登录" else "未登录")
        private set
    var dailyTrafficBytes by mutableLongStateOf(0L)
        private set
    var dailyTaskCount by mutableIntStateOf(0)
        private set
    var personalStats by mutableStateOf<CloudTrafficStats?>(null)
        private set
    var leaderboardEntries by mutableStateOf(emptyList<LeaderboardEntry>())
        private set
    var statsLoading by mutableStateOf(false)
        private set
    var manualSyncCooldownSeconds by mutableIntStateOf(0)
        private set
    val canManuallySyncCloudStats: Boolean
        get() = !statsLoading && manualSyncCooldownSeconds == 0

    var currentTaskName by mutableStateOf("-")
        private set

    var currentTaskConsumed by mutableLongStateOf(0L)
        private set

    var currentTaskTarget by mutableLongStateOf(0L)
        private set

    var currentSpeedBytesPerSec by mutableLongStateOf(0L)
        private set

    var totalConsumedBytes by mutableLongStateOf(0L)
        private set

    var totalTargetBytes by mutableLongStateOf(0L)
        private set

    private var nextId = 1L
    private var runJob: Job? = null
    private var autoCloudSyncJob: Job? = null
    private var manualSyncCooldownJob: Job? = null
    private var publicBackupWriteJob: Job? = null
    private var selectedCloudStatsPeriod = "day"
    private var activeRunTargets: Map<Long, Long> = emptyMap()
    private val cronetConsecutiveFailures = AtomicLong(0L)
    private var cronetTemporarilyDisabled = false
    private val activeRangeChunkSizeBytes = AtomicLong(DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES)
    private val clientDispatcher = Dispatcher().apply {
        maxRequests = 128
        maxRequestsPerHost = 128
    }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dispatcher(clientDispatcher)
        .connectionPool(ConnectionPool(128, 5, TimeUnit.MINUTES))
        .build()
    private var cronetEngine: CronetEngine? = null
    private var cronetReadyAttempted = false

    // 下载专用 OkHttpClient 单例（懒初始化）。原来每个 worker 新建一个 client + ConnectionPool(1,1,1s)，
    // 64 worker = 64 个独立池 + 每个 Range 请求重新握手；共享单例 + 大池才能打满 CDN 带宽。
    @Volatile
    private var downloadClientInstance: OkHttpClient? = null

    // Process-lifetime scope for the download run: survives Activity destruction and
    // ViewModel onCleared(). Previously the run lived in viewModelScope, so the OS reclaiming
    // the backgrounded Activity silently killed overnight downloads.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadLinks()
        loadTheme()
        loadSettings()
        loadDailyTraffic()
        nextId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
        TrafficNotificationCommandBus.register(this)
        if (keepAliveEnabled) syncKeepAliveService()
        restoreLocalJsonBackupIfNeeded()
        refreshPublicIpInfo()
        // 已登录态：启动时后台同步一次账号链接（登录后自动同步的补充）
        syncCloudLinksOnStartup()
        // P0-8h 修复③：进程被系统杀后重启，自动续跑未完成的批次（断点续传）
        maybeAutoResumeInterruptedRun()
    }

    // P0-8h 修复③：进程死亡后重启的自动续跑。条件：上次运行异常中断（标记未清）+
    // 标记年龄 < 24h + 后台常驻开启 + 有可续跑的任务。正常结束/用户停止会清标记，
    // 不会循环重启；坏链接最多被跳过后批次自然结束（配合修复①）。
    private fun maybeAutoResumeInterruptedRun() {
        if (autoResumeChecked) return
        autoResumeChecked = true
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
        if (!prefs.getBoolean(PREF_AUTO_RESUME, false)) return
        val startedAt = prefs.getLong(PREF_AUTO_RESUME_AT, 0L)
        val age = System.currentTimeMillis() - startedAt
        if (startedAt <= 0L || age < 0L || age > AUTO_RESUME_MAX_AGE_MS) {
            prefs.edit().remove(PREF_AUTO_RESUME).remove(PREF_AUTO_RESUME_AT).apply()
            return
        }
        if (!keepAliveEnabled) return
        if (isRunning) return
        if (processRunActive) {
            // 进程活着、下载循环还在跑——只是 VM 被重建了，无需也不允许再起一个
            Log.i(TAG, "run still active in process, skip auto-resume")
            return
        }
        // 有未完成的已勾选链接才值得续跑
        if (!hasEnabledValidLinks()) {
            prefs.edit().remove(PREF_AUTO_RESUME).remove(PREF_AUTO_RESUME_AT).apply()
            return
        }
        Log.i(TAG, "auto-resuming interrupted run (age=${age / 1000}s)")
        statusText = "检测到上次运行被系统中断，自动续跑未完成任务"
        // 一次性消费标记：start() 里若真跑起来会重新 set；若任务其实已完成（parsed 空）
        // 则不会留下僵尸标记导致每次启动空跑
        prefs.edit().remove(PREF_AUTO_RESUME).remove(PREF_AUTO_RESUME_AT).apply()
        // 自动续跑不能清零断点，否则"续跑"变成"从头开始"
        start(resetCompleted = false)
    }

    private fun markAutoResumeActive() {
        getApplication<Application>().getSharedPreferences(PREFS_NAME, 0).edit()
            .putBoolean(PREF_AUTO_RESUME, true)
            .putLong(PREF_AUTO_RESUME_AT, System.currentTimeMillis())
            .apply()
    }

    private fun clearAutoResume() {
        getApplication<Application>().getSharedPreferences(PREFS_NAME, 0).edit()
            .remove(PREF_AUTO_RESUME)
            .remove(PREF_AUTO_RESUME_AT)
            .apply()
    }

    fun refreshPublicIpInfo() {
        if (publicIpInfoLoading) return
        publicIpInfoLoading = true
        publicIpInfoError = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://hackmyip.com/api/ip")
                        .get()
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("HTTP ${response.code}")
                        val data = JSONObject(response.body?.string().orEmpty()).optJSONObject("data")
                            ?: error("响应数据为空")
                        val location = data.optJSONObject("location")
                        val network = data.optJSONObject("network")
                        PublicIpInfo(
                            ip = data.optString("ip").ifBlank { "-" },
                            location = listOfNotNull(
                                location?.optString("city")?.takeIf { it.isNotBlank() },
                                location?.optString("region")?.takeIf { it.isNotBlank() },
                                location?.optString("country_name")?.takeIf { it.isNotBlank() },
                            ).joinToString(", ").ifBlank { "未知" },
                            asn = network?.optString("asn")?.takeIf { it.isNotBlank() }?.let { "AS$it" } ?: "未知",
                            isp = network?.optString("isp")?.ifBlank { "未知" } ?: "未知",
                        )
                    }
                }
            }.onSuccess {
                publicIpInfo = it
            }.onFailure {
                publicIpInfoError = it.message ?: "IP 信息获取失败"
            }
            publicIpInfoLoading = false
        }
    }

    fun updateThreadCount(value: Int) {
        threadCount = value.coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
        // 运行中也要同步「活跃线程」显示，否则界面上的数字会一直是启动时的旧值。
        // 实际并发由 worker 循环每轮重算 desiredThreads 决定，这里只是让 UI 如实反映用户设定。
        activeThreadCount = threadCount
        persistSettings()
    }

    fun checkForUpdates() {
        if (updateState is UpdateState.Checking) return

        updateState = UpdateState.Checking
        viewModelScope.launch {
            updateState = withContext(Dispatchers.IO) { fetchLatestRelease() }
        }
    }

    fun requestCloudRegistrationCode(apiBaseUrl: String, username: String, email: String, password: String) {
        cloudStatus = "正在发送注册验证码..."
        viewModelScope.launch {
            runCatching { CloudApi.requestRegistrationCode(apiBaseUrl, username, email, password) }
                .onSuccess { cloudStatus = it.ifBlank { "验证码已发送到邮箱" } }
                .onFailure { cloudStatus = it.message ?: "验证码发送失败" }
        }
    }

    fun registerCloudAccount(apiBaseUrl: String, username: String, email: String, password: String, code: String) = authenticateCloudAccount {
        CloudApi.register(apiBaseUrl, username, email, password, code)
    }

    fun loginCloudAccount(apiBaseUrl: String, username: String, password: String, rememberPassword: Boolean) = authenticateCloudAccount {
        cloudSessionStore.saveLoginCredentials(username, password, rememberPassword)
        CloudApi.login(apiBaseUrl, username, password)
    }

    fun requestCloudPasswordResetCode(apiBaseUrl: String, email: String) {
        cloudStatus = "正在发送找回验证码..."
        viewModelScope.launch {
            runCatching { CloudApi.requestPasswordResetCode(apiBaseUrl, email) }
                .onSuccess { cloudStatus = it.ifBlank { "若该邮箱已注册，验证码将发送到邮箱" } }
                .onFailure { cloudStatus = it.message ?: "验证码发送失败" }
        }
    }

    fun resetCloudPassword(apiBaseUrl: String, email: String, code: String, newPassword: String) {
        cloudStatus = "正在重置密码..."
        viewModelScope.launch {
            runCatching { CloudApi.confirmPasswordReset(apiBaseUrl, email, code, newPassword) }
                .onSuccess { cloudStatus = it.ifBlank { "密码已重置，请使用新密码登录" } }
                .onFailure { cloudStatus = it.message ?: "密码重置失败" }
        }
    }

    fun logoutCloudAccount() {
        cloudSessionStore.clear()
        cloudSession = null
        cloudSnapshotReady = false
        stopAutoCloudSync()
        cloudUsername = ""
        cloudStatus = "未登录"
        personalStats = null
        leaderboardEntries = emptyList()
    }

    fun saveBackupConnection(connection: BackupConnection) {
        backupConnection = connection
        backupConnectionStore.save(connection)
    }

    fun uploadBackup(connection: BackupConnection) {
        saveBackupConnection(connection)
        backupBusy = true
        backupStatus = "正在上传备份..."
        viewModelScope.launch {
            runCatching {
                val document = createBackupDocument()
                when (connection.provider) {
                    "s3" -> S3Backup.upload(connection, document)
                    else -> WebDavBackup.upload(connection.webDavUrl, connection.webDavUsername, connection.webDavPassword, document)
                }
            }.onSuccess {
                backupStatus = "新备份上传成功"
                backupBusy = false
                refreshBackupList(connection)
            }
                .onFailure {
                    backupStatus = it.message ?: "备份上传失败"
                    backupBusy = false
                }
        }
    }

    fun refreshBackupList(connection: BackupConnection) {
        saveBackupConnection(connection)
        backupBusy = true
        backupStatus = "正在读取在线备份..."
        viewModelScope.launch {
            runCatching {
                when (connection.provider) {
                    "s3" -> S3Backup.list(connection)
                    else -> WebDavBackup.list(connection.webDavUrl, connection.webDavUsername, connection.webDavPassword)
                }
            }.onSuccess {
                remoteBackups = it
                backupPage = 0
                backupStatus = if (it.isEmpty()) "目录中没有备份" else "已读取 ${it.size} 个在线备份"
            }.onFailure { backupStatus = it.message ?: "在线备份读取失败" }
            backupBusy = false
        }
    }

    fun previousBackupPage() {
        backupPage = (backupPage - 1).coerceAtLeast(0)
    }

    fun nextBackupPage() {
        backupPage = (backupPage + 1).coerceAtMost(backupPageCount - 1)
    }

    fun clearBackupList() {
        remoteBackups = emptyList()
        backupPage = 0
        backupStatus = ""
    }

    fun downloadBackup(connection: BackupConnection, backup: RemoteBackup) {
        saveBackupConnection(connection)
        backupBusy = true
        backupStatus = "正在恢复 ${backup.fileName}..."
        viewModelScope.launch {
            runCatching {
                when (connection.provider) {
                    "s3" -> S3Backup.download(connection, backup.remoteId)
                    else -> WebDavBackup.download(backup.remoteId, connection.webDavUsername, connection.webDavPassword)
                }
            }.onSuccess {
                restoreBackupDocument(it)
                backupStatus = "备份恢复成功"
            }.onFailure { backupStatus = it.message ?: "备份下载失败" }
            backupBusy = false
        }
    }

    fun importLocalJsonBackup(uri: Uri) {
        if (isRunning) return
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("无法读取 JSON 文件")
                }
                restoreBackupDocument(JSONObject(raw))
            }.onSuccess {
                statusText = "已从本地 JSON 恢复链接"
            }.onFailure {
                statusText = it.message ?: "本地 JSON 恢复失败"
            }
        }
    }

    private fun restoreLocalJsonBackupIfNeeded() {
        if (links.isNotEmpty()) {
            schedulePublicJsonBackup()
            return
        }
        viewModelScope.launch {
            val document = withContext(Dispatchers.IO) {
                PublicJsonBackup.read(getApplication())
            }
            if (document == null) {
                schedulePublicJsonBackup()
                return@launch
            }
            runCatching {
                restoreBackupDocument(document)
            }.onSuccess {
                statusText = "已从本地 JSON 自动恢复链接"
            }.onFailure {
                statusText = it.message ?: "本地 JSON 自动恢复失败"
                schedulePublicJsonBackup()
            }
        }
    }

    private fun createBackupDocument(): JSONObject {
        val settings = JSONObject().apply {
            put("threadCount", threadCount)
            put("userAgent", userAgentText)
            put("userAgents", JSONArray(serializeUserAgentOptions(userAgentOptions).toString()))
            put("keepAlive", keepAliveEnabled)
            put("keepScreenAwake", keepScreenAwakeEnabled)
            put("progressNotification", progressNotificationEnabled)
            put("themeDark", isDarkTheme)
        }
        val linkArray = serializeLinksForBackup(links)
        return JSONObject().apply {
            put("schemaVersion", 1)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("createdAt", Instant.now().toString())
            put("links", linkArray)
            put("linkCount", linkArray.length())
            put("settings", settings)
            put("dailyTraffic", JSONObject().apply {
                val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
                put("date", prefs.getString(PREF_DAILY_TRAFFIC_DATE, LocalDate.now().toString()))
                put("bytes", dailyTrafficBytes)
                put("taskCount", dailyTaskCount)
            })
        }
    }

    private fun restoreBackupDocument(document: JSONObject) {
        require(document.optInt("schemaVersion", 0) == 1) { "不支持的备份版本" }
        val restoredLinks = document.optJSONArray("links") ?: throw IllegalStateException("备份缺少链接配置")
        val restored = buildList {
            for (index in 0 until restoredLinks.length()) {
                val item = restoredLinks.optJSONObject(index) ?: continue
                add(LinkItem(
                    id = index.toLong() + 1L,
                    name = item.optString("name"),
                    url = item.optString("url").trim(),
                    targetGbText = item.optString("targetGbText"),
                    userAgent = item.optString("userAgent"),
                    enabled = item.optBoolean("enabled", true),
                ))
            }
        }
        links = restored
        nextId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
        val settings = document.optJSONObject("settings")
        if (settings != null) {
            threadCount = settings.optInt("threadCount", threadCount).coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
            activeThreadCount = threadCount
            userAgentText = settings.optString("userAgent", userAgentText).take(500)
            userAgentOptions = loadUserAgentOptions(settings.optJSONArray("userAgents")?.toString())
            keepAliveEnabled = settings.optBoolean("keepAlive", keepAliveEnabled)
            keepScreenAwakeEnabled = settings.optBoolean("keepScreenAwake", keepScreenAwakeEnabled)
            progressNotificationEnabled = settings.optBoolean("progressNotification", progressNotificationEnabled)
            isDarkTheme = settings.optBoolean("themeDark", isDarkTheme)
            persistSettings()
            getApplication<Application>().getSharedPreferences(PREFS_NAME, 0).edit()
                .putBoolean(PREF_THEME_DARK, isDarkTheme)
                .apply()
        }
        val traffic = document.optJSONObject("dailyTraffic")
        if (traffic != null) {
            val today = LocalDate.now().toString()
            val backupDate = traffic.optString("date", today)
            persistDailyTraffic(
                today,
                if (backupDate == today) traffic.optLong("bytes", 0L).coerceAtLeast(0L) else 0L,
                if (backupDate == today) traffic.optInt("taskCount", 0).coerceAtLeast(0) else 0,
            )
        }
        persistLinks()
        syncKeepAliveService()
    }

    fun refreshCloudStats(period: String, isManual: Boolean = true) {
        val session = cloudSession ?: run {
            cloudStatus = "请先登录云端账号"
            return
        }
        if (statsLoading) {
            if (isManual) cloudStatus = "正在同步，请稍候"
            return
        }
        if (isManual && manualSyncCooldownSeconds > 0) {
            cloudStatus = "请在 ${manualSyncCooldownSeconds} 秒后再次同步"
            return
        }
        if (isManual) {
            selectedCloudStatsPeriod = period
            startManualSyncCooldown()
        }
        statsLoading = true
        viewModelScope.launch {
            // 首次同步前先读一次云端快照（保证链接先落地，再允许回传，避免覆盖账号数据）
            if (!cloudSnapshotReady) {
                runCatching { CloudApi.downloadProfileSnapshot(session) }.getOrNull()?.let { document ->
                    runCatching {
                        if (links.isEmpty()) restoreBackupDocument(document) else mergeLinksFromCloud(document)
                    }
                    cloudSnapshotReady = true
                }
            }
            runCatching {
                val today = LocalDate.now().toString()
                val cloudDayBeforeSync = CloudApi.personalStats(session, "day")
                val contribution = deviceTrafficContributionStore.contributionForSync(
                    session = session,
                    date = today,
                    localBytes = dailyTrafficBytes,
                    localTasks = dailyTaskCount,
                    cloudBytes = cloudDayBeforeSync.consumedBytes,
                    cloudTasks = cloudDayBeforeSync.taskCount,
                )
                CloudApi.syncTraffic(
                    session = session,
                    consumedBytes = contribution.consumedBytes,
                    taskCount = contribution.taskCount,
                    installationId = deviceTrafficContributionStore.installationId(),
                )
                val cloudDayAfterSync = CloudApi.personalStats(session, "day")
                val selectedStats = if (period == "day") cloudDayAfterSync else CloudApi.personalStats(session, period)
                restoreDailyTrafficFromCloud(cloudDayAfterSync)
                val snapshotError = runCatching {
                    // 守卫：必须先成功读过云端快照才允许回传，否则会把本机链接覆盖账号数据
                    if (!cloudSnapshotReady) {
                        throw IllegalStateException("尚未读取云端快照")
                    }
                    CloudApi.uploadProfileSnapshot(session, createBackupDocument())
                }.exceptionOrNull()
                CloudSyncResult(
                    selectedStats = selectedStats,
                    entries = CloudApi.leaderboard(session, period),
                    snapshotError = snapshotError,
                )
            }.onSuccess { result ->
                personalStats = result.selectedStats
                leaderboardEntries = result.entries
                cloudStatus = if (result.snapshotError == null) {
                    "已同步${result.selectedStats.period}统计"
                } else {
                    "已同步${result.selectedStats.period}统计，链接备份稍后重试"
                }
            }.onFailure { cloudStatus = it.message ?: "云端统计读取失败" }
            statsLoading = false
        }
    }

    private fun startManualSyncCooldown() {
        manualSyncCooldownJob?.cancel()
        manualSyncCooldownSeconds = MANUAL_CLOUD_SYNC_COOLDOWN_SECONDS
        manualSyncCooldownJob = viewModelScope.launch {
            while (manualSyncCooldownSeconds > 0) {
                delay(1_000)
                manualSyncCooldownSeconds -= 1
            }
        }
    }

    private fun startAutoCloudSync() {
        autoCloudSyncJob?.cancel()
        if (!isRunning || cloudSession == null) return
        autoCloudSyncJob = viewModelScope.launch {
            while (isActive && isRunning && cloudSession != null) {
                delay(AUTO_CLOUD_SYNC_INTERVAL_MS)
                if (isActive && isRunning && cloudSession != null) {
                    refreshCloudStats(selectedCloudStatsPeriod, isManual = false)
                }
            }
        }
    }

    private fun stopAutoCloudSync() {
        autoCloudSyncJob?.cancel()
        autoCloudSyncJob = null
    }

    private fun restoreDailyTrafficFromCloud(stats: CloudTrafficStats) {
        val (restoredBytes, restoredTasks) = mergeDailyTrafficWithCloud(
            dailyTrafficBytes,
            dailyTaskCount,
            stats.consumedBytes,
            stats.taskCount,
        )
        if (restoredBytes != dailyTrafficBytes || restoredTasks != dailyTaskCount) {
            persistDailyTraffic(LocalDate.now().toString(), restoredBytes, restoredTasks)
        }
    }

    private fun authenticateCloudAccount(action: suspend () -> CloudSession) {
        viewModelScope.launch {
            cloudStatus = "正在校验账号..."
            try {
                val session = action()
                cloudSessionStore.save(session)
                cloudSession = session
                cloudUsername = session.username
                // 登录后无条件同步账号里的链接：
                // - 本机无链接 → 全量恢复云端快照（含设置）
                // - 本机已有链接 → 按 URL 合并（云端链接优先，本地独有链接保留，不丢数据）
                var syncedCount = 0
                var syncError: Throwable? = null
                runCatching { CloudApi.downloadProfileSnapshot(session) }
                    .onSuccess { document ->
                        if (document != null) {
                            syncedCount = if (links.isEmpty()) {
                                runCatching { restoreBackupDocument(document) }
                                    .also { if (it.isFailure) syncError = it.exceptionOrNull() }
                                    .map { links.size }
                                    .getOrDefault(0)
                            } else {
                                runCatching { mergeLinksFromCloud(document) }
                                    .also { if (it.isFailure) syncError = it.exceptionOrNull() }
                                    .getOrDefault(0)
                            }
                            cloudSnapshotReady = syncError == null
                        }
                    }
                    .onFailure { syncError = it }
                cloudStatus = when {
                    syncError != null -> "登录成功，链接同步失败：${syncError?.message ?: "网络异常"}"
                    syncedCount > 0 -> "登录成功 · 已同步 $syncedCount 条链接"
                    else -> "登录成功"
                }
                refreshCloudStats("day", isManual = false)
                startAutoCloudSync()
            } catch (error: Throwable) {
                cloudStatus = error.message ?: "登录失败"
            }
        }
    }

    /**
     * 把云端快照里的链接合并进本机（登录/启动同步用）：
     * - 云端顺序优先；URL 相同的沿用本地 id 与已消耗字节，元数据以云端为准
     * - 云端没有的本地链接原样保留（避免用户本地新增内容被抹掉）
     * 返回合并后计入的云端链接数。
     */
    private fun mergeLinksFromCloud(document: JSONObject): Int {
        val arr = document.optJSONArray("links") ?: return 0
        val cloudLinks = buildList {
            for (index in 0 until arr.length()) {
                val item = arr.optJSONObject(index) ?: continue
                val url = item.optString("url").trim()
                if (url.isEmpty()) continue
                add(
                    LinkItem(
                        id = 0L,
                        name = item.optString("name"),
                        url = url,
                        targetGbText = item.optString("targetGbText"),
                        userAgent = item.optString("userAgent"),
                        enabled = item.optBoolean("enabled", true),
                    ),
                )
            }
        }
        if (cloudLinks.isEmpty()) return 0
        val localByUrl = links.associateBy { it.url.trim() }
        var nextLocalId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
        val merged = mutableListOf<LinkItem>()
        cloudLinks.forEach { cloud ->
            val local = localByUrl[cloud.url.trim()]
            merged += if (local != null) {
                local.copy(
                    name = cloud.name.ifBlank { local.name },
                    targetGbText = cloud.targetGbText.ifBlank { local.targetGbText },
                    userAgent = cloud.userAgent,
                    enabled = cloud.enabled,
                )
            } else {
                cloud.copy(id = nextLocalId++)
            }
        }
        val cloudUrls = cloudLinks.map { it.url.trim() }.toSet()
        links.filter { it.url.trim() !in cloudUrls }.forEach { merged += it }
        links = merged
        nextId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
        persistLinks()
        return cloudLinks.size
    }

    /** 启动时（已有登录态）后台同步一次账号链接，失败静默 */
    private fun syncCloudLinksOnStartup() {
        val session = cloudSession ?: return
        viewModelScope.launch {
            runCatching { CloudApi.downloadProfileSnapshot(session) }
                .onSuccess { document ->
                    if (document != null) {
                        runCatching {
                            if (links.isEmpty()) restoreBackupDocument(document) else mergeLinksFromCloud(document)
                        }
                        cloudSnapshotReady = true
                    }
                }
        }
    }

    private fun fetchLatestRelease(): UpdateState {
        return try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Network-Panel-X/${BuildConfig.VERSION_NAME}")
                .cacheControl(CacheControl.Builder().noCache().build())
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return UpdateState.Error("更新检查失败：服务器返回 ${response.code}")
                }

                val json = JSONObject(response.body?.string().orEmpty())
                val tagName = json.optString("tag_name").trim()
                val latestVersion = tagName.removePrefix("v").trim()
                val releasePageUrl = json.optString("html_url").trim()
                if (latestVersion.isEmpty() || releasePageUrl.isEmpty()) {
                    return UpdateState.Error("更新信息不完整，请稍后重试")
                }

                if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) <= 0) {
                    return UpdateState.UpToDate(latestVersion)
                }

                val assets = json.optJSONArray("assets") ?: JSONArray()
                val apkUrl = (0 until assets.length())
                    .asSequence()
                    .mapNotNull { assets.optJSONObject(it) }
                    .map { it.optString("browser_download_url").trim() }
                    .firstOrNull { it.endsWith(".apk", ignoreCase = true) }
                    ?.ifBlank { null }
                    ?.let { "$UPDATE_DOWNLOAD_PROXY_PREFIX$it" }

                UpdateState.Available(
                    ReleaseInfo(
                        version = latestVersion,
                        notes = json.optString("body").trim(),
                        apkDownloadUrl = apkUrl,
                        releasePageUrl = releasePageUrl,
                    ),
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Log.w(TAG, "Failed to check for updates", error)
            UpdateState.Error("无法连接更新服务，请检查网络后重试")
        }
    }

    fun updateUserAgent(value: String) {
        userAgentText = value.take(500)
        persistSettings()
    }

    fun selectGlobalUserAgent(value: String) {
        userAgentText = value.take(500)
        persistSettings()
    }

    fun saveCurrentUserAgentToList(name: String) {
        val value = userAgentText.trim()
        val normalizedName = name.trim().take(40)
        if (value.isEmpty() || normalizedName.isEmpty()) return
        val existing = userAgentOptions.firstOrNull { it.name == normalizedName }
        if (existing?.builtIn == true) return
        userAgentOptions = userAgentOptions
            .filterNot { it.name == normalizedName || (!it.builtIn && it.value == value) } +
            SavedUserAgent(name = normalizedName, value = value)
        persistSettings()
    }

    fun removeCurrentUserAgentFromList() {
        val value = userAgentText.trim()
        if (value.isEmpty()) return
        userAgentOptions = userAgentOptions.filterNot { !it.builtIn && it.value == value }
        persistSettings()
    }

    fun canRemoveCurrentUserAgent(): Boolean = userAgentOptions.any {
        !it.builtIn && it.value == userAgentText.trim()
    }

    fun clearGlobalUserAgent() {
        userAgentText = ""
        persistSettings()
    }

    fun updateKeepAliveEnabled(enabled: Boolean) {
        keepAliveEnabled = enabled
        persistSettings()
        syncKeepAliveService()
    }

    fun updateKeepScreenAwakeEnabled(enabled: Boolean) {
        keepScreenAwakeEnabled = enabled
        persistSettings()
        syncKeepAliveService()
    }

    fun updateProgressNotificationEnabled(enabled: Boolean) {
        progressNotificationEnabled = enabled
        persistSettings()
        updateBackgroundNotification()
    }

    /**
     * 切换流量单位制。同步更新全局 [trafficUnitMode]，
     * 让首页/链接管理/统计里所有 formatBytes 调用立即按新单位渲染。
     */
    fun updateTrafficUnit(unit: String) {
        val normalized = if (unit == TRAFFIC_UNIT_GB) TRAFFIC_UNIT_GB else TRAFFIC_UNIT_GIB
        trafficUnit = normalized
        trafficUnitMode = normalized
        getApplication<Application>().getSharedPreferences(PREFS_NAME, 0).edit()
            .putString(PREF_TRAFFIC_UNIT, normalized)
            .apply()
        persistSettings()
    }

    fun refreshBackgroundSystemStatus() {
        val app = getApplication<Application>()
        notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putBoolean(PREF_THEME_DARK, isDarkTheme)
            .apply()
    }

    fun addLink() {
        if (isRunning) return
        links = links + LinkItem(id = nextId++)
        persistLinks()
    }

    fun removeLink(id: Long) {
        if (isRunning) return
        links = links.filterNot { it.id == id }
        persistLinks()
    }

    fun updateLinkName(id: Long, value: String) {
        if (isRunning) return
        links = links.map { if (it.id == id) it.copy(name = value) else it }
        persistLinks()
    }

    fun updateLinkUrl(id: Long, value: String) {
        if (isRunning) return
        links = links.map { if (it.id == id) it.copy(url = value) else it }
        persistLinks()
    }

    fun updateLinkTargetGb(id: Long, value: String) {
        if (isRunning) return
        links = links.map { if (it.id == id) it.copy(targetGbText = value) else it }
        persistLinks()
    }

    fun updateLinkUserAgent(id: Long, value: String) {
        if (isRunning) return
        links = links.map { if (it.id == id) it.copy(userAgent = value) else it }
        persistLinks()
    }

    fun toggleLinkEnabled(id: Long, enabled: Boolean) {
        if (isRunning) return
        links = links.map { if (it.id == id) it.copy(enabled = enabled) else it }
        persistLinks()
    }

    fun moveLinkUp(id: Long) {
        if (isRunning) return
        val index = links.indexOfFirst { it.id == id }
        if (index <= 0) return
        val mutable = links.toMutableList()
        val temp = mutable[index - 1]
        mutable[index - 1] = mutable[index]
        mutable[index] = temp
        links = mutable
        persistLinks()
    }

    fun moveLinkDown(id: Long) {
        if (isRunning) return
        val index = links.indexOfFirst { it.id == id }
        if (index == -1 || index >= links.lastIndex) return
        val mutable = links.toMutableList()
        val temp = mutable[index + 1]
        mutable[index + 1] = mutable[index]
        mutable[index] = temp
        links = mutable
        persistLinks()
    }

    fun stop() {
        runJob?.cancel()
        runJob = null
        isRunning = false
        stopAutoCloudSync()
        statusText = "已暂停"
        currentSpeedBytesPerSec = 0L
        cronetConsecutiveFailures.set(0L)
        cronetTemporarilyDisabled = false
        // P0-8h 修复③：用户主动暂停，不自动恢复（尊重用户意图）
        clearAutoResume()
        syncKeepAliveService()
        links = links.map {
            if (it.status == "运行中") it.copy(status = "已停止") else it
        }
    }

    fun quickStartSelectedTraffic(): Boolean {
        if (isRunning) return false

        val selectedCount = links.count { it.enabled }
        if (selectedCount <= 0) {
            statusText = "请先在链接管理页勾选要测流的链接"
            return false
        }

        val quickTargetBytes = (QUICK_TRAFFIC_TARGET_GB_TEXT.toDouble() * bytesPerGigabyte()).toLong()
        val parsed = mutableListOf<ParsedTask>()
        links = links.map { item ->
            if (!item.enabled) {
                item
            } else {
                val url = item.url.trim()
                val name = item.name.trim().ifEmpty { "未命名链接" }
                if (url.isNotEmpty()) {
                    parsed += ParsedTask(
                        id = item.id,
                        name = name,
                        url = url,
                        targetBytes = quickTargetBytes,
                        initialConsumedBytes = 0L,
                        userAgent = resolveTaskUserAgent(item),
                    )
                    item.copy(consumedBytes = 0L, status = "未开始")
                } else {
                    item.copy(consumedBytes = 0L, status = "请填写URL")
                }
            }
        }

        if (parsed.isEmpty()) {
            statusText = "请先为已勾选链接填写下载链接 URL"
            return false
        }

        startParsedTasks(
            parsed = parsed,
            initialStatusText = "一键测流开始，共 ${parsed.size} 个链接，每条 ${QUICK_TRAFFIC_TARGET_GB_TEXT}${gigabyteSuffix()}，线程数 $threadCount",
            markResumable = false,
        )
        return true
    }

    fun start(resetCompleted: Boolean = true) {
        if (isRunning) return

        // 用户点「开始」是明确的重新开始意图：把已完成（consumedBytes >= 目标）的链接
        // 的计数归零，否则这些链接会被 parseTasks 跳过，parsed 为空时界面只显示
        // 「所选链接已完成」，用户看到的就是「点击开始没反应」。
        // 只重置"已达标"的链接，未完成的保留断点继续跑。
        // resetCompleted=false 用于自动续跑场景（不能把断点清零）。
        if (resetCompleted) {
            val resetDone = links.map { item ->
                val gb = item.targetGbText.trim().toDoubleOrNull()
                if (!item.enabled || gb == null || gb <= 0.0) {
                    item
                } else {
                    val target = (gb * bytesPerGigabyte()).toLong()
                    if (target > 0L && item.consumedBytes >= target) {
                        item.copy(consumedBytes = 0L, status = "未开始")
                    } else {
                        item
                    }
                }
            }
            if (resetDone != links) {
                links = resetDone
                persistLinks()
            }
        }

        val parsed = parseTasks()
        if (parsed.isEmpty()) {
            statusText = if (hasEnabledValidLinks()) {
                "所选链接已完成，可修改目标后重跑"
            } else {
                "请在管理页勾选并填写有效链接（URL + 目标${gigabyteSuffix()}）"
            }
            return
        }

        startParsedTasks(
            parsed = parsed,
            initialStatusText = "开始/继续执行，共 ${parsed.size} 个链接，线程数 $threadCount",
        )
    }

    private fun startParsedTasks(parsed: List<ParsedTask>, initialStatusText: String, markResumable: Boolean = true) {
        activeRunTargets = parsed.associate { it.id to it.targetBytes }
        cronetConsecutiveFailures.set(0L)
        cronetTemporarilyDisabled = false
        totalTargetBytes = if (parsed.any { isUnlimitedTarget(it.targetBytes) }) 0L else parsed.sumOf { it.targetBytes }
        totalConsumedBytes = parsed.sumOf { if (isUnlimitedTarget(it.targetBytes)) it.initialConsumedBytes else minOf(it.initialConsumedBytes, it.targetBytes) }
        activeThreadCount = threadCount
        activeRangeChunkSizeBytes.set(DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES)
        currentTaskName = "-"
        currentTaskConsumed = 0L
        currentTaskTarget = 0L
        currentSpeedBytesPerSec = 0L

        links = links.map {
            if (it.id in activeRunTargets) {
                val target = activeRunTargets[it.id] ?: 0L
                val status = if (!isUnlimitedTarget(target) && it.consumedBytes >= target) "完成" else "未开始"
                it.copy(status = status)
            } else {
                it
            }
        }
        isRunning = true
        addDailyTaskCount(parsed.size)
        statusText = initialStatusText
        syncKeepAliveService()
        startAutoCloudSync()
        // P0-8h 修复③：批次进行中持久化标记，进程被杀后重启可自动续跑
        // （一键测流传 markResumable=false：0.1GB 快测被杀后不应自动重跑消耗流量）
        if (markResumable) markAutoResumeActive()

        runJob = appScope.launch(Dispatchers.IO) {
            processRunActive = true
            var failedCount = 0
            for ((index, task) in parsed.withIndex()) {
                if (!isActive) break

                withContext(Dispatchers.Main) {
                    currentTaskName = task.name
                    currentTaskTarget = task.targetBytes
                    currentTaskConsumed = task.initialConsumedBytes
                    links = links.map {
                        when (it.id) {
                            task.id -> it.copy(status = "运行中")
                            else -> if (it.status == "运行中") it.copy(status = "未开始") else it
                        }
                    }
                    statusText = "执行第 ${index + 1}/${parsed.size} 个链接"
                }

                try {
                    consumeTask(task)
                    // 修复「没跑够却记成完成」：
                    // 原来无条件写 consumedBytes = task.targetBytes，
                    // 导致中途断网/失败时实际只下了 934MB，界面却显示 3.00GB 完成。
                    // 现在按真实计数收尾：达到目标才算完成，没达到就是「失败」。
                    val actualConsumed = runCatching {
                        links.firstOrNull { it.id == task.id }?.consumedBytes
                    }.getOrNull() ?: 0L
                    val reached = isUnlimitedTarget(task.targetBytes) ||
                        actualConsumed >= task.targetBytes
                    withContext(Dispatchers.Main) {
                        links = links.map {
                            if (it.id == task.id) {
                                it.copy(
                                    status = if (reached) "完成" else "失败",
                                    consumedBytes = if (isUnlimitedTarget(task.targetBytes)) actualConsumed else minOf(actualConsumed, task.targetBytes),
                                )
                            } else it
                        }
                        if (!reached) {
                            failedCount++
                            statusText = "第 ${index + 1} 个链接未达目标（实收 ${formatBytes(actualConsumed)} / 目标 ${formatBytes(task.targetBytes)}）"
                        }
                        // 任务成功收尾清掉上一条的错误提示，避免误导
                        lastErrorText = ""
                        // P0-8h 修复③：单任务完成的断点立即落盘
                        persistLinks()
                    }
                } catch (_: CancellationException) {
                    processRunActive = false
                    return@launch
                } catch (e: Exception) {
                    // P0-8h 修复①：单条失败（含 15 秒 watchdog 网络抖动）只跳过该链接，
                    // 不再放弃整个批次——挂机过夜时一次抖动不再全军覆没
                    failedCount++
                    withContext(Dispatchers.Main) {
                        links = links.map {
                            if (it.id == task.id) it.copy(status = "失败") else it
                        }
                        statusText = "第 ${index + 1} 个链接失败（${e.message ?: "未知错误"}），跳过继续"
                        currentSpeedBytesPerSec = 0L
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isRunning = false
                stopAutoCloudSync()
                currentSpeedBytesPerSec = 0L
                syncKeepAliveService()
                // P0-8h 修复③：批次正常走完（含失败跳过），清除自动续跑标记
                clearAutoResume()
                processRunActive = false
                statusText = when {
                    failedCount > 0 && links.any { it.status == "完成" } -> "执行结束，${failedCount} 个链接失败（已跳过），其余完成"
                    failedCount > 0 -> "执行结束，全部 ${failedCount} 个链接失败"
                    links.any { it.status == "已停止" } -> "已停止"
                    else -> "全部完成"
                }
                // 批次正常结束时清掉错误提示（失败原因已并入 statusText 的计数汇总）
                if (failedCount == 0) lastErrorText = ""
            }
        }
    }

    private fun parseTasks(): List<ParsedTask> {
        val tasks = mutableListOf<ParsedTask>()
        for (item in links) {
            if (!item.enabled) continue
            val url = item.url.trim()
            val name = item.name.trim().ifEmpty { "未命名链接" }
            val gb = item.targetGbText.trim().toDoubleOrNull()
            if (url.isEmpty() || gb == null || gb < 0.0) continue
            val targetBytes = if (gb == 0.0) 0L else (gb * bytesPerGigabyte()).toLong()
            if (gb > 0.0 && targetBytes <= 0L) continue
            val initialConsumed = if (isUnlimitedTarget(targetBytes)) item.consumedBytes else item.consumedBytes.coerceAtMost(targetBytes)
            if (!isUnlimitedTarget(targetBytes) && initialConsumed >= targetBytes) continue
            tasks += ParsedTask(
                id = item.id,
                name = name,
                url = url,
                targetBytes = targetBytes,
                initialConsumedBytes = initialConsumed,
                userAgent = resolveTaskUserAgent(item),
            )
        }
        return tasks
    }

    private fun hasEnabledValidLinks(): Boolean {
        return links.any { item ->
            if (!item.enabled) return@any false
            val url = item.url.trim()
            val gb = item.targetGbText.trim().toDoubleOrNull()
            url.isNotEmpty() && gb != null && gb >= 0.0
        }
    }

    private suspend fun probeRangeSupport(url: String, userAgent: String?): Pair<Boolean, Long> {
        // M-f 修复：探测整体加 10 秒预算。慢 CDN / 重定向循环不再卡住任务启动 25s+，
        // 超时按不支持 Range 降级直接开跑（全量下载模式）。
        // 只捕获 TimeoutCancellationException——外部取消（用户停止任务）正常传播
        return try {
            withTimeout(PROBE_TOTAL_BUDGET_MS) {
                probeRangeSupportInner(url, userAgent)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "probeRangeSupport timed out after ${PROBE_TOTAL_BUDGET_MS}ms: $url")
            false to -1L
        }
    }

    private suspend fun probeRangeSupportInner(url: String, userAgent: String?): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val minRangeSize = MIN_ADAPTIVE_CHUNK_SIZE_BYTES
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .head()
            applyUserAgentHeader(requestBuilder, userAgent)
            val request = requestBuilder.build()
            httpClient.newCall(request).execute().use { response ->
                val length = response.header("Content-Length")?.toLongOrNull() ?: response.body?.contentLength() ?: -1L
                val acceptRanges = response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
                if (response.isSuccessful && acceptRanges && length > minRangeSize) {
                    return@withContext true to length
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Some CDN links, including cloud.139.com APK links, reject HEAD with 403
            // but still support byte ranges. Fall through to a 1-byte range probe.
        }

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0")
                .header("Accept", "*/*")
            applyUserAgentHeader(requestBuilder, userAgent)
            val request = requestBuilder.build()
            httpClient.newCall(request).execute().use { response ->
                val contentRange = response.header("Content-Range").orEmpty()
                val totalLength = contentRange.substringAfter('/', "").toLongOrNull() ?: -1L
                if (response.code == 206 && totalLength > minRangeSize) {
                    return@withContext true to totalLength
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        false to -1L
    }

    private suspend fun consumeTask(task: ParsedTask) {
        val consumed = AtomicLong(task.initialConsumedBytes)
        var lastBytes = task.initialConsumedBytes
        var lastTime = System.currentTimeMillis()
        var smoothedSpeed = 0L
        val lastProgressAt = AtomicLong(System.currentTimeMillis())
        val firstError = AtomicReference<String?>(null)
        // 失败原因上屏（节流 3s，避免每 worker 每秒刷一次重组）
        val lastErrorPushAt = AtomicLong(0L)
        suspend fun pushError(raw: String?) {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return
            val now = System.currentTimeMillis()
            if (now - lastErrorPushAt.get() < 3_000L) return
            // 修复「部分 worker 失败就报错，但整体在正常跑」：
            // 只要最近 3 秒内有数据进来，说明整体下载仍在推进，
            // 单个 worker 的瞬时超时/断连属于正常抖动，不该弹到界面上吓用户。
            // 真正卡死的情况（全部 worker 都失败）由下方 15s watchdog 判定并抛错。
            if (now - lastProgressAt.get() < 3_000L) return
            lastErrorPushAt.set(now)
            val friendly = friendlyErrorText(text)
            withContext(Dispatchers.Main) { if (lastErrorText != friendly) lastErrorText = friendly }
            Log.w(TAG, "download error: $text")
        }
        withContext(Dispatchers.Main) { if (lastErrorText.isNotEmpty()) lastErrorText = "" }
        val (supportsRange, contentLength) = probeRangeSupport(task.url, task.userAgent)
        activeRangeChunkSizeBytes.set(chooseChunkSizeForContent(contentLength, 0L))
        Log.i(TAG, "task probe: range=$supportsRange len=$contentLength adaptiveThreads=${adaptiveConcurrencyFor(contentLength, threadCount, task.targetBytes)} (requested=$threadCount target=${task.targetBytes}) url=${task.url.take(60)}")
        val nextRangeStart = AtomicLong(0L)

        coroutineScope {
            val progressJob = launch(Dispatchers.Default) {
                var tick = 0
                while (currentCoroutineContext().isActive) {
                    delay(1000)
                    val now = System.currentTimeMillis()
                    val snapshot = if (isUnlimitedTarget(task.targetBytes)) consumed.get() else consumed.get().coerceAtMost(task.targetBytes)
                    val dt = max(1L, now - lastTime)
                    val delta = (snapshot - lastBytes).coerceAtLeast(0L)
                    val instantSpeed = (delta * 1000L) / dt
                    smoothedSpeed = when {
                        smoothedSpeed <= 0L -> instantSpeed
                        instantSpeed <= 0L -> (smoothedSpeed * 7L) / 10L
                        else -> ((smoothedSpeed * 7L) + (instantSpeed * 3L)) / 10L
                    }
                    lastBytes = snapshot
                    lastTime = now

                    withContext(Dispatchers.Main) {
                        currentTaskConsumed = snapshot
                        currentSpeedBytesPerSec = smoothedSpeed
                        updateConsumed(task.id, snapshot)
                        updateBackgroundNotification()
                        // 修复「错误提示挂着不走」：数据恢复流动后自动清除旧错误。
                        // 之前 pushError 只写不清，一旦某次超时把提示点亮，
                        // 后续即使下载早已恢复，界面仍一直显示「连接超时」，
                        // 用户误以为任务卡死（实际在正常跑）。
                        if (lastErrorText.isNotEmpty() && delta > 0L) {
                            lastErrorText = ""
                        }
                    }

                    // P2 修复：每 30 秒落盘一次（原来每秒写 prefs）
                    tick += 1
                    if (tick % 30 == 0) {
                        withContext(Dispatchers.Main) { flushDailyTrafficIfDirty(false) }
                        // P0-8h 修复③：断点数据定期落盘，被杀后重启可从 consumedBytes 续跑
                        withContext(Dispatchers.Main) { persistLinks() }
                    }

                    if (hasReachedTarget(snapshot, task.targetBytes)) break
                }
            }

            repeat(MAX_EFFECTIVE_CONCURRENCY) { index ->
                launch(Dispatchers.IO) {
                    val buffer = ByteArray(READ_BUFFER_SIZE)
                    val downloadClient = buildDownloadClient()
                    delay(index * WORKER_STAGGER_MS)
                    // M-b 修复：每个 worker 的重试退避状态（成功后归零）
                    var retryBackoffMs = RETRY_BACKOFF_BASE_MS
                    while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                        val desiredThreads = computeActiveThreadCount(smoothedSpeed, contentLength, task.targetBytes)
                        if (index >= desiredThreads) {
                            delay(200)
                            continue
                        }
                        try {
                            val cronet: CronetEngine? = null
                            if (cronet != null) {
                                val statusCode = downloadWithCronet(
                                    engine = cronet,
                                    url = task.url,
                                    userAgent = task.userAgent,
                                    targetCounter = consumed,
                                    targetBytes = task.targetBytes,
                                    onBytesRead = { bytesRead ->
                                        addConsumedSafely(consumed, task.targetBytes, bytesRead)
                                        lastProgressAt.set(System.currentTimeMillis())
                                    },
                                )
                                cronetConsecutiveFailures.set(0L)
                                if (statusCode !in 200..299) {
                                    val message = "HTTP $statusCode"
                                    firstError.compareAndSet(null, message)
                                    if (statusCode in 400..499) {
                                        throw FatalRequestException("请求失败：$message")
                                }
                                delay(retryBackoffMs)
                                retryBackoffMs = (retryBackoffMs * 2).coerceAtMost(RETRY_BACKOFF_MAX_MS)
                                continue
                            }
                        } else {
                                // 小文件不分片：走整文件 GET，让每个 worker 独立下一份完整文件。
                                // 分片在 0.6MB 文件上会让 TTFB(50ms) 压过传输(30ms)，反而更慢。
                                val isSmallFile = contentLength in 1..SMALL_FILE_NO_SLICE_BYTES
                                val rangeHeader = if (isSmallFile) null
                                                  else nextRangeHeader(supportsRange, contentLength, nextRangeStart)
                                openDownloadStream(task.url, rangeHeader, downloadClient, task.userAgent).use { streamResult ->
                                    if (streamResult.code !in 200..299) {
                                        val message = "HTTP ${streamResult.code}"
                                        firstError.compareAndSet(null, message)
                                        if (streamResult.code in 400..499) {
                                            throw FatalRequestException("请求失败：$message")
                                        }
                                        delay(retryBackoffMs)
                                        retryBackoffMs = (retryBackoffMs * 2).coerceAtMost(RETRY_BACKOFF_MAX_MS)
                                        return@use
                                    }
                                    val stream = streamResult.inputStream ?: return@use
                                    var readAny = false
                                    while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                                        val read = stream.read(buffer)
                                        if (read <= 0) break
                                        if (hasReachedTarget(consumed.get(), task.targetBytes)) break
                                        addConsumedSafely(consumed, task.targetBytes, read.toLong())
                                        lastProgressAt.set(System.currentTimeMillis())
                                        readAny = true
                                    }
                                    if (readAny) retryBackoffMs = RETRY_BACKOFF_BASE_MS
                                }
                            }
                            if (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                                // 小文件整份下完立刻重来（5ms），大文件分片之间用退避值
                                val loopDelay = if (contentLength in 1..SMALL_FILE_NO_SLICE_BYTES) SMALL_FILE_LOOP_DELAY_MS else retryBackoffMs
                                if (loopDelay > 0L) delay(loopDelay)
                            }
                        } catch (e: FatalRequestException) {
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
                            pushError(e.message ?: e.javaClass.simpleName)
                            throw e
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: CronetException) {
                            val count = cronetConsecutiveFailures.incrementAndGet()
                            if (count >= CRONET_FAILURE_THRESHOLD) {
                                cronetTemporarilyDisabled = true
                                Log.w(TAG, "Cronet disabled after repeated failures: ${e.message}")
                            }
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
                            pushError(e.message ?: e.javaClass.simpleName)
                            delay(retryBackoffMs)
                            retryBackoffMs = (retryBackoffMs * 2).coerceAtMost(RETRY_BACKOFF_MAX_MS)
                        } catch (e: Exception) {
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
                            pushError(e.message ?: e.javaClass.simpleName)
                            delay(retryBackoffMs)
                            retryBackoffMs = (retryBackoffMs * 2).coerceAtMost(RETRY_BACKOFF_MAX_MS)
                        }
                    }
                }
            }

            var noDataStrikes = 0
            var lastStrikeCheckConsumed = -1L
            while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                val now = System.currentTimeMillis()
                if (now - lastProgressAt.get() > 15_000L) {
                    // P0-8h 修复②：瞬时网络抖动（ISP 凌晨重拨/CDN 节点切换）不再直接判死——
                    // 先重置计时器给重连一个窗口，连续 3 轮无数据才放弃该链接
                    noDataStrikes++
                    if (noDataStrikes >= 3) {
                        currentCoroutineContext().cancelChildren()
                        val reason = firstError.get()?.let { "，最近错误：$it" } ?: ""
                        throw IllegalStateException("约45秒内无有效数据，已重试3轮，请检查链接可用性$reason")
                    }
                    // 重置计时器，给重连一个新的 15 秒窗口
                    lastProgressAt.set(now)
                    Log.w(TAG, "no data for 15s (strike $noDataStrikes/3), workers keep retrying: ${task.url}")
                } else if (consumed.get() != lastStrikeCheckConsumed) {
                    // 数据恢复流动后清零 strike 计数（三次独立抖动不会累积误杀）
                    lastStrikeCheckConsumed = consumed.get()
                    noDataStrikes = 0
                }
                delay(100)
            }

            currentCoroutineContext().cancelChildren()
            progressJob.cancel()

            withContext(Dispatchers.Main) {
                // 修复「没跑够却记成完成」：原来这里对有限目标无条件写 task.targetBytes，
                // 导致断网/失败时实际只下了 846MB，界面却显示 3.00GB / 100% 完成。
                // 现在按真实计数收尾，未达标就不该记成目标值。
                val realConsumed = consumed.get()
                val finalConsumed = if (isUnlimitedTarget(task.targetBytes)) {
                    realConsumed
                } else {
                    minOf(realConsumed, task.targetBytes)
                }
                currentTaskConsumed = finalConsumed
                currentSpeedBytesPerSec = 0L
                updateConsumed(task.id, finalConsumed)
                // P2 修复：任务收尾强制落盘，保证崩溃/停止也不丢当日统计
                flushDailyTrafficIfDirty(true)
            }
        }
    }

    private fun nextRangeHeader(supportsRange: Boolean, contentLength: Long, nextRangeStart: AtomicLong): String? {
        val chunkSize = currentRangeChunkSizeBytes()
        if (!supportsRange || contentLength <= chunkSize) return null
        val maxStart = (contentLength - chunkSize).coerceAtLeast(0L)
        val rawStart = nextRangeStart.getAndAdd(chunkSize)
        val start = rawStart % (maxStart + 1L)
        val end = (start + chunkSize - 1L).coerceAtMost(contentLength - 1L)
        return "bytes=$start-$end"
    }

    private suspend fun ensureCronetEngine(): CronetEngine? {
        if (cronetEngine != null) return cronetEngine
        if (cronetReadyAttempted) return null
        cronetReadyAttempted = true

        val app = getApplication<Application>()
        val installed = runCatching {
            suspendCancellableCoroutine<Boolean> { cont ->
                CronetProviderInstaller.installProvider(app).addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(task.isSuccessful)
                }
            }
        }.getOrDefault(false)
        if (!installed) return null

        return try {
            CronetEngine.Builder(app)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .build()
                .also { cronetEngine = it }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun downloadWithCronet(
        engine: CronetEngine,
        url: String,
        userAgent: String?,
        targetCounter: AtomicLong,
        targetBytes: Long,
        onBytesRead: (Long) -> Unit,
    ): Int = suspendCancellableCoroutine { cont ->
        val callback = object : UrlRequest.Callback() {
            private val buffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE)
            private var statusCode = 0

            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                request.followRedirect()
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                statusCode = info.httpStatusCode
                if (statusCode !in 200..299) {
                    request.cancel()
                    if (cont.isActive) cont.resume(statusCode)
                    return
                }
                buffer.clear()
                request.read(buffer)
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                val bytesRead = byteBuffer.position().toLong()
                if (bytesRead > 0L) {
                    onBytesRead(bytesRead)
                }
                if (!cont.isActive || hasReachedTarget(targetCounter.get(), targetBytes)) {
                    request.cancel()
                    if (cont.isActive) cont.resume(statusCode)
                    return
                }
                byteBuffer.clear()
                request.read(byteBuffer)
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                if (cont.isActive) cont.resume(info.httpStatusCode)
            }

            override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                if (cont.isActive) {
                    cont.resumeWithException(error)
                }
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                if (cont.isActive) cont.resume(statusCode)
            }
        }

        try {
            val builder = engine.newUrlRequestBuilder(url, callback, clientDispatcher.executorService)
                .setHttpMethod("GET")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", userAgent.orEmpty())
                .addHeader("Cache-Control", "no-store")
                .disableCache()
            val request = builder.build()

            cont.invokeOnCancellation { request.cancel() }
            request.start()
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }

    /**
     * 下载用 OkHttpClient。
     * 修复：原来每次 new worker 都新建一个 OkHttpClient，且池是 ConnectionPool(1, 1, 1s)——
     * 每池只留 1 条连接、1 秒就过期，64 worker = 64 个独立池 + 每个 Range 请求都重新 TCP/TLS 握手。
     * 实测同一应用宝 CDN 大文件：这样只能跑 136MB/s，而 NAS 侧 curl 复用长连接能到 203MB/s。
     * 现在改为进程内共享单例 + 足够大的池（keepAlive 5 分钟），让 Range 请求复用连接。
     */
    private fun downloadClient(): OkHttpClient = downloadClientInstance ?: synchronized(this) {
        downloadClientInstance ?: OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            // P0 修复：共享单例后必须放开 Dispatcher 的并发上限。
            // OkHttp 默认 maxRequestsPerHost = 5，64 个 worker 共用单例时会被压到 5 并发，
            // 真机上咪咕单流 ~10.6MB/s × 5 ≈ 53MB/s，表现为"网速很慢"。
            // 容器里测不出来是因为 5 个连接就能跑满容器 138MB/s 天花板。
            .dispatcher(Dispatcher().apply {
                maxRequests = MAX_EFFECTIVE_CONCURRENCY * 4
                maxRequestsPerHost = MAX_EFFECTIVE_CONCURRENCY * 4
            })
            .connectionPool(ConnectionPool(MAX_EFFECTIVE_CONCURRENCY * 2, 5, TimeUnit.MINUTES))
            .build()
            .also { downloadClientInstance = it }
    }

    private fun buildDownloadClient(): OkHttpClient = downloadClient()

    private fun applyUserAgentHeader(builder: Request.Builder, userAgent: String?) {
        // L-ua 修复：UA 为空时不发送空 User-Agent 头（部分 CDN 对空 UA 敏感直接 403），
        // 省略该头让 OkHttp/系统默认 UA 自然生效
        val ua = userAgent?.trim().orEmpty()
        if (ua.isNotEmpty()) {
            builder.header("User-Agent", ua)
        }
    }

    private suspend fun openDownloadStream(url: String, rangeHeader: String?, downloadClient: OkHttpClient, userAgent: String?): StreamResult {
        val cronet: CronetEngine? = null
        if (cronet != null) {
            try {
                val connection = withContext(Dispatchers.IO) {
                    (cronet.openConnection(URL(url)) as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 15_000
                        instanceFollowRedirects = true
                        setRequestProperty("Accept", "*/*")
                        // L-ua：UA 为空时不发送空头（与 OkHttp 路径行为一致）
                        userAgent?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            setRequestProperty("User-Agent", it)
                        }
                        setRequestProperty("Cache-Control", "no-store")
                        if (rangeHeader != null) {
                            setRequestProperty("Range", rangeHeader)
                        }
                        connect()
                    }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                return StreamResult(code, stream, connection.contentLengthLong) { connection.disconnect() }
            } catch (_: Exception) {
            }
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .cacheControl(CacheControl.Builder().noStore().build())
            .header("Accept", "*/*")

        applyUserAgentHeader(requestBuilder, userAgent)
        if (rangeHeader != null) {
            requestBuilder.header("Range", rangeHeader)
        }
        val response = downloadClient.newCall(requestBuilder.build()).execute()
        val stream = if (response.isSuccessful) response.body?.byteStream() else response.body?.byteStream()
        return StreamResult(response.code, stream, response.body?.contentLength() ?: -1L) { response.close() }
    }

    private fun addConsumedSafely(counter: AtomicLong, target: Long, incoming: Long) {
        if (isUnlimitedTarget(target)) {
            counter.addAndGet(incoming)
            return
        }
        while (true) {
            val current = counter.get()
            if (current >= target) return
            val accepted = minOf(incoming, target - current)
            if (counter.compareAndSet(current, current + accepted)) return
        }
    }

    private fun updateConsumed(taskId: Long, taskConsumed: Long) {
        val previousTotal = totalConsumedBytes
        links = links.map {
            if (it.id == taskId) it.copy(consumedBytes = taskConsumed) else it
        }
        totalConsumedBytes = links.sumOf { item ->
            val target = activeRunTargets[item.id] ?: return@sumOf 0L
            if (isUnlimitedTarget(target)) item.consumedBytes else minOf(item.consumedBytes, target)
        }
        addDailyTraffic((totalConsumedBytes - previousTotal).coerceAtLeast(0L))
    }

    private fun persistLinks() {
        val array = JSONArray()
        links.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("url", item.url)
                    put("targetGbText", item.targetGbText)
                    put("userAgent", item.userAgent)
                    put("enabled", item.enabled)
                    // P0-8h 修复③：断点数据一并持久化，被杀重启后才能续跑
                    put("consumedBytes", item.consumedBytes)
                }
            )
        }
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putString(PREF_LINKS, array.toString())
            .apply()
        schedulePublicJsonBackup()
    }

    private fun schedulePublicJsonBackup() {
        publicBackupWriteJob?.cancel()
        publicBackupWriteJob = viewModelScope.launch {
            delay(400)
            // M-d 修复：JSON 序列化从调用者线程（常为主线程）挪到 IO 线程
            withContext(Dispatchers.IO) {
                val document = createBackupDocument().toString()
                PublicJsonBackup.write(getApplication(), document)
            }
        }
    }

    private fun loadLinks() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
        val raw = prefs.getString(PREF_LINKS, null) ?: return
        // R1 修复：links_json 损坏时不再让 ViewModel 构造崩溃（启动即崩且用户无法自救），
        // 解析失败回退空列表并清除坏数据，让用户可进 App 重新配置
        val array = runCatching { JSONArray(raw) }.getOrNull()
        if (array == null) {
            Log.w(TAG, "links_json corrupted, resetting to empty list")
            runCatching { prefs.edit().remove(PREF_LINKS).apply() }
            return
        }
        val loaded = mutableListOf<LinkItem>()
        for (i in 0 until array.length()) {
            val obj = runCatching { array.getJSONObject(i) }.getOrNull() ?: continue
            loaded += LinkItem(
                id = obj.optLong("id", i.toLong() + 1L),
                name = obj.optString("name", ""),
                url = obj.optString("url", ""),
                targetGbText = obj.optString("targetGbText", ""),
                userAgent = obj.optString("userAgent", ""),
                enabled = obj.optBoolean("enabled", true),
                consumedBytes = obj.optLong("consumedBytes", 0L).coerceAtLeast(0L),
            )
        }
        links = loaded
    }

    private fun loadTheme() {
        isDarkTheme = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .getBoolean(PREF_THEME_DARK, false)
    }

    private fun loadSettings() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
        threadCount = prefs.getInt(PREF_THREAD_COUNT, 8).coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
        activeThreadCount = threadCount
        userAgentText = runCatching { prefs.getString(PREF_USER_AGENT, DEFAULT_USER_AGENT) }
            .getOrNull()
            ?: DEFAULT_USER_AGENT
        if (userAgentText == LEGACY_ANDROID_APP_USER_AGENT) userAgentText = DEFAULT_USER_AGENT
        val userAgentListRaw = runCatching { prefs.getString(PREF_USER_AGENT_LIST, null) }.getOrNull()
        userAgentOptions = loadUserAgentOptions(userAgentListRaw)
        keepAliveEnabled = prefs.getBoolean(PREF_KEEP_ALIVE, true)
        keepScreenAwakeEnabled = prefs.getBoolean(PREF_KEEP_SCREEN_AWAKE, true)
        progressNotificationEnabled = prefs.getBoolean(PREF_PROGRESS_NOTIFICATION, true)
        trafficUnit = prefs.getString(PREF_TRAFFIC_UNIT, DEFAULT_TRAFFIC_UNIT)
            ?.let { if (it == TRAFFIC_UNIT_GB) TRAFFIC_UNIT_GB else TRAFFIC_UNIT_GIB }
            ?: DEFAULT_TRAFFIC_UNIT
        trafficUnitMode = trafficUnit
        refreshBackgroundSystemStatus()
    }

    private fun persistSettings() {
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putInt(PREF_THREAD_COUNT, threadCount)
            .putString(PREF_USER_AGENT, userAgentText)
            .putString(PREF_USER_AGENT_LIST, serializeUserAgentOptions(userAgentOptions).toString())
            .putBoolean(PREF_KEEP_ALIVE, keepAliveEnabled)
            .putBoolean(PREF_KEEP_SCREEN_AWAKE, keepScreenAwakeEnabled)
            .putBoolean(PREF_PROGRESS_NOTIFICATION, progressNotificationEnabled)
            .putString(PREF_TRAFFIC_UNIT, trafficUnit)
            .apply()
        schedulePublicJsonBackup()
    }

    private fun resolveTaskUserAgent(item: LinkItem): String? {
        return item.userAgent.trim().ifEmpty { userAgentText.trim() }.ifEmpty { null }
    }

    private fun loadUserAgentOptions(raw: String?): List<SavedUserAgent> {
        if (raw.isNullOrBlank()) return defaultUserAgentOptions()
        return runCatching {
            val array = JSONArray(raw)
            buildList<SavedUserAgent> {
                for (i in 0 until array.length()) {
                    when (val entry = array.opt(i)) {
                        is JSONObject -> {
                            val name = entry.optString("name").trim().take(40)
                            val value = entry.optString("value").trim().take(500)
                            if (
                                name.isNotEmpty() &&
                                value.isNotEmpty() &&
                                value != LEGACY_ANDROID_APP_USER_AGENT &&
                                none { it.name == name || it.value == value }
                            ) {
                                add(SavedUserAgent(name = name, value = value))
                            }
                        }

                        is String -> {
                            val value = entry.trim().take(500)
                            if (value.isNotEmpty() && value != LEGACY_ANDROID_APP_USER_AGENT && none { it.value == value }) {
                                val name = if (value == COMMON_ANDROID_USER_AGENT) "Android Chrome" else "自定义 UA ${size + 1}"
                                add(SavedUserAgent(name = name, value = value))
                            }
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
            .let { saved ->
                val android = defaultUserAgentOptions().single()
                listOf(android) + saved.filterNot { it.value == android.value }
            }
    }

    private fun serializeUserAgentOptions(options: List<SavedUserAgent>): JSONArray = JSONArray().apply {
        options.filterNot { it.builtIn }.forEach { option ->
            put(
                JSONObject().apply {
                    put("name", option.name)
                    put("value", option.value)
                },
            )
        }
    }

    private fun loadDailyTraffic() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
        val today = LocalDate.now().toString()
        if (prefs.getString(PREF_DAILY_TRAFFIC_DATE, null) == today) {
            dailyTrafficBytes = prefs.getLong(PREF_DAILY_TRAFFIC_BYTES, 0L)
            dailyTaskCount = prefs.getInt(PREF_DAILY_TASK_COUNT, 0)
        } else {
            persistDailyTraffic(today, 0L, 0)
        }
    }

    private fun addDailyTraffic(bytes: Long) {
        if (bytes <= 0L) return
        val today = LocalDate.now().toString()
        if (getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
                .getString(PREF_DAILY_TRAFFIC_DATE, null) != today) {
            dailyTrafficBytes = 0L
            dailyTaskCount = 0
        }
        dailyTrafficBytes += bytes
        // P2 修复：不再每秒同步写盘/写云贡献存储。
        // 内存累加 + 脏标记，由 flushDailyTrafficIfDirty() 定期（及任务结束/停止时）落盘。
        dailyTrafficDirty = true
        cloudSession?.let { session ->
            deviceTrafficContributionStore.recordLocalTraffic(session, today, bytes, 0)
        }
    }

    private fun flushDailyTrafficIfDirty(force: Boolean) {
        if (!dailyTrafficDirty && !force) return
        persistDailyTraffic(LocalDate.now().toString(), dailyTrafficBytes, dailyTaskCount)
        dailyTrafficDirty = false
    }

    private var dailyTrafficDirty = false

    private fun addDailyTaskCount(count: Int) {
        if (count <= 0) return
        val today = LocalDate.now().toString()
        if (getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
                .getString(PREF_DAILY_TRAFFIC_DATE, null) != today) {
            dailyTrafficBytes = 0L
            dailyTaskCount = 0
        }
        dailyTaskCount += count
        persistDailyTraffic(today, dailyTrafficBytes, dailyTaskCount)
        cloudSession?.let { session ->
            deviceTrafficContributionStore.recordLocalTraffic(session, today, 0L, count)
        }
    }

    private fun persistDailyTraffic(date: String, bytes: Long, taskCount: Int) {
        dailyTrafficBytes = bytes
        dailyTaskCount = taskCount
        getApplication<Application>().getSharedPreferences(PREFS_NAME, 0).edit()
            .putString(PREF_DAILY_TRAFFIC_DATE, date)
            .putLong(PREF_DAILY_TRAFFIC_BYTES, bytes)
            .putInt(PREF_DAILY_TASK_COUNT, taskCount)
            .apply()
    }

    private fun currentRangeChunkSizeBytes(): Long = activeRangeChunkSizeBytes.get()
        .coerceIn(MIN_ADAPTIVE_CHUNK_SIZE_BYTES, MAX_ADAPTIVE_CHUNK_SIZE_BYTES)

    /**
     * 动态切片：分片大小按「传输时长 ≥ TTFB 的 3 倍」反推，而不是按文件大小拍脑袋。
     *
     * 原来按文件大小分档（0.6MB→32KB）会让小文件每片只传 4ms、却付 50ms 的 TTFB，
     * 切片开销占 90%+。现在按实测速度算：chunk = speed × 3 × TTFB，
     * 速度慢时自动缩小分片（保证多线程同时开工），速度快时自动放大（减少请求数）。
     */
    private fun chooseChunkSizeForContent(contentLength: Long, speedBytesPerSec: Long): Long {
        // 小文件走整文件模式，不分片
        if (contentLength in 1..SMALL_FILE_NO_SLICE_BYTES) {
            return contentLength.coerceIn(MIN_ADAPTIVE_CHUNK_SIZE_BYTES, MAX_ADAPTIVE_CHUNK_SIZE_BYTES)
        }
        // 还没测到速度（刚启动）：按文件大小给一个保守初值，后续每秒按实测速度重算
        val initial = when {
            contentLength in 1..(16L * 1024L * 1024L) -> 256L * 1024L
            contentLength in 1..(128L * 1024L * 1024L) -> 2L * 1024L * 1024L
            contentLength in 1..(1024L * 1024L * 1024L) -> 4L * 1024L * 1024L
            contentLength in 1..(4L * 1024L * 1024L * 1024L) -> 8L * 1024L * 1024L
            contentLength > 0L -> 16L * 1024L * 1024L
            else -> DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES
        }
        if (speedBytesPerSec <= 0L) {
            return initial.coerceIn(MIN_ADAPTIVE_CHUNK_SIZE_BYTES, MAX_ADAPTIVE_CHUNK_SIZE_BYTES)
        }
        // 动态：每片传输时长 ≈ 3 × TTFB（50ms），既不让请求数爆炸也不让 TTFB 占比过高
        val dynamic = speedBytesPerSec * (3L * TTFB_BUDGET_MS) / 1000L
        // 不要超过文件的 1/8（否则并发铺不开），也不要小于 32KB
        val maxUseful = (contentLength / 8L).coerceAtLeast(MIN_ADAPTIVE_CHUNK_SIZE_BYTES)
        return dynamic.coerceIn(MIN_ADAPTIVE_CHUNK_SIZE_BYTES, minOf(maxUseful, MAX_ADAPTIVE_CHUNK_SIZE_BYTES))
    }

    /**
     * 把底层异常文本翻译成用户能看懂的自查提示。
     * 尤其是 CLEARTEXT：明文 http 被系统拦掉时异常文本是
     * "CLEARTEXT communication to xxx not permitted by network security policy"，
     * 用户看到原文完全无法行动，翻译后能直接指向「换 https 链接」。
     */
    private fun friendlyErrorText(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("cleartext") ->
                "链接是明文 http，被系统拦截（改用 https 链接）"
            lower.contains("timeout") || lower.contains("timed out") ->
                "连接超时（服务器无响应或网络不通）"
            lower.contains("unknownhost") || lower.contains("unable to resolve") ->
                "域名解析失败（检查域名拼写或 DNS）"
            lower.contains("ssl") || lower.contains("certificate") || lower.contains("handshake") ->
                "TLS/证书错误（https 握手失败）"
            lower.contains("http 403") || lower.contains("请求失败：403") ->
                "服务器拒绝访问 403（可能需 UA 或该链接对当前出口限流）"
            lower.contains("http 404") || lower.contains("请求失败：404") ->
                "链接已失效 404（请更换下载地址）"
            lower.contains("http 4") ->
                "服务器返回 ${raw.substringAfter("HTTP ").take(3)}，链接不可用"
            lower.contains("econnreset") || lower.contains("connection reset") ->
                "连接被重置（服务器主动断开）"
            lower.contains("failed to connect") || lower.contains("connectexception") ->
                "无法建立连接（网络或服务器不可达）"
            else -> raw.take(80)
        }
    }

    private fun computeActiveThreadCount(speedBytesPerSec: Long, contentLength: Long, targetBytes: Long = 0L): Int {
        val desired = adaptiveConcurrencyFor(contentLength, threadCount, targetBytes)
        activeRangeChunkSizeBytes.set(chooseChunkSizeForContent(contentLength, speedBytesPerSec))
        return desired
    }

    /**
     * 并发数决策。
     *
     * 关键区分（2026-10-30 真机反馈修复）：
     *  - 「一次性下载小文件」：文件 <2MB 时 4 线程够用，64 并发只是白白堆连接。
     *  - 「小文件重复刷大流量」：同一个 0.6MB gif 要刷 5GB，等于重复下载 8000+ 次。
     *    这时并发数就是吞吐量本身——4 线程实测只有 1.91MB/s，用户体感"网速很慢"。
     *    判据是「目标字节数 ÷ 文件大小」的重复次数：超过 [SMALL_FILE_REPEAT_THRESHOLD] 次
     *    就不再降并发，按用户设定跑。
     */
    private fun adaptiveConcurrencyFor(contentLength: Long, requested: Int, targetBytes: Long = 0L): Int {
        val maxAllowed = requested.coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
        if (contentLength <= 0L) return maxAllowed   // 未知长度：用用户设定
        // 小文件被反复下载凑流量时，并发就是吞吐量，不能按"文件小"去压
        if (targetBytes > 0L && contentLength > 0L) {
            val repeats = targetBytes / contentLength
            if (repeats >= SMALL_FILE_REPEAT_THRESHOLD) return maxAllowed
        }
        val cap = when {
            contentLength < 2L * 1024L * 1024L -> 4     // <2MB：单次秒完，4 线程足够
            contentLength < 32L * 1024L * 1024L -> 8    // <32MB
            contentLength < 128L * 1024L * 1024L -> 16  // <128MB
            contentLength < 512L * 1024L * 1024L -> 32  // <512MB
            else -> maxAllowed                          // 大文件：用户设定（可到 64）
        }
        return cap.coerceIn(1, maxAllowed)
    }


    private fun syncKeepAliveService() {
        val app = getApplication<Application>()
        if (keepAliveEnabled) {
            TrafficKeepAliveService.start(
                context = app,
                keepScreenAwake = keepScreenAwakeEnabled,
                showProgress = progressNotificationEnabled,
                taskRunning = isRunning,
                taskName = currentTaskName,
                totalConsumedBytes = totalConsumedBytes,
                totalTargetBytes = totalTargetBytes,
                speedBytesPerSec = currentSpeedBytesPerSec,
            )
        } else {
            TrafficKeepAliveService.stop(app)
        }
    }

    private fun updateBackgroundNotification() {
        if (!keepAliveEnabled) return
        TrafficKeepAliveService.update(
            context = getApplication(),
            keepScreenAwake = keepScreenAwakeEnabled,
            showProgress = progressNotificationEnabled,
            taskRunning = isRunning,
            taskName = currentTaskName,
            totalConsumedBytes = totalConsumedBytes,
            totalTargetBytes = totalTargetBytes,
            speedBytesPerSec = currentSpeedBytesPerSec,
        )
    }

    override fun onCleared() {
        TrafficNotificationCommandBus.unregister(this)
        super.onCleared()
        // Keep the run and the keep-alive service alive after Activity death: the download now
        // lives in appScope and the foreground service keeps the process alive. Only stop the
        // service when the run itself has finished (user pressed stop or all tasks done).
        if (!isRunning) {
            TrafficKeepAliveService.stop(getApplication())
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrafficViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TrafficViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// TrafficScreen / 新 SpeedPanel 已迁移到 NpxNewUi.kt（2026-10-25 定稿新 UI）。
// 旧版 SpeedPanel 保留为 LegacySpeedPanel 供对照，不再挂载。
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LegacySpeedPanel(vm: TrafficViewModel) {
    val context = LocalContext.current
    val qqGroup = "1074735930"
    val hasFiniteTotalTarget = vm.totalTargetBytes > 0L
    val progress = if (hasFiniteTotalTarget) {
        (vm.totalConsumedBytes.toFloat() / vm.totalTargetBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remain = if (hasFiniteTotalTarget) (vm.totalTargetBytes - vm.totalConsumedBytes).coerceAtLeast(0L) else 0L
    val etaSec = if (hasFiniteTotalTarget && vm.currentSpeedBytesPerSec > 0L) remain / vm.currentSpeedBytesPerSec else -1L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("运行状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(vm.statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                        }
                        FilterChip(
                            selected = vm.isRunning,
                            onClick = {},
                            label = { Text(if (vm.isRunning) "运行中" else "待开始") },
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetricCard("实时网速", "${formatBytes(vm.currentSpeedBytesPerSec)}/s", Modifier.weight(1f))
                        MetricCard("总进度", if (hasFiniteTotalTarget) "${"%.2f".format(progress * 100f)}%" else "无限", Modifier.weight(1f))
                        MetricCard("预计剩余", if (etaSec >= 0L) formatDuration(etaSec) else "-", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("任务详情", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("当前链接：${vm.currentTaskName}")
                    Text("当前链接消耗：${formatBytes(vm.currentTaskConsumed)} / ${formatTargetBytes(vm.currentTaskTarget)}")
                    Text("总流量消耗：${formatBytes(vm.totalConsumedBytes)} / ${formatTargetBytes(vm.totalTargetBytes)}")
                    Text("活跃线程：${vm.activeThreadCount} / ${vm.threadCount}")
                    Slider(
                        value = vm.threadCount.toFloat(),
                        onValueChange = { vm.updateThreadCount(it.toInt()) },
                        enabled = true,
                        valueRange = 1f..64f,
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = { vm.start() }, enabled = !vm.isRunning) {
                    Text("开始")
                }
                Button(onClick = { vm.stop() }, enabled = vm.isRunning) {
                    Text("停止")
                }
            }
        }

        item {
            PublicIpCard(vm)
        }

        item {
            Text(
                "本测试工具仅提供网络速度自查，请勿用于非法用途，使用本工具造成的一切后果由用户承担！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "更新QQ群：$qqGroup（点击复制）",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("更新QQ群", qqGroup))
                        Toast.makeText(context, "QQ群号已复制：$qqGroup", Toast.LENGTH_SHORT).show()
                    },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "版本号：${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(vm: TrafficViewModel) {
    val context = LocalContext.current
    var savedUserAgentName by rememberSaveable { mutableStateOf("") }
    val localJsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(vm::importLocalJsonBackup)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshBackgroundSystemStatus() }
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { vm.refreshBackgroundSystemStatus() }

    LaunchedEffect(Unit) {
        vm.refreshBackgroundSystemStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CloudAccountPanel(vm)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("请求 UA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "内置 Android Chrome UA。链接留空时继承全局 UA；自定义 UA 可命名保存，便于链接快速选择。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    )
                    UserAgentDropdown(
                        label = "已保存的 UA",
                        value = vm.userAgentText,
                        options = vm.userAgentOptions,
                        emptyLabel = "空 UA（不使用默认 UA）",
                        onValueSelected = { vm.selectGlobalUserAgent(it) },
                    )
                    OutlinedTextField(
                        value = vm.userAgentText,
                        onValueChange = { vm.updateUserAgent(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("自定义全局 User-Agent") },
                        minLines = 2,
                        supportingText = { Text("填写内容后，为它指定名称再保存。") },
                    )
                    OutlinedTextField(
                        value = savedUserAgentName,
                        onValueChange = { savedUserAgentName = it.take(40) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("UA 名称") },
                        singleLine = true,
                        supportingText = { Text("例如：云手机、线路 A") },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                vm.saveCurrentUserAgentToList(savedUserAgentName)
                                savedUserAgentName = ""
                            },
                            enabled = vm.userAgentText.isNotBlank() && savedUserAgentName.isNotBlank(),
                        ) {
                            Text("保存为新 UA")
                        }
                        Button(onClick = { vm.removeCurrentUserAgentFromList() }, enabled = vm.canRemoveCurrentUserAgent()) {
                            Text("从列表移除")
                        }
                        Button(onClick = { vm.clearGlobalUserAgent() }) {
                            Text("不使用 UA")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("本地 JSON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "启动时会自动尝试读取公共下载目录中的备份；也可以手动选择 JSON 恢复链接和设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    )
                    Button(
                        onClick = { localJsonImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                        enabled = !vm.isRunning,
                    ) {
                        Text("导入本地 JSON")
                    }
                }
            }
        }

        item {
            BackgroundSettingsPanel(
                vm = vm,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.refreshBackgroundSystemStatus()
                    }
                },
                onRequestBatteryExemption = {
                    try {
                        batteryOptimizationLauncher.launch(
                            Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenNotificationSettings = {
                    val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, TrafficKeepAliveService.notificationChannelId())
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            },
                        )
                    }
                },
            )
        }

        item {
            BackupPanel(vm)
        }

        item {
            UpdatePanel(vm = vm, context = context)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupPanel(vm: TrafficViewModel) {
    val saved = vm.backupConnection
    var provider by rememberSaveable { mutableStateOf(saved.provider) }
    var webDavUrl by rememberSaveable { mutableStateOf(saved.webDavUrl) }
    var webDavUsername by rememberSaveable { mutableStateOf(saved.webDavUsername) }
    // S2 修复：密码类字段用 remember（不参与 saved-instance-state 序列化），
    // 进程死亡后从 EncryptedSharedPreferences 重读，避免明文密码进系统存档 blob
    var webDavPassword by remember { mutableStateOf(saved.webDavPassword) }
    var s3Endpoint by rememberSaveable { mutableStateOf(saved.s3Endpoint) }
    var s3Region by rememberSaveable { mutableStateOf(saved.s3Region) }
    var s3Bucket by rememberSaveable { mutableStateOf(saved.s3Bucket) }
    var s3AccessKey by rememberSaveable { mutableStateOf(saved.s3AccessKey) }
    // S2 修复：同上，Secret Key 不进 saved-instance-state
    var s3SecretKey by remember { mutableStateOf(saved.s3SecretKey) }
    var s3ObjectPrefix by rememberSaveable { mutableStateOf(saved.s3ObjectPrefix) }
    var selectedBackup by remember { mutableStateOf<RemoteBackup?>(null) }

    fun connection() = BackupConnection(
        provider = provider,
        webDavUrl = webDavUrl.trim(),
        webDavUsername = webDavUsername.trim(),
        webDavPassword = webDavPassword,
        s3Endpoint = s3Endpoint.trim(),
        s3Region = s3Region.trim(),
        s3Bucket = s3Bucket.trim(),
        s3AccessKey = s3AccessKey.trim(),
        s3SecretKey = s3SecretKey,
        s3ObjectPrefix = s3ObjectPrefix.trim(),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("备份与恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "每次上传会在远端目录新增一个带时间戳的版本；在线列表按时间倒序分页显示。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = provider == "webdav",
                    onClick = { provider = "webdav"; vm.clearBackupList() },
                    label = { Text("WebDAV") },
                )
                FilterChip(
                    selected = provider == "s3",
                    onClick = { provider = "s3"; vm.clearBackupList() },
                    label = { Text("S3") },
                )
            }
            if (provider == "webdav") {
                OutlinedTextField(
                    value = webDavUrl,
                    onValueChange = { webDavUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WebDAV 备份目录") },
                    placeholder = { Text("https://dav.example.com/backups/") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = webDavUsername,
                    onValueChange = { webDavUsername = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WebDAV 用户名") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = webDavPassword,
                    onValueChange = { webDavPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WebDAV 密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            } else {
                OutlinedTextField(
                    value = s3Endpoint,
                    onValueChange = { s3Endpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("S3 Endpoint") },
                    placeholder = { Text("https://s3.amazonaws.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = s3Region,
                    onValueChange = { s3Region = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("S3 Region") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = s3Bucket,
                    onValueChange = { s3Bucket = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bucket") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = s3AccessKey,
                    onValueChange = { s3AccessKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Access Key") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = s3SecretKey,
                    onValueChange = { s3SecretKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Secret Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = s3ObjectPrefix,
                    onValueChange = { s3ObjectPrefix = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("对象前缀（文件夹）") },
                    placeholder = { Text("network-panel-x") },
                    singleLine = true,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.uploadBackup(connection()) }, enabled = !vm.backupBusy) {
                    Text("上传新备份")
                }
                Button(onClick = { vm.refreshBackupList(connection()) }, enabled = !vm.backupBusy) {
                    Text("刷新在线列表")
                }
            }
            if (vm.backupStatus.isNotBlank()) {
                Text(vm.backupStatus, style = MaterialTheme.typography.bodySmall)
            }
            vm.backupPageItems.forEach { backup ->
                val isLatest = vm.remoteBackups.firstOrNull()?.remoteId == backup.remoteId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (isLatest) "最新 · ${backup.fileName}" else backup.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        "${formatBackupTime(backup.modifiedAtEpochMs)} · ${formatBytes(backup.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    )
                    Button(onClick = { selectedBackup = backup }, enabled = !vm.backupBusy) {
                        Text("恢复这个版本")
                    }
                }
            }
            if (vm.remoteBackups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = vm::previousBackupPage, enabled = vm.backupPage > 0 && !vm.backupBusy) { Text("上一页") }
                    Text("${vm.backupPage + 1} / ${vm.backupPageCount}")
                    Button(onClick = vm::nextBackupPage, enabled = vm.backupPage + 1 < vm.backupPageCount && !vm.backupBusy) { Text("下一页") }
                }
            }
        }
    }

    selectedBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { selectedBackup = null },
            title = { Text("确认恢复备份？") },
            text = { Text("将恢复 ${backup.fileName}。当前链接、UA、测速设置和本机统计会被覆盖，云端账号不会改变。") },
            confirmButton = {
                Button(onClick = {
                    selectedBackup = null
                    vm.downloadBackup(connection(), backup)
                }) { Text("确认恢复") }
            },
            dismissButton = {
                Button(onClick = { selectedBackup = null }) { Text("取消") }
            },
        )
    }
}

fun formatBackupTime(epochMs: Long): String {
    if (epochMs <= 0L) return "时间未知"
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudAccountPanel(vm: TrafficViewModel) {
    var username by rememberSaveable { mutableStateOf(vm.savedCloudUsername) }
    var password by remember { mutableStateOf(vm.savedCloudPassword) }
    var rememberPassword by rememberSaveable { mutableStateOf(vm.savedCloudPassword.isNotBlank()) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var accountMode by rememberSaveable { mutableStateOf("login") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("云端账号与统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (vm.cloudUsername.isNotBlank()) {
                Text("已登录：${vm.cloudUsername}")
                Button(onClick = vm::logoutCloudAccount) { Text("退出登录") }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = accountMode == "login", onClick = { accountMode = "login" }, label = { Text("登录") })
                    FilterChip(selected = accountMode == "register", onClick = { accountMode = "register" }, label = { Text("注册") })
                    FilterChip(selected = accountMode == "reset", onClick = { accountMode = "reset" }, label = { Text("忘记密码") })
                }
                if (accountMode != "reset") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名") },
                        singleLine = true,
                    )
                }
                if (accountMode != "login") {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(254) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("邮箱") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (accountMode == "reset") "新密码" else "密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                if (accountMode == "login") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberPassword, onCheckedChange = { rememberPassword = it })
                        Text("保存密码")
                    }
                }
                if (accountMode != "login") {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("邮箱验证码") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (accountMode) {
                        "login" -> Button(onClick = { vm.loginCloudAccount(DEFAULT_CLOUD_API_URL, username, password, rememberPassword) }) { Text("登录") }
                        "register" -> {
                            Button(onClick = { vm.requestCloudRegistrationCode(DEFAULT_CLOUD_API_URL, username, email, password) }) { Text("发送验证码") }
                            Button(onClick = { vm.registerCloudAccount(DEFAULT_CLOUD_API_URL, username, email, password, code) }) { Text("完成注册") }
                        }
                        else -> {
                            Button(onClick = { vm.requestCloudPasswordResetCode(DEFAULT_CLOUD_API_URL, email) }) { Text("发送验证码") }
                            Button(onClick = { vm.resetCloudPassword(DEFAULT_CLOUD_API_URL, email, code, password) }) { Text("重置密码") }
                        }
                    }
                }
            }
            Text(
                vm.cloudStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsPanel(vm: TrafficViewModel) {
    var period by rememberSaveable { mutableStateOf("day") }
    val labels = mapOf("day" to "日", "month" to "月", "year" to "年")
    LaunchedEffect(vm.cloudUsername) {
        if (vm.cloudUsername.isNotBlank()) {
            vm.refreshCloudStats(period, isManual = false)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("流量统计排行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("本机今日流量：${formatBytes(vm.dailyTrafficBytes)}，任务数：${vm.dailyTaskCount}")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { (key, label) ->
                            FilterChip(
                                selected = period == key,
                                onClick = {
                                    if (period != key) {
                                        period = key
                                        vm.refreshCloudStats(key, isManual = false)
                                    }
                                },
                                label = { Text("${label}榜") },
                            )
                        }
                    }
                    Button(onClick = { vm.refreshCloudStats(period) }, enabled = vm.canManuallySyncCloudStats) {
                        Text(
                            when {
                                vm.statsLoading -> "同步中..."
                                vm.manualSyncCooldownSeconds > 0 -> "${vm.manualSyncCooldownSeconds} 秒后可同步"
                                else -> "同步并刷新排行"
                            },
                        )
                    }
                    Text(vm.cloudStatus, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("个人${labels[period]}统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val stats = vm.personalStats
                    Text("流量：${formatBytes(stats?.consumedBytes ?: 0L)}")
                    Text("任务数：${stats?.taskCount ?: 0L}")
                }
            }
        }
        item {
            Text("全体用户${labels[period]}流量排行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(vm.leaderboardEntries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("#${entry.rank}  ${entry.username}", fontWeight = FontWeight.Medium)
                    Text(formatBytes(entry.consumedBytes))
                }
            }
        }
    }
}

@Composable
fun BackgroundSettingsPanel(
    vm: TrafficViewModel,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("后台运行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "启用后，测速会使用前台服务在后台持续运行。Android 要求前台服务保留系统通知。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )

            BackgroundSettingRow(
                title = "后台常驻",
                description = "测速时启动前台服务，降低被系统回收的概率。",
                checked = vm.keepAliveEnabled,
                onCheckedChange = vm::updateKeepAliveEnabled,
            )
            BackgroundSettingRow(
                title = "熄屏继续运行",
                description = "使用唤醒锁保持 CPU 和网络任务；会增加耗电。",
                checked = vm.keepScreenAwakeEnabled,
                onCheckedChange = vm::updateKeepScreenAwakeEnabled,
                enabled = vm.keepAliveEnabled,
            )
            BackgroundSettingRow(
                title = "状态栏进度通知",
                description = "显示总流量、网速和任务状态文字，并在锁屏界面公开显示。",
                checked = vm.progressNotificationEnabled,
                onCheckedChange = vm::updateProgressNotificationEnabled,
                enabled = vm.keepAliveEnabled,
            )

            Text(
                if (vm.notificationPermissionGranted) "通知权限：已允许" else "通知权限：未允许，Android 13 及以上系统可能不在通知栏展示进度。",
                style = MaterialTheme.typography.bodySmall,
                color = if (vm.notificationPermissionGranted) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (!vm.notificationPermissionGranted) {
                Button(onClick = onRequestNotificationPermission) {
                    Text("授权通知")
                }
            }
            Button(onClick = onOpenNotificationSettings) {
                Text("锁屏通知设置")
            }

            Text(
                if (vm.ignoringBatteryOptimizations) "电池优化：已忽略" else "电池优化：建议忽略，避免熄屏后被系统限制。",
                style = MaterialTheme.typography.bodySmall,
                color = if (vm.ignoringBatteryOptimizations) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (!vm.ignoringBatteryOptimizations) {
                Button(onClick = onRequestBatteryExemption) {
                    Text("忽略电池优化")
                }
            }
        }
    }
}

@Composable
private fun BackgroundSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpdatePanel(vm: TrafficViewModel, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("在线更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "当前版本：${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (val state = vm.updateState) {
                UpdateState.Idle -> Text(
                    "从 GitHub Release 检查最新正式包。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )

                UpdateState.Checking -> {
                    Text("正在检查更新...", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is UpdateState.UpToDate -> Text(
                    "已是最新版本（${state.latestVersion}）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )

                is UpdateState.Available -> {
                    Text(
                        "发现新版本：${state.release.version}",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.release.notes.isNotBlank()) {
                        Text(
                            state.release.notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                is UpdateState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { vm.checkForUpdates() },
                    enabled = vm.updateState !is UpdateState.Checking,
                ) {
                    Text("检查更新")
                }
                Button(onClick = { openExternalLink(context, GITHUB_RELEASES_URL) }) {
                    Text("GitHub 更新")
                }
                val available = vm.updateState as? UpdateState.Available
                if (available != null) {
                    Button(
                        onClick = {
                            openExternalLink(
                                context = context,
                                url = available.release.apkDownloadUrl ?: available.release.releasePageUrl,
                            )
                        },
                    ) {
                        Text(if (available.release.apkDownloadUrl != null) "下载更新" else "查看发行页")
                    }
                }
            }
        }
    }
}

fun openExternalLink(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (error: Exception) {
        Log.w("NetworkPanelX", "Unable to open update URL", error)
        Toast.makeText(context, "无法打开下载页面", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgentDropdown(
    label: String,
    value: String,
    options: List<SavedUserAgent>,
    emptyLabel: String,
    enabled: Boolean = true,
    onValueSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.value == value }?.name ?: value.ifBlank { emptyLabel },
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(emptyLabel) },
                onClick = {
                    onValueSelected("")
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onValueSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PublicIpCard(vm: TrafficViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("公网 IP 信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = vm::refreshPublicIpInfo, enabled = !vm.publicIpInfoLoading) {
                    Text(if (vm.publicIpInfoLoading) "刷新中" else "刷新")
                }
            }
            when {
                vm.publicIpInfoLoading && vm.publicIpInfo == null -> Text("正在获取公网 IP…")
                vm.publicIpInfo != null -> {
                    val info = vm.publicIpInfo!!
                    IpInfoRow("IP 地址", info.ip)
                    IpInfoRow("位置", info.location)
                    IpInfoRow("ASN", info.asn)
                    IpInfoRow("运营商", info.isp)
                }
                else -> Text(vm.publicIpInfoError ?: "暂无 IP 信息")
            }
        }
    }
}

@Composable
private fun IpInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.size(width = 56.dp, height = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), maxLines = 1)
        Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SystemBarsEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as Activity).window
        val color = if (isDarkTheme) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.parseColor("#F1F5F9")
        window.statusBarColor = color
        window.navigationBarColor = color
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LinkManagePanel(
    vm: TrafficViewModel,
    onQuickTrafficStarted: () -> Unit,
) {
    val expandedIds = rememberSaveable { mutableStateOf(setOf<Long>()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { vm.addLink() }, enabled = !vm.isRunning) {
                        Text("新增链接")
                    }
                    Button(
                        onClick = {
                            if (vm.quickStartSelectedTraffic()) {
                                onQuickTrafficStarted()
                            }
                        },
                        enabled = !vm.isRunning && vm.links.any { it.enabled },
                    ) {
                        Text("一键测流")
                    }
                    Button(onClick = { vm.stop() }, enabled = vm.isRunning) {
                        Text("一键暂停")
                    }
                }
                Text("已勾选：${vm.links.count { it.enabled }} / ${vm.links.size}，一键测流每条 ${QUICK_TRAFFIC_TARGET_GB_TEXT}${gigabyteSuffix()}")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.links, key = { item -> item.id }) { item ->
                val index = vm.links.indexOfFirst { it.id == item.id }
                val expanded = item.id in expandedIds.value
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.enabled,
                                onCheckedChange = { vm.toggleLinkEnabled(item.id, it) },
                                enabled = !vm.isRunning,
                            )
                            Text("优先级：${index + 1}")
                            Button(
                                onClick = { vm.moveLinkUp(item.id) },
                                enabled = !vm.isRunning && index > 0,
                            ) {
                                Text("上")
                            }
                            Button(
                                onClick = { vm.moveLinkDown(item.id) },
                                enabled = !vm.isRunning && index < vm.links.lastIndex,
                            ) {
                                Text("下")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { vm.removeLink(item.id) },
                                enabled = !vm.isRunning,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Text("×", fontSize = 24.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val displayName = item.name.trim().ifEmpty { "未命名链接" }
                            Text(displayName)
                            Text(
                                text = if (expanded) "点击收起" else "点击展开",
                                modifier = Modifier.clickable {
                                    expandedIds.value = if (expanded) {
                                        expandedIds.value - item.id
                                    } else {
                                        expandedIds.value + item.id
                                    }
                                },
                            )
                        }

                        if (expanded) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { vm.updateLinkName(item.id, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("链接名称") },
                                enabled = !vm.isRunning,
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = item.url,
                                onValueChange = { vm.updateLinkUrl(item.id, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("下载链接 URL") },
                                enabled = !vm.isRunning,
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = item.targetGbText,
                                onValueChange = { vm.updateLinkTargetGb(item.id, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("目标流量 (${gigabyteSuffix()}，0=无限)") },
                                enabled = !vm.isRunning,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )

                            UserAgentDropdown(
                                label = "从保存的 UA 选择",
                                value = item.userAgent,
                                options = vm.userAgentOptions,
                                emptyLabel = "使用全局 UA",
                                enabled = !vm.isRunning,
                                onValueSelected = { vm.updateLinkUserAgent(item.id, it) },
                            )
                        }

                        val gbValue = item.targetGbText.toDoubleOrNull()
                        val targetBytes = gbValue
                            ?.takeIf { it > 0.0 }
                            ?.let { (it * bytesPerGigabyte()).toLong() } ?: 0L
                        val remainText = if (gbValue == 0.0) "无限" else formatBytes((targetBytes - item.consumedBytes).coerceAtLeast(0L))
                        Text("本次消耗：${formatBytes(item.consumedBytes)}，目标剩余：$remainText")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private class FatalRequestException(message: String) : IllegalStateException(message)

private class StreamResult(
    val code: Int,
    val inputStream: InputStream?,
    val contentLength: Long,
    private val onClose: () -> Unit,
) : AutoCloseable {
    override fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {
        }
        onClose()
    }
}

fun formatTargetBytes(bytes: Long): String {
    return if (isUnlimitedTarget(bytes)) "无限" else formatBytes(bytes)
}

/**
 * 流量显示单位制（全局可变状态，由设置页切换后即时生效）。
 *
 * 背景：运营商与手机系统设置一律按十进制统计
 * （1 GB = 1000^3 字节），而 Android/传统习惯用二进制（1 GiB = 1024^3 字节）。
 * 旧版固定按 1024 进制却显示 "GB"，导致 App 数字比运营商小约 7%
 * （39 GB 量级差 2.8 GB），用户对不上账。
 *
 * 现在提供开关：
 *  - GiB（默认）：显示 xxx GiB / MiB / KiB，数值与旧版完全一致（不改变老用户观感）
 *  - GB：显示 xxx GB / MB / KB，十进制，与运营商账单口径一致
 *
 * 注意：内部一律用字节（Long）存储和传输，单位制只影响「显示」和「输入解析」。
 */
/**
 * 流量显示单位制（全局 Compose 状态）。
 *
 * 必须是 mutableStateOf 而不是普通 @Volatile 变量：所有页面的 formatBytes()
 * 都在 Composable 里读取它，只有可观察状态变化才会触发重组，否则切换单位后
 * 界面数字不会更新（按钮高亮变了但数值还是旧口径）。
 */
val trafficUnitModeState = mutableStateOf(DEFAULT_TRAFFIC_UNIT)

var trafficUnitMode: String
    get() = trafficUnitModeState.value
    set(value) { trafficUnitModeState.value = value }

private const val KIB = 1024.0
private const val MIB = KIB * 1024.0
private const val GIB = MIB * 1024.0
private const val TIB = GIB * 1024.0
private const val PIB = TIB * 1024.0
private const val EIB = PIB * 1024.0

private const val KB_DEC = 1000.0
private const val MB_DEC = KB_DEC * 1000.0
private const val GB_DEC = MB_DEC * 1000.0
private const val TB_DEC = GB_DEC * 1000.0
private const val PB_DEC = TB_DEC * 1000.0
private const val EB_DEC = PB_DEC * 1000.0

/** 当前单位制下 1 GB/GiB 对应的字节数（供目标流量输入框换算用） */
fun bytesPerGigabyte(): Double =
    if (trafficUnitMode == TRAFFIC_UNIT_GB) GB_DEC else GIB

/** 当前单位制下的单位后缀，如 "GB" / "GiB" */
fun gigabyteSuffix(): String = if (trafficUnitMode == TRAFFIC_UNIT_GB) "GB" else "GiB"

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    return if (trafficUnitMode == TRAFFIC_UNIT_GB) {
        // 十进制：与运营商/手机系统口径一致
        when {
            bytes >= EB_DEC -> String.format("%.3f EB", bytes / EB_DEC)
            bytes >= PB_DEC -> String.format("%.3f PB", bytes / PB_DEC)
            bytes >= TB_DEC -> String.format("%.3f TB", bytes / TB_DEC)
            bytes >= GB_DEC -> String.format("%.2f GB", bytes / GB_DEC)
            bytes >= MB_DEC -> String.format("%.2f MB", bytes / MB_DEC)
            bytes >= KB_DEC -> String.format("%.2f KB", bytes / KB_DEC)
            else -> "$bytes B"
        }
    } else {
        // 二进制：传统习惯，后缀明确写成 GiB/MiB/KiB
        when {
            bytes >= EIB -> String.format("%.3f EiB", bytes / EIB)
            bytes >= PIB -> String.format("%.3f PiB", bytes / PIB)
            bytes >= TIB -> String.format("%.3f TiB", bytes / TIB)
            bytes >= GIB -> String.format("%.2f GiB", bytes / GIB)
            bytes >= MIB -> String.format("%.2f MiB", bytes / MIB)
            bytes >= KIB -> String.format("%.2f KiB", bytes / KIB)
            else -> "$bytes B"
        }
    }
}

/** 供非 Composable 的 Service 复用同一套格式化逻辑（如通知栏）。 */
fun formatBytesGlobal(bytes: Long): String = formatBytes(bytes)

/**
 * 统计榜单专用：**永远按二进制**（GiB/MiB），不受设置里的 GB/GiB 开关影响。
 *
 * 原因：云端榜单是全体用户的横向排名，如果每个人的客户端按不同口径渲染，
 * 同一个字节数在不同手机上显示成不同数字，排名看起来会自相矛盾。
 * 所以榜单固定二进制口径，与个人本机统计的口径解耦。
 */
fun formatBytesBinary(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    return when {
        bytes >= EIB -> String.format("%.3f EiB", bytes / EIB)
        bytes >= PIB -> String.format("%.3f PiB", bytes / PIB)
        bytes >= TIB -> String.format("%.3f TiB", bytes / TIB)
        bytes >= GIB -> String.format("%.2f GiB", bytes / GIB)
        bytes >= MIB -> String.format("%.2f MiB", bytes / MIB)
        bytes >= KIB -> String.format("%.2f KiB", bytes / KIB)
        else -> "$bytes B"
    }
}

fun formatDuration(totalSec: Long): String {
    if (totalSec < 0L) return "-"
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%dh %02dm %02ds", h, m, s)
    } else {
        String.format("%02dm %02ds", m, s)
    }
}




