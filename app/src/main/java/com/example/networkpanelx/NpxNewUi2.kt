package com.example.networkpanelx

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 新 UI 第二批（2026-10-25 定稿渲染图落地）：
 * - NewLinkManagePanel：链接队列（页头+新增按钮 / 序号+自绘勾选+行内操作 / 展开编辑区）
 * - NewStatsPanel：流量统计排行（分段切换器+排行列表）
 * - NewSettingsPanel：分组面板（云端账号卡 / UA / 备份 / 后台运行 / 更新）
 * 逻辑全部复用旧 Panel 的 VM 调用与子组件，仅重排视觉。
 */

// ============================================================
// 链接管理（新版）
// ============================================================

@Composable
fun NewLinkManagePanel(
    vm: TrafficViewModel,
    onQuickTrafficStarted: () -> Unit,
) {
    val expandedIds = rememberSaveable { mutableStateOf(setOf<Long>()) }
    val enabledCount = vm.links.count { it.enabled }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- 页头：链接队列 + 新增 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("链接队列", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NpxPalette.textMid)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已勾选 ", fontSize = 11.sp, color = NpxPalette.textGhost)
                    Text("$enabledCount", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NpxPalette.accent)
                    Text(" / ${vm.links.size} · 按序执行", fontSize = 11.sp, color = NpxPalette.textGhost)
                }
            }
            // 新增链接（橙色描边按钮）
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.dp, NpxPalette.accent, RoundedCornerShape(5.dp))
                    .clickable(enabled = !vm.isRunning) { vm.addLink() }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NpxPalette.accent)
                Spacer(modifier = Modifier.width(7.dp))
                Text("新增链接", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NpxPalette.accent)
            }
        }

        // ---- 快捷操作行（一键测流/暂停，保留功能）----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickChip(
                text = "一键测流",
                enabled = !vm.isRunning && vm.links.any { it.enabled },
                primary = true,
            ) {
                if (vm.quickStartSelectedTraffic()) onQuickTrafficStarted()
            }
            QuickChip(
                text = "一键暂停",
                enabled = vm.isRunning,
                primary = false,
            ) { vm.stop() }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "每条 0.1GB",
                fontSize = 10.sp,
                color = NpxPalette.textGhost,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // ---- 队列列表 ----
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(vm.links, key = { it.id }) { item ->
                val index = vm.links.indexOfFirst { it.id == item.id }
                QueueItem(
                    index = index,
                    item = item,
                    expanded = item.id in expandedIds.value,
                    locked = vm.isRunning,
                    onToggleEnabled = { vm.toggleLinkEnabled(item.id, it) },
                    onToggleExpand = {
                        expandedIds.value = if (item.id in expandedIds.value) {
                            expandedIds.value - item.id
                        } else {
                            expandedIds.value + item.id
                        }
                    },
                    onUp = { vm.moveLinkUp(item.id) },
                    onDown = { vm.moveLinkDown(item.id) },
                    onDelete = { vm.removeLink(item.id) },
                    onNameChange = { vm.updateLinkName(item.id, it) },
                    onUrlChange = { vm.updateLinkUrl(item.id, it) },
                    onTargetChange = { vm.updateLinkTargetGb(item.id, it) },
                    userAgentOptions = vm.userAgentOptions,
                    userAgentValue = item.userAgent,
                    onUserAgentChange = { vm.updateLinkUserAgent(item.id, it) },
                )
            }
            item {
                if (vm.links.isEmpty()) {
                    Text(
                        "队列为空 · 点右上角「+ 新增链接」",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 11.sp,
                        color = NpxPalette.textGhost,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/** 快捷操作小胶囊按钮 */
@Composable
private fun QuickChip(
    text: String,
    enabled: Boolean = true,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                when {
                    !enabled -> NpxPalette.surfaceAlt
                    primary -> NpxPalette.accent
                    else -> Color.Transparent
                }
            )
            .then(
                if (enabled && !primary) {
                    Modifier.border(1.dp, NpxPalette.textFaint, RoundedCornerShape(5.dp))
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                !enabled -> NpxPalette.textMuted
                primary -> NpxPalette.onAccent
                else -> NpxPalette.textFaint
            },
        )
    }
}

/** 队列条目：序号 + 勾选框 + 名称/域名 + 行内操作（+ 展开编辑区） */
@Composable
private fun QueueItem(
    index: Int,
    item: LinkItem,
    expanded: Boolean,
    locked: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleExpand: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    userAgentOptions: List<SavedUserAgent>,
    userAgentValue: String,
    onUserAgentChange: (String) -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(1.dp)
                .background(NpxPalette.dividerSoft),
        )
        // ---- 行 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号（01/02…）
            Text(
                "%02d".format(index + 1),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.enabled) NpxPalette.textDim else NpxPalette.textMuted,
                modifier = Modifier.width(28.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            // 自绘勾选框
            NpxCheckbox(
                checked = item.enabled,
                enabled = !locked,
                onCheckedChange = onToggleEnabled,
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 名称 + 域名/目标（点击展开）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleExpand),
            ) {
                Text(
                    item.name.trim().ifEmpty { "未命名链接" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.enabled) NpxPalette.textMid else NpxPalette.textGhost,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.enabled) null else TextDecoration.LineThrough,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${hostOf(item.url)} · ${item.targetGbText.ifBlank { "?" }} GB" +
                        if (item.status == "完成") " · 已完成" else "",
                    fontSize = 10.sp,
                    color = NpxPalette.textGhost,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            // 行内操作：上/下/删
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OpButton("↑", enabled = !locked && index > 0, onClick = onUp)
                OpButton("↓", enabled = !locked && index < 0 + Int.MAX_VALUE - 1, onClick = onDown)
                OpButton("×", enabled = !locked, danger = true, onClick = onDelete)
            }
        }
        // ---- 展开编辑区 ----
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 62.dp, end = 24.dp, top = 4.dp, bottom = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NpxPalette.dividerSoft.copy(alpha = 0.6f)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                NpxField(label = "链接名称", value = item.name, enabled = !locked, singleLine = true, onValueChange = onNameChange)
                Spacer(modifier = Modifier.height(10.dp))
                NpxField(label = "下载链接 URL", value = item.url, enabled = !locked, singleLine = true, onValueChange = onUrlChange)
                Spacer(modifier = Modifier.height(10.dp))
                NpxField(
                    label = "目标流量 (GB，0=无限)",
                    value = item.targetGbText,
                    enabled = !locked,
                    singleLine = true,
                    numeric = true,
                    onValueChange = onTargetChange,
                )
                Spacer(modifier = Modifier.height(10.dp))
                // UA 选择（简化为输入 + 已存选项横滚）
                NpxField(
                    label = "自定义 UA（留空继承全局）",
                    value = item.userAgent,
                    enabled = !locked,
                    singleLine = true,
                ) { onUserAgentChange(it) }
                if (userAgentOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        userAgentOptions.take(4).forEach { saved -> val ua = saved.value
                            Text(
                                saved.name.ifBlank { ua.take(12) }.ifBlank { "UA" },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(NpxPalette.surfaceAlt)
                                    .clickable(enabled = !locked) { onUserAgentChange(ua) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = if (userAgentValue == ua) NpxPalette.accent else NpxPalette.textFaint,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // 消耗读数
                val gbValue = item.targetGbText.toDoubleOrNull()
                val targetBytes = gbValue?.takeIf { it > 0.0 }?.let { (it * BYTES_PER_GB).toLong() } ?: 0L
                val remainText = if (gbValue == 0.0) "无限" else formatBytes((targetBytes - item.consumedBytes).coerceAtLeast(0L))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("上次运行消耗", fontSize = 11.sp, color = NpxPalette.textGhost)
                    Row {
                        Text(formatBytes(item.consumedBytes), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NpxPalette.textDim)
                        Text(" · 剩余 $remainText", fontSize = 11.sp, color = NpxPalette.accent)
                    }
                }
            }
        }
    }
}

/** 28x28 行内操作小按钮 */
@Composable
private fun OpButton(sym: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, NpxPalette.dividerSoft.copy(alpha = 1f), RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            sym,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> NpxPalette.textGhost2
                danger -> NpxPalette.failRed.copy(alpha = 0.8f)
                else -> NpxPalette.textFaint
            },
        )
    }
}

/** 自绘勾选框（设计稿：18dp 方框，勾选橙色填充+深色对勾） */
@Composable
fun NpxCheckbox(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.5.dp,
                color = if (checked) NpxPalette.accent else NpxPalette.textGhost2,
                shape = RoundedCornerShape(3.dp),
            )
            .background(if (checked) NpxPalette.accent else Color.Transparent, RoundedCornerShape(3.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                "✓",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NpxPalette.onAccent,
            )
        }
    }
}

/** 设计稿风格输入框：标签在上，深底圆角+底部 2px 线 */
@Composable
fun NpxField(
    label: String,
    value: String,
    enabled: Boolean,
    singleLine: Boolean,
    numeric: Boolean = false,
    password: Boolean = false,
    email: Boolean = false,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column {
        if (label.isNotBlank()) {
            Text(label, fontSize = 11.sp, color = NpxPalette.textGhost)
            Spacer(modifier = Modifier.height(5.dp))
        }
        val kbType = when {
            password -> KeyboardType.Password
            email -> KeyboardType.Email
            numeric -> KeyboardType.Decimal
            else -> KeyboardType.Text
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = NpxPalette.textMid,
                lineHeight = 18.sp,
            ),
            keyboardOptions = if (kbType == KeyboardType.Text) KeyboardOptions.Default else KeyboardOptions(keyboardType = kbType),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (minLines > 1) Modifier.heightIn(min = (minLines * 26 + 28).dp)
                    else Modifier.height(52.dp)
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = NpxPalette.surface,
                unfocusedContainerColor = NpxPalette.surface,
                disabledContainerColor = NpxPalette.surface.copy(alpha = 0.5f),
                focusedIndicatorColor = NpxPalette.accent,
                unfocusedIndicatorColor = NpxPalette.divider,
                disabledIndicatorColor = NpxPalette.divider,
                focusedTextColor = NpxPalette.textMid,
                unfocusedTextColor = NpxPalette.textMid,
                cursorColor = NpxPalette.accent,
            ),
            shape = RoundedCornerShape(4.dp),
        )
    }
}

/** 从 URL 提取域名 */
private fun hostOf(url: String): String {
    return try {
        val u = java.net.URI(url.trim())
        u.host ?: "—"
    } catch (_: Exception) {
        if (url.isBlank()) "—" else url.take(20)
    }
}

// ============================================================
// 统计（新版）
// ============================================================

@Composable
fun NewStatsPanel(vm: TrafficViewModel) {
    var period by rememberSaveable { mutableStateOf("day") }
    val labels = mapOf("day" to "日", "month" to "月", "year" to "年")
    LaunchedEffect(vm.cloudUsername) {
        if (vm.cloudUsername.isNotBlank()) {
            vm.refreshCloudStats(period, isManual = false)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ---- 页头 + 分段切换器 ----
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("流量统计", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NpxPalette.textMid)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "本机今日 ${formatBytes(vm.dailyTrafficBytes)} · ${vm.dailyTaskCount} 个任务",
                        fontSize = 11.sp,
                        color = NpxPalette.textGhost,
                    )
                }
                // 同步按钮
                QuickChip(
                    text = when {
                        vm.statsLoading -> "同步中…"
                        vm.manualSyncCooldownSeconds > 0 -> "${vm.manualSyncCooldownSeconds}s"
                        else -> "同步"
                    },
                    enabled = vm.canManuallySyncCloudStats,
                    primary = true,
                ) { vm.refreshCloudStats(period) }
            }
        }
        // ---- 周期分段切换 ----
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                labels.forEach { (key, label) ->
                    val selected = period == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (selected) NpxPalette.accent else Color.Transparent)
                            .then(
                                if (!selected) Modifier.border(1.dp, NpxPalette.divider, RoundedCornerShape(5.dp)) else Modifier
                            )
                            .clickable {
                                if (period != key) {
                                    period = key
                                    vm.refreshCloudStats(key, isManual = false)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "${label}榜",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) NpxPalette.onAccent else NpxPalette.textFaint,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        // ---- 云状态 ----
        if (vm.cloudStatus.isNotBlank()) {
            item {
                Text(
                    vm.cloudStatus,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 10.sp,
                    color = NpxPalette.textGhost,
                )
            }
        }
        // ---- 个人统计 ----
        item {
            NpxGroup(title = "个人${labels[period]}统计") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    val stats = vm.personalStats
                    Column(modifier = Modifier.weight(1f)) {
                        Text("流量", fontSize = 11.sp, color = NpxPalette.textGhost)
                        Text(
                            formatBytes(stats?.consumedBytes ?: 0L),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NpxPalette.textMid,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("任务数", fontSize = 11.sp, color = NpxPalette.textGhost)
                        Text(
                            "${stats?.taskCount ?: 0L}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NpxPalette.textMid,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // ---- 排行榜 ----
        item {
            Text(
                "全体用户${labels[period]}流量排行",
                modifier = Modifier.padding(horizontal = 24.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NpxPalette.textFaint,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        items(vm.leaderboardEntries) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "#${entry.rank}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) NpxPalette.accent else NpxPalette.textMuted,
                    modifier = Modifier.width(38.dp),
                )
                Text(
                    entry.username,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NpxPalette.textMid,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatBytes(entry.consumedBytes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NpxPalette.textDim,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(1.dp)
                    .background(NpxPalette.dividerSoft),
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ============================================================
// 设置（新版）：分组面板样式
// ============================================================

@Composable
fun NewSettingsPanel(vm: TrafficViewModel) {
    val context = LocalContext.current
    var savedUserAgentName by rememberSaveable { mutableStateOf("") }
    val localJsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::importLocalJsonBackup) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshBackgroundSystemStatus() }
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { vm.refreshBackgroundSystemStatus() }

    LaunchedEffect(Unit) { vm.refreshBackgroundSystemStatus() }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ---- 云端账号卡（新风格重写版）----
        item {
            Spacer(modifier = Modifier.height(14.dp))
            NpxCloudAccountPanel(vm)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- 请求 UA ----
        item {
            NpxGroup(title = "请求 UA", hint = "链接留空时继承全局 UA") {
                // 全局 UA 选择
                UserAgentDropdown(
                    label = "已保存的 UA",
                    value = vm.userAgentText,
                    options = vm.userAgentOptions,
                    emptyLabel = "空 UA（不使用默认 UA）",
                    onValueSelected = { vm.selectGlobalUserAgent(it) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                NpxField(
                    label = "自定义 UA（可命名保存，便于链接快速选择）",
                    value = vm.userAgentText,
                    enabled = true,
                    singleLine = false,
                    minLines = 3,
                ) { vm.updateUserAgent(it) }
                Spacer(modifier = Modifier.height(10.dp))
                NpxField(
                    label = "UA 名称 · 例如：云手机、线路 A",
                    value = savedUserAgentName,
                    enabled = true,
                    singleLine = true,
                ) { savedUserAgentName = it.take(40) }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PadButton(
                        text = "保存为新 UA",
                        primary = true,
                        enabled = vm.userAgentText.isNotBlank() && savedUserAgentName.isNotBlank(),
                    ) {
                        vm.saveCurrentUserAgentToList(savedUserAgentName)
                        savedUserAgentName = ""
                    }
                    PadButton(
                        text = "从列表移除",
                        primary = false,
                        enabled = vm.canRemoveCurrentUserAgent(),
                    ) { vm.removeCurrentUserAgentFromList() }
                    PadButton(text = "不使用 UA", primary = false, enabled = true) { vm.clearGlobalUserAgent() }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- 备份与恢复 ----
        item {
            NpxBackupPanel(vm)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- 本地 JSON ----
        item {
            NpxGroup(title = "本地 JSON", hint = "启动时自动读取公共下载目录备份") {
                SettingRow(
                    title = "从文件恢复",
                    desc = "手动选择 JSON 恢复链接和设置",
                ) {
                    MiniBtn("选择文件") {
                        localJsonImportLauncher.launch(arrayOf("application/json", "text/plain"))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- 后台运行 ----
        item {
            NpxBackgroundPanel(
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- 在线更新 ----
        item {
            NpxUpdatePanel(vm = vm, context = context)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================================
// 通用组件
// ============================================================

/** 分组面板：标题 + hint + 边框圆角卡（设计稿 .panel/.group） */
@Composable
fun NpxGroup(title: String, hint: String = "", content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NpxPalette.textFaint)
            if (hint.isNotBlank()) {
                Text(hint, fontSize = 10.sp, color = NpxPalette.textMuted)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NpxPalette.divider, RoundedCornerShape(8.dp))
                .background(NpxPalette.surface.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(14.dp),
            content = content,
        )
    }
}

/** 设置行：标题+说明 左，控件右 */
@Composable
fun SettingRow(title: String, desc: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NpxPalette.textMid)
            if (desc.isNotBlank()) {
                Text(desc, fontSize = 11.sp, color = NpxPalette.textGhost, lineHeight = 15.sp)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        trailing()
    }
}

/** 小型描边按钮（设计稿 .mini-btn） */
@Composable
fun MiniBtn(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, NpxPalette.accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) NpxPalette.accent else NpxPalette.textMuted,
        )
    }
}

/** 胶囊按钮（设计稿 .pad-btn：amber 实心 / dim 灰底 / outline 描边） */
@Composable
fun PadButton(text: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                when {
                    !enabled -> NpxPalette.surfaceAlt
                    primary -> NpxPalette.accent
                    else -> NpxPalette.surfaceAlt
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> NpxPalette.textMuted
                primary -> NpxPalette.onAccent
                else -> NpxPalette.textDim
            },
        )
    }
}
