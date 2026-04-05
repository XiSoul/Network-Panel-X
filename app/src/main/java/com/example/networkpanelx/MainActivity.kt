package com.example.networkpanelx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

private const val PREFS_NAME = "traffic_prefs"
private const val PREF_LINKS = "links_json"
private const val PREF_THEME_DARK = "theme_dark"
private const val TEST_CHUNK_SIZE_BYTES = 8L * 1024L * 1024L
private const val READ_BUFFER_SIZE = 256 * 1024

data class LinkItem(
    val id: Long,
    val name: String = "",
    val url: String = "",
    val targetGbText: String = "",
    val enabled: Boolean = true,
    val consumedBytes: Long = 0L,
    val status: String = "未开始",
)

data class ParsedTask(
    val id: Long,
    val name: String,
    val url: String,
    val targetBytes: Long,
    val initialConsumedBytes: Long,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TrafficViewModel = viewModel(factory = TrafficViewModel.Factory(application))
            MaterialTheme(
                colorScheme = if (vm.isDarkTheme) {
                    darkColorScheme(
                        background = Color(0xFF121212),
                        surface = Color(0xFF1E1E1E),
                    )
                } else {
                    lightColorScheme(
                        background = Color(0xFFF5F5F5),
                        surface = Color.White,
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
}

class TrafficViewModel(app: Application) : AndroidViewModel(app) {
    var links by mutableStateOf(emptyList<LinkItem>())
        private set

    var isRunning by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("在管理页配置链接后开始测速")
        private set

    var threadCount by mutableIntStateOf(8)
        private set
    var keepAliveEnabled by mutableStateOf(false)
        private set
    var isDarkTheme by mutableStateOf(false)
        private set

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
    private var activeRunTargets: Map<Long, Long> = emptyMap()
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

    init {
        loadLinks()
        loadTheme()
        nextId = (links.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    fun updateThreadCount(value: Int) {
        if (isRunning) return
        threadCount = value.coerceIn(1, 64)
    }

    fun toggleKeepAlive() {
        keepAliveEnabled = !keepAliveEnabled
        syncKeepAliveService()
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
        statusText = "已暂停"
        currentSpeedBytesPerSec = 0L
        syncKeepAliveService()
        links = links.map {
            if (it.status == "运行中") it.copy(status = "已停止") else it
        }
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

        activeRunTargets = parsed.associate { it.id to it.targetBytes }
        totalTargetBytes = parsed.sumOf { it.targetBytes }
        totalConsumedBytes = parsed.sumOf { minOf(it.initialConsumedBytes, it.targetBytes) }
        currentTaskName = "-"
        currentTaskConsumed = 0L
        currentTaskTarget = 0L
        currentSpeedBytesPerSec = 0L

        links = links.map {
            if (it.id in activeRunTargets) {
                val target = activeRunTargets[it.id] ?: 0L
                val status = if (it.consumedBytes >= target && target > 0L) "完成" else "未开始"
                it.copy(status = status)
            } else {
                it
            }
        }
        isRunning = true
        statusText = "开始/继续执行，共 ${parsed.size} 个链接，线程数 $threadCount"
        syncKeepAliveService()

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
                    consumeTask(task, threadCount)
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
                        currentSpeedBytesPerSec = 0L
                        syncKeepAliveService()
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                isRunning = false
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
            if (url.isEmpty() || gb == null || gb <= 0.0) continue
            val targetBytes = (gb * 1024.0 * 1024.0 * 1024.0).toLong()
            if (targetBytes <= 0L) continue
            val initialConsumed = item.consumedBytes.coerceAtMost(targetBytes)
            if (initialConsumed >= targetBytes) continue
            tasks += ParsedTask(
                id = item.id,
                name = name,
                url = url,
                targetBytes = targetBytes,
                initialConsumedBytes = initialConsumed,
            )
        }
        return tasks
    }

    private fun hasEnabledValidLinks(): Boolean {
        return links.any { item ->
            if (!item.enabled) return@any false
            val url = item.url.trim()
            val gb = item.targetGbText.trim().toDoubleOrNull()
            url.isNotEmpty() && gb != null && gb > 0.0
        }
    }

    private suspend fun consumeTask(task: ParsedTask, threads: Int) {
        val consumed = AtomicLong(task.initialConsumedBytes)
        var lastBytes = task.initialConsumedBytes
        var lastTime = System.currentTimeMillis()
        val lastProgressAt = AtomicLong(System.currentTimeMillis())
        val firstError = AtomicReference<String?>(null)
        val endpoint = probeEndpoint(task.url)

        coroutineScope {
            val progressJob = launch(Dispatchers.Default) {
                while (currentCoroutineContext().isActive) {
                    delay(500)
                    val now = System.currentTimeMillis()
                    val snapshot = consumed.get().coerceAtMost(task.targetBytes)
                    val dt = max(1L, now - lastTime)
                    val delta = (snapshot - lastBytes).coerceAtLeast(0L)
                    val speed = (delta * 1000L) / dt
                    lastBytes = snapshot
                    lastTime = now

                    withContext(Dispatchers.Main) {
                        currentTaskConsumed = snapshot
                        currentSpeedBytesPerSec = speed
                        updateConsumed(task.id, snapshot)
                    }

                    if (snapshot >= task.targetBytes) break
                }
            }

            repeat(threads.coerceIn(1, 64)) {
                launch(Dispatchers.IO) {
                    val buffer = ByteArray(READ_BUFFER_SIZE)
                    while (currentCoroutineContext().isActive && consumed.get() < task.targetBytes) {
                        val requestBuilder = Request.Builder()
                            .url(task.url)
                            .get()
                            .header("Cache-Control", "no-cache")
                            .header("Pragma", "no-cache")
                            .header("Accept-Encoding", "identity")
                            .header("Connection", "keep-alive")
                            .header("User-Agent", "Mozilla/5.0 (Android)")

                        if (endpoint.supportsRange && endpoint.contentLength > 0L) {
                            val maxStart = (endpoint.contentLength - TEST_CHUNK_SIZE_BYTES).coerceAtLeast(0L)
                            val start = if (maxStart > 0L) Random.nextLong(maxStart + 1L) else 0L
                            val end = minOf(endpoint.contentLength - 1L, start + TEST_CHUNK_SIZE_BYTES - 1L)
                            requestBuilder.header("Range", "bytes=$start-$end")
                        }

                        try {
                            val call = httpClient.newCall(requestBuilder.build())
                            call.execute().use { resp ->
                                if (!resp.isSuccessful) {
                                    val message = "HTTP ${resp.code}"
                                    firstError.compareAndSet(null, message)
                                    if (resp.code in 400..499) {
                                        currentCoroutineContext().cancelChildren()
                                        throw IllegalStateException("请求失败：$message")
                                    }
                                    delay(120)
                                    return@use
                                }
                                val body = resp.body ?: return@use
                                val stream = body.byteStream()
                                while (currentCoroutineContext().isActive && consumed.get() < task.targetBytes) {
                                    val read = stream.read(buffer)
                                    if (read <= 0) break
                                    if (consumed.get() >= task.targetBytes) break
                                    addConsumedSafely(consumed, task.targetBytes, read.toLong())
                                    lastProgressAt.set(System.currentTimeMillis())
                                }
                            }
                        } catch (e: Exception) {
                            firstError.compareAndSet(null, e.message ?: e.javaClass.simpleName)
                            delay(120)
                        }
                    }
                }
            }

            while (currentCoroutineContext().isActive && consumed.get() < task.targetBytes) {
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
                currentTaskConsumed = task.targetBytes
                currentSpeedBytesPerSec = 0L
                updateConsumed(task.id, task.targetBytes)
            }
        }
    }

    private fun probeEndpoint(url: String): EndpointInfo {
        return try {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("Accept-Encoding", "identity")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val len = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                val range = resp.header("Accept-Ranges").orEmpty().contains("bytes", ignoreCase = true)
                EndpointInfo(contentLength = len, supportsRange = range)
            }
        } catch (_: Exception) {
            EndpointInfo(contentLength = -1L, supportsRange = false)
        }
    }

    private fun addConsumedSafely(counter: AtomicLong, target: Long, incoming: Long) {
        while (true) {
            val current = counter.get()
            if (current >= target) return
            val accepted = minOf(incoming, target - current)
            if (counter.compareAndSet(current, current + accepted)) return
        }
    }

    private fun updateConsumed(taskId: Long, taskConsumed: Long) {
        links = links.map {
            if (it.id == taskId) it.copy(consumedBytes = taskConsumed) else it
        }
        totalConsumedBytes = links.sumOf { item ->
            val target = activeRunTargets[item.id] ?: return@sumOf 0L
            minOf(item.consumedBytes, target)
        }
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
                    put("enabled", item.enabled)
                }
            )
        }
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putString(PREF_LINKS, array.toString())
            .apply()
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

    private fun syncKeepAliveService() {
        val app = getApplication<Application>()
        if (keepAliveEnabled && isRunning) {
            TrafficKeepAliveService.start(app)
        } else {
            TrafficKeepAliveService.stop(app)
        }
    }

    override fun onCleared() {
        super.onCleared()
        TrafficKeepAliveService.stop(getApplication())
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "网络面板X", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { vm.toggleTheme() }) {
                Icon(
                    painter = painterResource(
                        id = if (vm.isDarkTheme) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
                    ),
                    contentDescription = if (vm.isDarkTheme) "切换到白天主题" else "切换到黑夜主题",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        TabRow(selectedTabIndex = page) {
            Tab(selected = page == 0, onClick = { page = 0 }, text = { Text("测速") })
            Tab(selected = page == 1, onClick = { page = 1 }, text = { Text("链接管理") })
        }

        if (page == 0) {
            SpeedPanel(vm)
        } else {
            LinkManagePanel(vm)
        }
    }
}

@Composable
private fun SpeedPanel(vm: TrafficViewModel) {
    val progress = if (vm.totalTargetBytes > 0L) {
        (vm.totalConsumedBytes.toFloat() / vm.totalTargetBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remain = (vm.totalTargetBytes - vm.totalConsumedBytes).coerceAtLeast(0L)
    val etaSec = if (vm.currentSpeedBytesPerSec > 0L) remain / vm.currentSpeedBytesPerSec else -1L

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("状态：${vm.statusText}")

        Text("线程数：${vm.threadCount}")
        Slider(
            value = vm.threadCount.toFloat(),
            onValueChange = { vm.updateThreadCount(it.toInt()) },
            enabled = !vm.isRunning,
            valueRange = 1f..64f,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("当前链接：${vm.currentTaskName}")
            Text("实时网速：${formatBytes(vm.currentSpeedBytesPerSec)}/s")
            Text("当前链接消耗：${formatBytes(vm.currentTaskConsumed)} / ${formatBytes(vm.currentTaskTarget)}")
            Text("总流量消耗：${formatBytes(vm.totalConsumedBytes)} / ${formatBytes(vm.totalTargetBytes)}")
            Text("总进度：${"%.2f".format(progress * 100f)}%")
            Text("预计剩余：${if (etaSec >= 0L) formatDuration(etaSec) else "-"}")
        }

        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.start() }, enabled = !vm.isRunning) {
                Text("开始")
            }
            Button(onClick = { vm.stop() }, enabled = vm.isRunning) {
                Text("停止")
            }
            Button(onClick = { vm.toggleKeepAlive() }) {
                Text(if (vm.keepAliveEnabled) "后台常驻: 开" else "后台常驻: 关")
            }
        }

        Text("本测试工具仅提供网络速度自查，请勿用于非法用途，使用本工具造成的一切后果由用户承担！")
        Spacer(modifier = Modifier.weight(1f))
        Text("更新QQ群：1074735930", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("版本号：1.0.0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

private data class EndpointInfo(
    val contentLength: Long,
    val supportsRange: Boolean,
)

@Composable
private fun SystemBarsEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as Activity).window
        val color = if (isDarkTheme) android.graphics.Color.parseColor("#121212") else android.graphics.Color.WHITE
        window.statusBarColor = color
        window.navigationBarColor = color
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme
    }
}

@Composable
private fun LinkManagePanel(vm: TrafficViewModel) {
    val expandedIds = rememberSaveable { mutableStateOf(setOf<Long>()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { vm.addLink() }, enabled = !vm.isRunning) {
                    Text("新增链接")
                }
                Text("已勾选：${vm.links.count { it.enabled }} / ${vm.links.size}")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.links, key = { item -> item.id }) { item ->
                val index = vm.links.indexOfFirst { it.id == item.id }
                val expanded = item.id in expandedIds.value
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                label = { Text("目标流量 (GB)") },
                                enabled = !vm.isRunning,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }

                        val targetBytes = item.targetGbText.toDoubleOrNull()
                            ?.takeIf { it > 0.0 }
                            ?.let { (it * 1024.0 * 1024.0 * 1024.0).toLong() } ?: 0L
                        val remainBytes = (targetBytes - item.consumedBytes).coerceAtLeast(0L)
                        Text("本次消耗：${formatBytes(item.consumedBytes)}，目标剩余：${formatBytes(remainBytes)}")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
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
        String.format("%d小时%02d分%02d秒", h, m, s)
    } else {
        String.format("%02d分%02d秒", m, s)
    }
}




