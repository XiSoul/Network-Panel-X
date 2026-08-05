package com.example.networkpanelx

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

private const val PREFS_NAME = "traffic_prefs"
private const val PREF_LINKS = "links_json"
private const val PREF_THEME_DARK = "theme_dark"
private const val PREF_THREAD_COUNT = "thread_count"
private const val PREF_USER_AGENT = "user_agent"
private const val PREF_USER_AGENT_LIST = "user_agent_list"
private const val PREF_KEEP_ALIVE = "keep_alive"
private const val PREF_KEEP_SCREEN_AWAKE = "keep_screen_awake"
private const val PREF_PROGRESS_NOTIFICATION = "progress_notification"
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
private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0
private const val READ_BUFFER_SIZE = 1024 * 1024
private const val DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES = 2L * 1024L * 1024L
private const val MIN_ADAPTIVE_CHUNK_SIZE_BYTES = 128L * 1024L
private const val MAX_ADAPTIVE_CHUNK_SIZE_BYTES = 32L * 1024L * 1024L
private const val MAX_EFFECTIVE_CONCURRENCY = 64
private const val CRONET_FAILURE_THRESHOLD = 3
private const val FAST_RETRY_DELAY_MS = 0L
private const val WORKER_STAGGER_MS = 8L
private const val QUICK_TRAFFIC_TARGET_GB_TEXT = "0.1"
private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/XiSoul/Network-Panel-X/releases/latest"
private const val GITHUB_RELEASES_URL = "https://github.com/XiSoul/Network-Panel-X/releases"
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
    fun write(context: Context, document: String) {
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
            MaterialTheme(
                colorScheme = if (vm.isDarkTheme) {
                    darkColorScheme(
                        primary = Color(0xFF8AB4F8),
                        secondary = Color(0xFF80CBC4),
                        background = Color(0xFF0F172A),
                        surface = Color(0xFF1E293B),
                        surfaceVariant = Color(0xFF334155),
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF2563EB),
                        secondary = Color(0xFF0891B2),
                        background = Color(0xFFF1F5F9),
                        surface = Color.White,
                        surfaceVariant = Color(0xFFE2E8F0),
                    )
                },
            ) {
                SystemBarsEffect(isDarkTheme = vm.isDarkTheme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
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
    }

    var links by mutableStateOf(emptyList<LinkItem>())
        private set

    var isRunning by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("在管理页配置链接后开始测速")
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
    var keepScreenAwakeEnabled by mutableStateOf(true)
        private set
    var progressNotificationEnabled by mutableStateOf(true)
        private set
    var notificationPermissionGranted by mutableStateOf(true)
        private set
    var ignoringBatteryOptimizations by mutableStateOf(false)
        private set
    var isDarkTheme by mutableStateOf(false)
        private set

    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    private val cloudSessionStore = CloudSessionStore(app)
    private var cloudSession: CloudSession? = cloudSessionStore.load()
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

    init {
        loadLinks()
        loadTheme()
        loadSettings()
        loadDailyTraffic()
        nextId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
        TrafficNotificationCommandBus.register(this)
        if (keepAliveEnabled) syncKeepAliveService()
        schedulePublicJsonBackup()
    }

    fun updateThreadCount(value: Int) {
        threadCount = value.coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
        if (!isRunning) {
            activeThreadCount = threadCount
        }
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

    fun loginCloudAccount(apiBaseUrl: String, username: String, password: String) = authenticateCloudAccount {
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
                if (links.isEmpty()) {
                    runCatching { CloudApi.downloadProfileSnapshot(session) }
                        .getOrNull()
                        ?.let(::restoreBackupDocument)
                }
                cloudStatus = "登录成功"
                refreshCloudStats("day", isManual = false)
                startAutoCloudSync()
            } catch (error: Throwable) {
                cloudStatus = error.message ?: "登录失败"
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

        val quickTargetBytes = (QUICK_TRAFFIC_TARGET_GB_TEXT.toDouble() * BYTES_PER_GB).toLong()
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
            initialStatusText = "一键测流开始，共 ${parsed.size} 个链接，每条 ${QUICK_TRAFFIC_TARGET_GB_TEXT}GB，线程数 $threadCount",
        )
        return true
    }

    fun start() {
        if (isRunning) return

        val parsed = parseTasks()
        if (parsed.isEmpty()) {
            statusText = if (hasEnabledValidLinks()) {
                "所选链接已完成，可修改目标后重跑"
            } else {
                "请在管理页勾选并填写有效链接（URL + 目标GB）"
            }
            return
        }

        startParsedTasks(
            parsed = parsed,
            initialStatusText = "开始/继续执行，共 ${parsed.size} 个链接，线程数 $threadCount",
        )
    }

    private fun startParsedTasks(parsed: List<ParsedTask>, initialStatusText: String) {
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

        runJob = viewModelScope.launch(Dispatchers.IO) {
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
                    withContext(Dispatchers.Main) {
                        links = links.map {
                            if (it.id == task.id) it.copy(status = "完成", consumedBytes = task.targetBytes) else it
                        }
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        links = links.map {
                            if (it.id == task.id) it.copy(status = "失败") else it
                        }
                        statusText = "链接失败：${e.message ?: "未知错误"}"
                        isRunning = false
                        stopAutoCloudSync()
                        currentSpeedBytesPerSec = 0L
                        syncKeepAliveService()
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                isRunning = false
                stopAutoCloudSync()
                currentSpeedBytesPerSec = 0L
                syncKeepAliveService()
                statusText = when {
                    links.any { it.status == "失败" } -> "执行结束，存在失败链接"
                    links.any { it.status == "已停止" } -> "已停止"
                    else -> "全部完成"
                }
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
            val targetBytes = if (gb == 0.0) 0L else (gb * BYTES_PER_GB).toLong()
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

    private suspend fun probeRangeSupport(url: String, userAgent: String?): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
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
        val (supportsRange, contentLength) = probeRangeSupport(task.url, task.userAgent)
        activeRangeChunkSizeBytes.set(chooseChunkSizeForContent(contentLength, 0L))
        val nextRangeStart = AtomicLong(0L)

        coroutineScope {
            val progressJob = launch(Dispatchers.Default) {
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
                    }

                    if (hasReachedTarget(snapshot, task.targetBytes)) break
                }
            }

            repeat(MAX_EFFECTIVE_CONCURRENCY) { index ->
                launch(Dispatchers.IO) {
                    val buffer = ByteArray(READ_BUFFER_SIZE)
                    val downloadClient = buildDownloadClient()
                    delay(index * WORKER_STAGGER_MS)
                    while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                        val desiredThreads = computeActiveThreadCount(smoothedSpeed, contentLength)
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
                                delay(FAST_RETRY_DELAY_MS)
                                continue
                            }
                        } else {
                                val rangeHeader = nextRangeHeader(supportsRange, contentLength, nextRangeStart)
                                openDownloadStream(task.url, rangeHeader, downloadClient, task.userAgent).use { streamResult ->
                                    if (streamResult.code !in 200..299) {
                                        val message = "HTTP ${streamResult.code}"
                                        firstError.compareAndSet(null, message)
                                        if (streamResult.code in 400..499) {
                                            throw FatalRequestException("请求失败：$message")
                                        }
                                        delay(FAST_RETRY_DELAY_MS)
                                        return@use
                                    }
                                    val stream = streamResult.inputStream ?: return@use
                                    while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                                        val read = stream.read(buffer)
                                        if (read <= 0) break
                                        if (hasReachedTarget(consumed.get(), task.targetBytes)) break
                                        addConsumedSafely(consumed, task.targetBytes, read.toLong())
                                        lastProgressAt.set(System.currentTimeMillis())
                                    }
                                }
                            }
                            if (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                                delay(FAST_RETRY_DELAY_MS)
                            }
                        } catch (e: FatalRequestException) {
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
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
                            delay(FAST_RETRY_DELAY_MS)
                        } catch (e: Exception) {
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
                            delay(FAST_RETRY_DELAY_MS)
                        }
                    }
                }
            }

            while (currentCoroutineContext().isActive && !hasReachedTarget(consumed.get(), task.targetBytes)) {
                val now = System.currentTimeMillis()
                if (now - lastProgressAt.get() > 15_000L) {
                    currentCoroutineContext().cancelChildren()
                    val reason = firstError.get()?.let { "，最近错误：$it" } ?: ""
                    throw IllegalStateException("15秒内无有效数据，请检查链接可用性$reason")
                }
                delay(100)
            }

            currentCoroutineContext().cancelChildren()
            progressJob.cancel()

            withContext(Dispatchers.Main) {
                val finalConsumed = if (isUnlimitedTarget(task.targetBytes)) consumed.get() else task.targetBytes
                currentTaskConsumed = finalConsumed
                currentSpeedBytesPerSec = 0L
                updateConsumed(task.id, finalConsumed)
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
        val installed = suspendCancellableCoroutine<Boolean> { cont ->
            CronetProviderInstaller.installProvider(app).addOnCompleteListener { task ->
                cont.resume(task.isSuccessful)
            }
        }
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

    private fun buildDownloadClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(1, 1, TimeUnit.SECONDS))
            .build()
    }

    private fun applyUserAgentHeader(builder: Request.Builder, userAgent: String?) {
        builder.header("User-Agent", userAgent.orEmpty())
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
                        setRequestProperty("User-Agent", userAgent.orEmpty())
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
        val document = createBackupDocument().toString()
        publicBackupWriteJob?.cancel()
        publicBackupWriteJob = viewModelScope.launch {
            delay(400)
            withContext(Dispatchers.IO) {
                PublicJsonBackup.write(getApplication(), document)
            }
        }
    }

    private fun loadLinks() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
        val raw = prefs.getString(PREF_LINKS, null) ?: return
        val array = JSONArray(raw)
        val loaded = mutableListOf<LinkItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            loaded += LinkItem(
                id = obj.optLong("id", i.toLong() + 1L),
                name = obj.optString("name", ""),
                url = obj.optString("url", ""),
                targetGbText = obj.optString("targetGbText", ""),
                userAgent = obj.optString("userAgent", ""),
                enabled = obj.optBoolean("enabled", true),
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
        persistDailyTraffic(today, dailyTrafficBytes, dailyTaskCount)
        cloudSession?.let { session ->
            deviceTrafficContributionStore.recordLocalTraffic(session, today, bytes, 0)
        }
    }

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

    private fun chooseChunkSizeForContent(contentLength: Long, speedBytesPerSec: Long): Long {
        val baseChunk = when {
            contentLength in 1..(2L * 1024L * 1024L) -> 128L * 1024L
            contentLength in 1..(16L * 1024L * 1024L) -> 512L * 1024L
            contentLength in 1..(128L * 1024L * 1024L) -> 2L * 1024L * 1024L
            contentLength in 1..(1024L * 1024L * 1024L) -> 4L * 1024L * 1024L
            contentLength in 1..(4L * 1024L * 1024L * 1024L) -> 8L * 1024L * 1024L
            contentLength > 0L -> 16L * 1024L * 1024L
            else -> DEFAULT_ADAPTIVE_CHUNK_SIZE_BYTES
        }
        val speedAdjustedChunk = when {
            speedBytesPerSec >= 160L * 1024L * 1024L -> max(baseChunk, 32L * 1024L * 1024L)
            speedBytesPerSec >= 60L * 1024L * 1024L -> max(baseChunk, 16L * 1024L * 1024L)
            else -> baseChunk
        }
        return speedAdjustedChunk.coerceIn(MIN_ADAPTIVE_CHUNK_SIZE_BYTES, MAX_ADAPTIVE_CHUNK_SIZE_BYTES)
    }

    private fun computeActiveThreadCount(speedBytesPerSec: Long, contentLength: Long): Int {
        val desired = threadCount.coerceIn(1, MAX_EFFECTIVE_CONCURRENCY)
        activeThreadCount = desired
        activeRangeChunkSizeBytes.set(chooseChunkSizeForContent(contentLength, speedBytesPerSec))
        return desired
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
        TrafficKeepAliveService.stop(getApplication())
        cronetEngine?.shutdown()
        cronetEngine = null
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

@Composable
fun TrafficScreen(vm: TrafficViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "网络面板X",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "批量流量任务 · 实时测速面板",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    )
                }
                IconButton(onClick = { vm.toggleTheme() }) {
                    Icon(
                        painter = painterResource(
                            id = if (vm.isDarkTheme) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
                        ),
                        contentDescription = if (vm.isDarkTheme) "切换到白天主题" else "切换到黑夜主题",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = page,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Tab(selected = page == 0, onClick = { page = 0 }, text = { Text("测速") })
            Tab(selected = page == 1, onClick = { page = 1 }, text = { Text("链接管理") })
            Tab(selected = page == 2, onClick = { page = 2 }, text = { Text("统计") })
            Tab(selected = page == 3, onClick = { page = 3 }, text = { Text("设置") })
        }

        when (page) {
            0 -> SpeedPanel(vm)
            1 -> LinkManagePanel(vm = vm, onQuickTrafficStarted = { page = 0 })
            2 -> StatsPanel(vm)
            else -> SettingsPanel(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SpeedPanel(vm: TrafficViewModel) {
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

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricCard("实时网速", "${formatBytes(vm.currentSpeedBytesPerSec)}/s")
                        MetricCard("总进度", if (hasFiniteTotalTarget) "${"%.2f".format(progress * 100f)}%" else "无限")
                        MetricCard("预计剩余", if (etaSec >= 0L) formatDuration(etaSec) else "-")
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
private fun SettingsPanel(vm: TrafficViewModel) {
    val context = LocalContext.current
    var savedUserAgentName by rememberSaveable { mutableStateOf("") }
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
            CloudAccountPanel(vm)
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
private fun BackupPanel(vm: TrafficViewModel) {
    val saved = vm.backupConnection
    var provider by rememberSaveable { mutableStateOf(saved.provider) }
    var webDavUrl by rememberSaveable { mutableStateOf(saved.webDavUrl) }
    var webDavUsername by rememberSaveable { mutableStateOf(saved.webDavUsername) }
    var webDavPassword by rememberSaveable { mutableStateOf(saved.webDavPassword) }
    var s3Endpoint by rememberSaveable { mutableStateOf(saved.s3Endpoint) }
    var s3Region by rememberSaveable { mutableStateOf(saved.s3Region) }
    var s3Bucket by rememberSaveable { mutableStateOf(saved.s3Bucket) }
    var s3AccessKey by rememberSaveable { mutableStateOf(saved.s3AccessKey) }
    var s3SecretKey by rememberSaveable { mutableStateOf(saved.s3SecretKey) }
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

private fun formatBackupTime(epochMs: Long): String {
    if (epochMs <= 0L) return "时间未知"
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloudAccountPanel(vm: TrafficViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                        "login" -> Button(onClick = { vm.loginCloudAccount(DEFAULT_CLOUD_API_URL, username, password) }) { Text("登录") }
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
private fun StatsPanel(vm: TrafficViewModel) {
    var period by rememberSaveable { mutableStateOf("day") }
    val labels = mapOf("day" to "日", "month" to "月", "year" to "年")
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
                            FilterChip(selected = period == key, onClick = { period = key }, label = { Text("${label}榜") })
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
private fun BackgroundSettingsPanel(
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
private fun UpdatePanel(vm: TrafficViewModel, context: Context) {
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

private fun openExternalLink(context: Context, url: String) {
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
private fun UserAgentDropdown(
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
private fun MetricCard(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
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
private fun LinkManagePanel(
    vm: TrafficViewModel,
    onQuickTrafficStarted: () -> Unit,
) {
    val expandedIds = rememberSaveable { mutableStateOf(setOf<Long>()) }
    val localJsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(vm::importLocalJsonBackup)
    }

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
                    Button(onClick = { localJsonImportLauncher.launch(arrayOf("application/json", "text/plain")) }, enabled = !vm.isRunning) {
                        Text("导入本地 JSON")
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
                Text("已勾选：${vm.links.count { it.enabled }} / ${vm.links.size}，一键测流每条 0.1GB")
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
                                label = { Text("目标流量 (GB，0=无限)") },
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
                            ?.let { (it * BYTES_PER_GB).toLong() } ?: 0L
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

private fun formatTargetBytes(bytes: Long): String {
    return if (isUnlimitedTarget(bytes)) "无限" else formatBytes(bytes)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun formatDuration(totalSec: Long): String {
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




