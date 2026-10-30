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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置页四个子面板的新 UI 重写（替代旧 Material 卡片风格的
 * CloudAccountPanel / BackupPanel / BackgroundSettingsPanel / UpdatePanel）。
 * 视觉规范与 NpxNewUi/NpxNewUi2 一致：NpxField 输入框、NpxCheckbox、
 * QuickChip 分段、PadButton 按钮、NpxToggle 开关。
 */

// ============================================================
// 云端账号（新）
// ============================================================

@Composable
fun NpxCloudAccountPanel(vm: TrafficViewModel) {
    var username by rememberSaveable { mutableStateOf(vm.savedCloudUsername) }
    var password by remember { mutableStateOf(vm.savedCloudPassword) }
    var rememberPassword by rememberSaveable { mutableStateOf(vm.savedCloudPassword.isNotBlank()) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var accountMode by rememberSaveable { mutableStateOf("login") }

    NpxGroup(title = "云端账号与统计", hint = "统计排行同步到云端") {
        if (vm.cloudUsername.isNotBlank()) {
            // 已登录态
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NpxPalette.doneGreen, androidx.compose.foundation.shape.CircleShape),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("已登录账号", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NpxPalette.text)
                Spacer(modifier = Modifier.weight(1f))
                Text(vm.cloudUsername, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NpxPalette.accent)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(vm.cloudStatus.ifBlank { "已登录" }, fontSize = 11.sp, color = NpxPalette.textGhost)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PadButton("切换账号", primary = false) {
                    vm.logoutCloudAccount()
                }
                PadButton("退出登录", primary = false) { vm.logoutCloudAccount() }
            }
        } else {
            // 分段：登录/注册/忘记密码
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("login" to "登录", "register" to "注册", "reset" to "忘记密码").forEach { (key, label) ->
                    val selected = accountMode == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (selected) NpxPalette.accent else androidx.compose.ui.graphics.Color.Transparent)
                            .then(
                                if (!selected) Modifier.border(1.dp, NpxPalette.divider, RoundedCornerShape(5.dp)) else Modifier
                            )
                            .clickable { accountMode = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) NpxPalette.onAccent else NpxPalette.textFaint,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (accountMode != "reset") {
                NpxField("用户名", username, true, singleLine = true) { username = it.take(32) }
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (accountMode != "login") {
                NpxField("邮箱", email, true, singleLine = true, email = true) { email = it.take(254) }
                Spacer(modifier = Modifier.height(10.dp))
            }
            NpxField(
                if (accountMode == "reset") "新密码" else "密码",
                password, true, singleLine = true, password = true,
            ) { password = it.take(128) }
            if (accountMode == "login") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NpxCheckbox(checked = rememberPassword, enabled = true) { rememberPassword = it }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存密码", fontSize = 12.sp, color = NpxPalette.textGhost)
                }
            }
            if (accountMode != "login") {
                Spacer(modifier = Modifier.height(10.dp))
                NpxField("邮箱验证码", code, true, singleLine = true, numeric = true) { code = it.filter(Char::isDigit).take(6) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (accountMode) {
                    "login" -> PadButton("登录", primary = true) {
                        vm.loginCloudAccount(DEFAULT_CLOUD_API_URL, username, password, rememberPassword)
                    }
                    "register" -> {
                        PadButton("发送验证码", primary = true) { vm.requestCloudRegistrationCode(DEFAULT_CLOUD_API_URL, username, email, password) }
                        PadButton("完成注册", primary = false) { vm.registerCloudAccount(DEFAULT_CLOUD_API_URL, username, email, password, code) }
                    }
                    else -> {
                        PadButton("发送验证码", primary = true) { vm.requestCloudPasswordResetCode(DEFAULT_CLOUD_API_URL, email) }
                        PadButton("重置密码", primary = false) { vm.resetCloudPassword(DEFAULT_CLOUD_API_URL, email, code, password) }
                    }
                }
            }
            if (vm.cloudStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(vm.cloudStatus, fontSize = 10.sp, color = NpxPalette.textGhost)
            }
        }
    }
}

// ============================================================
// 备份与恢复（新）
// ============================================================

@Composable
fun NpxBackupPanel(vm: TrafficViewModel) {
    val saved = vm.backupConnection
    var provider by rememberSaveable { mutableStateOf(saved.provider) }
    var webDavUrl by rememberSaveable { mutableStateOf(saved.webDavUrl) }
    var webDavUsername by rememberSaveable { mutableStateOf(saved.webDavUsername) }
    // S2 修复保持：密码不进 saved-instance-state
    var webDavPassword by remember { mutableStateOf(saved.webDavPassword) }
    var s3Endpoint by rememberSaveable { mutableStateOf(saved.s3Endpoint) }
    var s3Region by rememberSaveable { mutableStateOf(saved.s3Region) }
    var s3Bucket by rememberSaveable { mutableStateOf(saved.s3Bucket) }
    var s3AccessKey by rememberSaveable { mutableStateOf(saved.s3AccessKey) }
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

    NpxGroup(title = "备份与恢复", hint = "每次上传生成带时间戳版本") {
        // WebDAV / S3 分段
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("webdav" to "WebDAV", "s3" to "S3").forEach { (key, label) ->
                val selected = provider == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) NpxPalette.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .then(
                            if (!selected) Modifier.border(1.dp, NpxPalette.divider, RoundedCornerShape(5.dp)) else Modifier
                        )
                        .clickable {
                            provider = key
                            vm.clearBackupList()
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) NpxPalette.onAccent else NpxPalette.textFaint,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (provider == "webdav") {
            NpxField("WebDAV 备份目录", webDavUrl, true, singleLine = true) { webDavUrl = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("WebDAV 用户名", webDavUsername, true, singleLine = true) { webDavUsername = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("WebDAV 密码", webDavPassword, true, singleLine = true, password = true) { webDavPassword = it }
        } else {
            NpxField("S3 Endpoint", s3Endpoint, true, singleLine = true) { s3Endpoint = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("S3 Region", s3Region, true, singleLine = true) { s3Region = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("Bucket", s3Bucket, true, singleLine = true) { s3Bucket = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("Access Key", s3AccessKey, true, singleLine = true) { s3AccessKey = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("Secret Key", s3SecretKey, true, singleLine = true, password = true) { s3SecretKey = it }
            Spacer(modifier = Modifier.height(10.dp))
            NpxField("对象前缀（文件夹）", s3ObjectPrefix, true, singleLine = true) { s3ObjectPrefix = it }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton("上传新备份", primary = true, enabled = !vm.backupBusy) { vm.uploadBackup(connection()) }
            PadButton("刷新在线列表", primary = false, enabled = !vm.backupBusy) { vm.refreshBackupList(connection()) }
        }
        if (vm.backupStatus.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(vm.backupStatus, fontSize = 10.sp, color = NpxPalette.textGhost)
        }
        // 备份版本列表
        vm.backupPageItems.forEach { backup ->
            val isLatest = vm.remoteBackups.firstOrNull()?.remoteId == backup.remoteId
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NpxPalette.surface, RoundedCornerShape(6.dp))
                    .padding(10.dp),
            ) {
                Text(
                    if (isLatest) "最新 · ${backup.fileName}" else backup.fileName,
                    fontSize = 12.sp,
                    fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                    color = NpxPalette.textMid,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${formatBackupTime(backup.modifiedAtEpochMs)} · ${formatBytes(backup.sizeBytes)}",
                    fontSize = 10.sp,
                    color = NpxPalette.textGhost,
                )
                Spacer(modifier = Modifier.height(6.dp))
                MiniBtn("恢复这个版本", enabled = !vm.backupBusy) { selectedBackup = backup }
            }
        }
        if (vm.remoteBackups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniBtn("上一页", enabled = vm.backupPage > 0 && !vm.backupBusy) { vm.previousBackupPage() }
                Text("${vm.backupPage + 1} / ${vm.backupPageCount}", fontSize = 11.sp, color = NpxPalette.textFaint)
                MiniBtn("下一页", enabled = vm.backupPage + 1 < vm.backupPageCount && !vm.backupBusy) { vm.nextBackupPage() }
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

// ============================================================
// 后台运行（新）
// ============================================================

@Composable
fun NpxBackgroundPanel(
    vm: TrafficViewModel,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    NpxGroup(title = "后台运行", hint = "挂机防杀") {
        Text(
            "启用后，测速会使用前台服务在后台持续运行。Android 要求前台服务保留系统通知。",
            fontSize = 10.sp,
            color = NpxPalette.textMuted,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        NpxSettingToggle("后台常驻", "测速时启动前台服务，降低被系统回收的概率。", vm.keepAliveEnabled) {
            vm.updateKeepAliveEnabled(it)
        }
        Spacer(modifier = Modifier.height(12.dp))
        NpxSettingToggle(
            "熄屏继续运行", "使用唤醒锁保持 CPU 和网络任务；会增加耗电。",
            vm.keepScreenAwakeEnabled, enabled = vm.keepAliveEnabled,
        ) { vm.updateKeepScreenAwakeEnabled(it) }
        Spacer(modifier = Modifier.height(12.dp))
        NpxSettingToggle(
            "状态栏进度通知", "显示总流量、网速和任务状态文字，并在锁屏界面公开显示。",
            vm.progressNotificationEnabled, enabled = vm.keepAliveEnabled,
        ) { vm.updateProgressNotificationEnabled(it) }
        Spacer(modifier = Modifier.height(12.dp))
        // 权限状态行
        SettingRow(
            title = "通知权限",
            desc = if (vm.notificationPermissionGranted) "已允许"
            else "未允许，Android 13 及以上系统可能不在通知栏展示进度。",
        ) {
            if (vm.notificationPermissionGranted) {
                NpxBadge("已授权", ok = true)
            } else {
                MiniBtn("去授权") { onRequestNotificationPermission() }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        SettingRow(title = "锁屏通知设置", desc = "调整系统通知渠道的可见性与样式") {
            MiniBtn("打开") { onOpenNotificationSettings() }
        }
        Spacer(modifier = Modifier.height(10.dp))
        SettingRow(
            title = "电池优化白名单",
            desc = if (vm.ignoringBatteryOptimizations) "已忽略，挂机防杀推荐保持"
            else "未忽略，熄屏后可能被系统限制",
        ) {
            if (vm.ignoringBatteryOptimizations) {
                NpxBadge("已授权", ok = true)
            } else {
                MiniBtn("去授权") { onRequestBatteryExemption() }
            }
        }
    }
}

/** 设置行 + 右侧 toggle 开关 */
@Composable
private fun NpxSettingToggle(
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NpxPalette.textMid)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = NpxPalette.textGhost, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        NpxToggle(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

/** 状态徽章（设计稿 .badge-ok/.badge-warn） */
@Composable
private fun NpxBadge(text: String, ok: Boolean) {
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (ok) NpxPalette.doneGreen else NpxPalette.failRed,
                RoundedCornerShape(3.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (ok) NpxPalette.doneGreen else NpxPalette.failRed,
        )
    }
}

// ============================================================
// 在线更新（新）
// ============================================================

@Composable
fun NpxUpdatePanel(vm: TrafficViewModel, context: android.content.Context) {
    NpxGroup(title = "在线更新", hint = "GitHub Release") {
        SettingRow(title = "当前版本", desc = "从 GitHub Release 检查最新正式包") {
            Text(
                BuildConfig.VERSION_NAME,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NpxPalette.textDim,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        when (val state = vm.updateState) {
            is UpdateState.Checking -> Text("正在检查更新…", fontSize = 11.sp, color = NpxPalette.textGhost)
            is UpdateState.UpToDate -> Text("已是最新版本（${state.latestVersion}）。", fontSize = 11.sp, color = NpxPalette.textGhost)
            is UpdateState.Available -> {
                Text(
                    "发现新版本：${state.release.version}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NpxPalette.accent,
                )
                if (state.release.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        state.release.notes,
                        fontSize = 10.sp,
                        color = NpxPalette.textGhost,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp,
                    )
                }
            }
            is UpdateState.Error -> Text(state.message, fontSize = 11.sp, color = NpxPalette.failRed)
            else -> {}
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton("检查更新", primary = true, enabled = vm.updateState !is UpdateState.Checking) {
                vm.checkForUpdates()
            }
            PadButton("GitHub 更新", primary = false) { openExternalLink(context, GITHUB_RELEASES_URL) }
            val available = vm.updateState as? UpdateState.Available
            if (available != null) {
                PadButton(
                    if (available.release.apkDownloadUrl != null) "下载更新" else "查看发行页",
                    primary = false,
                ) {
                    openExternalLink(
                        context,
                        available.release.apkDownloadUrl ?: available.release.releasePageUrl,
                    )
                }
            }
        }
    }
}
