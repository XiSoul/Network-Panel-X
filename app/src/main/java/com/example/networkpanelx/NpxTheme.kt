package com.example.networkpanelx

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 新 UI 设计系统（2026-10-25 定稿渲染图落地）
 * 深色：暖黑 #141414 底 + 琥珀橙 #FF9F0A 强调
 * 浅色：暖白 #F1F0EA 底 + 琥珀橙 #C77800 强调
 * 数字用等宽感字体（Rajdhani 对应 Compose 默认 sans 的 Bold + 紧字距近似）
 */

// ---------- 深色主题色板 ----------
object NpxDark {
    val bg = Color(0xFF141414)          // 主背景（暖黑）
    val bgDeep = Color(0xFF121211)      // tabbar 背景
    val surface = Color(0xFF1E1C18)     // 进度条轨道等控件底
    val surfaceAlt = Color(0xFF2A2824)  // 次按钮底
    val divider = Color(0xFF26241F)     // 分隔线（顶部分区）
    val dividerSoft = Color(0xFF232019) // 分隔线（内容分区）
    val tickLine = Color(0xFF2A2824)    // 进度条刻度线
    val text = Color(0xFFF2EEE8)        // 主文字（亮暖白）
    val textMid = Color(0xFFD8D3CC)     // 正文
    val textDim = Color(0xFFB9B4AC)     // 次级
    val textFaint = Color(0xFF8F8A82)   // 弱化
    val textGhost = Color(0xFF736E66)   // 标签/说明
    val textMuted = Color(0xFF5A564E)   // 免责声明
    val textGhost2 = Color(0xFF3C3A34)  // 版本号
    val accent = Color(0xFFFF9F0A)      // 琥珀橙（核心强调）
    val accentBright = Color(0xFFFFD08A) // 进度端点亮边
    val accentDark = Color(0xFFC77800)  // 浅色主题用橙
    val onAccent = Color(0xFF141414)    // 橙底上的文字
    // 状态点色：深色主题用高饱和亮色，保证在 #141414 上醒目
    val doneGreen = Color(0xFF34C759)   // 完成绿（iOS 亮绿）
    val failRed = Color(0xFFE5484D)     // 失败红（亮红）
    val runningBlue = Color(0xFF3E9EFF) // 运行中蓝
    val waitingYellow = Color(0xFFFFC53D) // 等待黄
    val idleGray = Color(0xFF8A8A8A)    // 未勾选灰（提亮一档，深底上可辨）
}

// ---------- 浅色主题色板 ----------
object NpxLight {
    val bg = Color(0xFFF1F0EA)          // 主背景（暖白）
    val bgDeep = Color(0xFFEAE8E1)      // tabbar 背景
    val surface = Color(0xFFE5E2D6)     // 控件底
    val surfaceAlt = Color(0xFFDEDACD)  // 次按钮底
    val divider = Color(0xFFD8D4C7)     // 分隔线
    val dividerSoft = Color(0xFFDEDACD) // 分隔线（内容分区）
    val tickLine = Color(0xFFCCC6B8)    // 刻度线
    val text = Color(0xFF1C1A16)        // 主文字（暖黑）
    val textMid = Color(0xFF33302A)     // 正文
    val textDim = Color(0xFF4A443A)     // 次级
    val textFaint = Color(0xFF6E6759)   // 弱化
    val textGhost = Color(0xFF6E6759)   // 标签/说明
    val textMuted = Color(0xFF8A8375)   // 免责声明
    val textGhost2 = Color(0xFFB5AF9F)  // 版本号
    val accent = Color(0xFFC77800)      // 琥珀橙（深）
    val accentBright = Color(0xFFE8A93E) // 进度端点亮边
    val onAccent = Color(0xFF141414)    // 橙底上的文字
    // 状态点色：浅色主题用加深版本，保证在 #F1F0EA 白底上对比度足够
    val doneGreen = Color(0xFF1E8E3E)   // 完成绿（深绿）
    val failRed = Color(0xFFC62828)     // 失败红（深红）
    val runningBlue = Color(0xFF1565C0) // 运行中蓝
    val waitingYellow = Color(0xFFA06A00) // 等待黄（深金，白底上对比更足）
    val idleGray = Color(0xFF9E9E9E)    // 未勾选灰
}

/** 语义色访问器：随主题切换 */
object NpxPalette {
    val bg get() = if (NpxThemeState.isDark) NpxDark.bg else NpxLight.bg
    val bgDeep get() = if (NpxThemeState.isDark) NpxDark.bgDeep else NpxLight.bgDeep
    val surface get() = if (NpxThemeState.isDark) NpxDark.surface else NpxLight.surface
    val surfaceAlt get() = if (NpxThemeState.isDark) NpxDark.surfaceAlt else NpxLight.surfaceAlt
    val divider get() = if (NpxThemeState.isDark) NpxDark.divider else NpxLight.divider
    val dividerSoft get() = if (NpxThemeState.isDark) NpxDark.dividerSoft else NpxLight.dividerSoft
    val tickLine get() = if (NpxThemeState.isDark) NpxDark.tickLine else NpxLight.tickLine
    val text get() = if (NpxThemeState.isDark) NpxDark.text else NpxLight.text
    val textMid get() = if (NpxThemeState.isDark) NpxDark.textMid else NpxLight.textMid
    val textDim get() = if (NpxThemeState.isDark) NpxDark.textDim else NpxLight.textDim
    val textFaint get() = if (NpxThemeState.isDark) NpxDark.textFaint else NpxLight.textFaint
    val textGhost get() = if (NpxThemeState.isDark) NpxDark.textGhost else NpxLight.textGhost
    val textMuted get() = if (NpxThemeState.isDark) NpxDark.textMuted else NpxLight.textMuted
    val textGhost2 get() = if (NpxThemeState.isDark) NpxDark.textGhost2 else NpxLight.textGhost2
    val accent get() = if (NpxThemeState.isDark) NpxDark.accent else NpxLight.accent
    val accentBright get() = if (NpxThemeState.isDark) NpxDark.accentBright else NpxLight.accentBright
    val onAccent get() = if (NpxThemeState.isDark) NpxDark.onAccent else NpxLight.onAccent
    val doneGreen get() = if (NpxThemeState.isDark) NpxDark.doneGreen else NpxLight.doneGreen
    val failRed get() = if (NpxThemeState.isDark) NpxDark.failRed else NpxLight.failRed
    val runningBlue get() = if (NpxThemeState.isDark) NpxDark.runningBlue else NpxLight.runningBlue
    val waitingYellow get() = if (NpxThemeState.isDark) NpxDark.waitingYellow else NpxLight.waitingYellow
    val idleGray get() = if (NpxThemeState.isDark) NpxDark.idleGray else NpxLight.idleGray
}

/** 主题状态（由 App 根部写入；避免每层传 isDark） */
object NpxThemeState {
    var isDark: Boolean = true
}

/**
 * 新版 MaterialTheme 包装：色板映射到 M3 语义槽，
 * 业务组件统一走 NpxPalette 取色（精确还原设计稿）。
 */
@Composable
fun NpxTheme(dark: Boolean, content: @Composable () -> Unit) {
    NpxThemeState.isDark = dark
    val scheme = if (dark) {
        darkColorScheme(
            primary = NpxDark.accent,
            onPrimary = NpxDark.onAccent,
            secondary = NpxDark.doneGreen,
            background = NpxDark.bg,
            onBackground = NpxDark.textMid,
            surface = NpxDark.bg,
            onSurface = NpxDark.textMid,
            surfaceVariant = NpxDark.surface,
            onSurfaceVariant = NpxDark.textDim,
            outline = NpxDark.divider,
            error = NpxDark.failRed,
        )
    } else {
        lightColorScheme(
            primary = NpxLight.accent,
            onPrimary = NpxLight.onAccent,
            secondary = NpxLight.doneGreen,
            background = NpxLight.bg,
            onBackground = NpxLight.textMid,
            surface = NpxLight.bg,
            onSurface = NpxLight.textMid,
            surfaceVariant = NpxLight.surface,
            onSurfaceVariant = NpxLight.textDim,
            outline = NpxLight.divider,
            error = NpxLight.failRed,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
