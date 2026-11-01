package com.example.networkpanelx

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 新 UI 主屏（2026-10-25 定稿渲染图落地）：
 * - 顶部 header：应用名+副题 / 状态药丸(呼吸点)+主题切换按钮
 * - 当前速率：超大数字仪表（数值+橙色单位）
 * - 三联指标：线程数 / 总消耗 / 预计剩余（竖线分隔）
 * - 量油尺总进度：带刻度+高亮端边
 * - 并发线程滑块：刻度 1/16/32/48/64，标注"停止后可调"
 * - 链接任务列表：状态圆点+名称+进度+右侧速度
 * - 控制区：后台常驻开关 + 开始/停止 大按钮
 * - 底部 tabbar：4 标签（测速/链接管理/统计/设置）
 * - 免责声明 / QQ群 / 版本号
 */

@Composable
fun TrafficScreen(vm: TrafficViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NpxPalette.bg),
    ) {
        // ============ 顶部 header ============
        NpxHeader(
            isRunning = vm.isRunning,
            statusText = vm.statusText,
            isDark = vm.isDarkTheme,
            onToggleTheme = { vm.toggleTheme() },
        )

        // ============ 页面内容 ============
        Box(modifier = Modifier.weight(1f)) {
            when (page) {
                0 -> NewSpeedPanel(vm)
                1 -> NewLinkManagePanel(vm = vm, onQuickTrafficStarted = { page = 0 })
                2 -> NewStatsPanel(vm)
                else -> NewSettingsPanel(vm)
            }
        }

        // ============ 底部 tabbar ============
        NpxTabBar(selected = page, onSelect = { page = it })

        // ============ 免责 / QQ / 版本 ============
        NpxFooter()
    }
}

@Composable
private fun NpxHeader(isRunning: Boolean, statusText: String, isDark: Boolean, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "网络面板X",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp,
                color = NpxPalette.text,
            )
            Text(
                text = "批量流量任务 · 实时测速面板",
                fontSize = 11.sp,
                color = NpxPalette.textGhost,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 状态药丸（呼吸点）
            StatusPill(isRunning = isRunning, statusText = statusText)
            Spacer(modifier = Modifier.width(14.dp))
            // 主题切换按钮
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .border(1.dp, NpxPalette.divider, RoundedCornerShape(5.dp))
                    .clickable(onClick = onToggleTheme),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isDark) "☀" else "☾",
                    fontSize = 15.sp,
                    color = NpxPalette.textFaint,
                )
            }
        }
    }
    // header 底部分隔线
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NpxPalette.divider),
    )
}

@Composable
private fun StatusPill(isRunning: Boolean, statusText: String) {
    // 状态文案与配色，语义与链接列表圆点保持一致：
    // 运行中=蓝、待开始=黄、完成=绿、失败=红、已停止=灰
    val (label, pillColor) = when {
        isRunning -> "运行中" to NpxPalette.runningBlue
        statusText.contains("失败") -> "有失败" to NpxPalette.failRed
        statusText.contains("全部完成") || statusText.contains("完成") -> "已完成" to NpxPalette.doneGreen
        statusText.contains("已停止") -> "已停止" to NpxPalette.idleGray
        else -> "待开始" to NpxPalette.waitingYellow
    }
    // 呼吸动画
    val transition = rememberInfiniteTransition(label = "pill")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 0.35f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(if (isRunning) alpha else 1f)
                .background(pillColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = pillColor,
        )
    }
}

// ============ 测速主页（新版） ============

@Composable
private fun NewSpeedPanel(vm: TrafficViewModel) {
    val context = LocalContext.current
    val qqGroup = "1074735930"
    val hasFiniteTotalTarget = vm.totalTargetBytes > 0L
    val progress = if (hasFiniteTotalTarget) {
        (vm.totalConsumedBytes.toFloat() / vm.totalTargetBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val remain = if (hasFiniteTotalTarget) (vm.totalTargetBytes - vm.totalConsumedBytes).coerceAtLeast(0L) else 0L
    val etaSec = if (hasFiniteTotalTarget && vm.currentSpeedBytesPerSec > 0L) remain / vm.currentSpeedBytesPerSec else -1L

    // 速率拆数值+单位
    val speedText = formatBytes(vm.currentSpeedBytesPerSec)
    val (speedNum, speedUnit) = splitSpeedValue(speedText)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ---- 当前速率仪表 ----
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("当前速率", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NpxPalette.textGhost)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = speedNum,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 76.sp,
                        color = NpxPalette.text,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = speedUnit,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NpxPalette.accent,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Text(
                    text = "状态 · ${vm.statusText}",
                    fontSize = 12.sp,
                    color = NpxPalette.textDim,
                )
                if (vm.lastErrorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠ ${vm.lastErrorText}",
                        fontSize = 12.sp,
                        color = NpxPalette.failRed,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ---- 三联指标 ----
        item {
            Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                TripleCell(
                    label = "线程数",
                    value = "${vm.activeThreadCount}",
                    modifier = Modifier.weight(1f),
                    divider = true,
                )
                TripleCell(
                    label = "总消耗",
                    value = formatBytes(vm.totalConsumedBytes),
                    modifier = Modifier.weight(1f),
                    divider = true,
                )
                TripleCell(
                    label = "预计剩余",
                    value = if (etaSec >= 0L) formatDuration(etaSec) else "-",
                    modifier = Modifier.weight(1f),
                    accent = true,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // ---- 量油尺总进度 ----
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("总进度", fontSize = 11.sp, color = NpxPalette.textGhost)
                    Text(
                        if (hasFiniteTotalTarget)
                            "${formatBytes(vm.totalConsumedBytes)} / ${formatBytes(vm.totalTargetBytes)}"
                        else "${formatBytes(vm.totalConsumedBytes)} · 无上限",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NpxPalette.textDim,
                    )
                    Text(
                        if (hasFiniteTotalTarget) "${(progress * 100).toInt()}%" else "∞",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpxPalette.accent,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                DipstickProgress(progress = progress)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ---- 并发线程滑块 ----
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NpxPalette.dividerSoft),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "并发线程 · ${if (vm.isRunning) "运行中可调" else "可调"}",
                        fontSize = 12.sp,
                        color = NpxPalette.textGhost,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${vm.threadCount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NpxPalette.accent,
                        )
                        Text(
                            text = "/64",
                            fontSize = 11.sp,
                            color = NpxPalette.textGhost,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                Slider(
                    value = vm.threadCount.toFloat(),
                    onValueChange = { vm.updateThreadCount(it.toInt()) },
                    // 运行中也可调：worker 循环每轮都会重算 desiredThreads，
                    // 调大立刻有新 worker 加入，调小则超出部分的 worker 自动退出。
                    enabled = true,
                    valueRange = 1f..64f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("1", "16", "32", "48", "64").forEach {
                        Text(it, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = NpxPalette.textMuted)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ---- 控制区：后台常驻 + 开始/停止 ----
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NpxPalette.dividerSoft),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "后台常驻",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NpxPalette.textGhost,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (vm.keepAliveEnabled) "开" else "关",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NpxPalette.accent,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NpxToggle(
                        checked = vm.keepAliveEnabled,
                        onCheckedChange = { vm.updateKeepAliveEnabled(it) },
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 开始（禁用态）/ 停止（主操作）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (vm.isRunning) NpxPalette.surfaceAlt else NpxPalette.accent)
                            .clickable(enabled = !vm.isRunning) { vm.start() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "开始",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (vm.isRunning) Color(0xFF6B675F) else NpxPalette.onAccent,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (vm.isRunning) NpxPalette.accent else NpxPalette.surfaceAlt)
                            .clickable(enabled = vm.isRunning) { vm.stop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "停止任务",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (vm.isRunning) NpxPalette.onAccent else Color(0xFF6B675F),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ---- 链接任务列表 ----
        // 排序（2026-10-30 用户要求）：勾选的排前面，同组内保持原有顺序（即链接本身顺序）。
        // 这样刷第 4/5/6 条时，它们会显示在最上面，不用往下翻。
        val orderedLinks = vm.links.filter { it.enabled } + vm.links.filter { !it.enabled }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NpxPalette.dividerSoft),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("链接任务", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NpxPalette.textMid)
                val doneCount = vm.links.count { it.status == "完成" }
                Text(
                    "$doneCount / ${vm.links.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NpxPalette.textGhost,
                )
            }
        }
        items(orderedLinks) { link ->
            CircuitRow(
                name = link.name.ifBlank { "未命名链接" },
                meta = when (link.status) {
                    "完成" -> "${formatBytes(link.consumedBytes)} / ${formatTargetBytes(if (link.targetGbText.toDoubleOrNull() != null && link.targetGbText.toDouble() > 0) (link.targetGbText.toDouble() * bytesPerGigabyte()).toLong() else 0L)} · 已完成"
                    "运行中" -> "${formatBytes(link.consumedBytes)} / ${formatTargetBytes(if (link.targetGbText.toDoubleOrNull() != null && link.targetGbText.toDouble() > 0) (link.targetGbText.toDouble() * bytesPerGigabyte()).toLong() else 0L)}"
                    "失败" -> "失败 · ${formatBytes(link.consumedBytes)}"
                    "已停止" -> "已停止 · ${formatBytes(link.consumedBytes)}"
                    else -> if (link.enabled) "等待中 · ${formatBytes(link.consumedBytes)}" else "未勾选"
                },
                status = link.status,
                isActive = link.status == "运行中",
                speedText = if (link.status == "运行中") speedText else null,
                enabled = link.enabled,
            )
        }
        if (vm.links.isEmpty()) {
            item {
                Text(
                    "暂无链接 · 到「链接管理」页添加",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = NpxPalette.textGhost,
                )
            }
        }

        // ---- 公网 IP 卡片（保留功能） ----
        item { PublicIpCard(vm) }

        // ---- QQ 群（免责+版本移到全局 Footer） ----
        item {
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NpxPalette.accent,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** 速率文本拆数值和单位（"1.84 MB" -> "1.84" / "MB/s"） */
private fun splitSpeedValue(speedText: String): Pair<String, String> {
    val m = Regex("^([\\d.]+)\\s*(.+)$").find(speedText.trim())
    return if (m != null) {
        m.groupValues[1] to "${m.groupValues[2]}/s"
    } else {
        speedText to ""
    }
}

@Composable
private fun TripleCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier.then(
            if (divider) Modifier.padding(end = 16.dp) else Modifier.padding(start = 16.dp)
        ),
    ) {
        Text(label, fontSize = 11.sp, color = NpxPalette.textGhost)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) NpxPalette.accent else NpxPalette.textMid,
        )
    }
    // 竖分隔线（跟在 cell 后面）
    if (divider) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(NpxPalette.divider),
        )
    }
}

/** 量油尺进度条：刻度 + 橙色填充 + 亮端边 */
@Composable
private fun DipstickProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(NpxPalette.surface),
    ) {
        // 刻度（10 格）
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(10) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .border(
                            width = if (i < 9) 1.dp else 0.dp,
                            color = NpxPalette.tickLine,
                        ),
                )
            }
        }
        // 填充 + 亮端边
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(NpxPalette.accent.copy(alpha = 0.92f)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxSize()
                    .background(NpxPalette.accentBright),
            )
        }
    }
}

/** 链接任务行 */
@Composable
private fun CircuitRow(
    name: String,
    meta: String,
    status: String,
    isActive: Boolean,
    speedText: String?,
    enabled: Boolean = true,
) {
    // 状态点语义色（2026-10-30 用户要求）：
    // 运行中=蓝、勾选等待=黄、完成=绿、失败/网络不通=红、未勾选=灰
    val dotColor = when {
        !enabled -> NpxPalette.idleGray
        status == "运行中" -> NpxPalette.runningBlue
        status == "完成" -> NpxPalette.doneGreen
        status == "失败" -> NpxPalette.failRed
        status == "已停止" -> NpxPalette.idleGray
        else -> NpxPalette.waitingYellow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 圆点带同色光晕，暗色/亮色主题下都足够显眼
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(dotColor.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(9.dp).background(dotColor, CircleShape))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (!enabled) NpxPalette.textGhost else NpxPalette.textMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(meta, fontSize = 11.sp, color = NpxPalette.textGhost)
        }
        Text(
            speedText ?: "—",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) NpxPalette.runningBlue else NpxPalette.textDim,
        )
    }
}

/** 自绘 toggle 开关（设计稿样式） */
@Composable
fun NpxToggle(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(21.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) NpxPalette.accent else NpxPalette.surfaceAlt)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(17.dp)
                .background(if (checked) NpxPalette.bg else NpxPalette.textFaint, CircleShape),
        )
    }
}

/** 底部 tabbar（4 标签，选中项上方短橙线） */
@Composable
private fun NpxTabBar(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpxPalette.divider),
        )
        Row(modifier = Modifier.fillMaxWidth().background(NpxPalette.bgDeep)) {
            listOf("测速", "链接管理", "统计", "设置").forEachIndexed { i, label ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) }
                        .padding(top = 9.dp, bottom = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.dp)
                            .background(if (selected == i) NpxPalette.accent else Color.Transparent),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected == i) NpxPalette.text else NpxPalette.textGhost,
                    )
                }
            }
        }
    }
}

/** 全局页脚：免责声明 + 版本 */
@Composable
private fun NpxFooter() {
    Column(modifier = Modifier.background(NpxPalette.bg)) {
        Text(
            "本测试工具仅提供网络速度自查，请勿用于非法用途，使用本工具造成的一切后果由用户承担！",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = NpxPalette.textMuted,
        )
        Text(
            "版本号：${BuildConfig.VERSION_NAME}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = NpxPalette.textGhost2,
        )
    }
}
