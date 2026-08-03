package com.example.networkpanelx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class TrafficKeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForegroundStarted = false
    private var keepScreenAwake = true
    private var showProgress = true
    private var taskName = "准备测速"
    private var consumedBytes = 0L
    private var targetBytes = 0L
    private var speedBytesPerSec = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        keepScreenAwake = intent?.getBooleanExtra(EXTRA_KEEP_SCREEN_AWAKE, true) ?: keepScreenAwake
        showProgress = intent?.getBooleanExtra(EXTRA_SHOW_PROGRESS, true) ?: showProgress
        taskName = intent?.getStringExtra(EXTRA_TASK_NAME)?.ifBlank { taskName } ?: taskName
        consumedBytes = intent?.getLongExtra(EXTRA_CONSUMED_BYTES, consumedBytes) ?: consumedBytes
        targetBytes = intent?.getLongExtra(EXTRA_TARGET_BYTES, targetBytes) ?: targetBytes
        speedBytesPerSec = intent?.getLongExtra(EXTRA_SPEED_BYTES_PER_SEC, speedBytesPerSec) ?: speedBytesPerSec

        if (keepScreenAwake) acquireWakeLock() else releaseWakeLock()

        val notification = buildNotification()
        if (isForegroundStarted) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundStarted = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        isForegroundStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台测速与进度",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示后台测速状态和进度"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:traffic_keepalive").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hasFiniteTarget = targetBytes > 0L
        val progress = if (hasFiniteTarget) {
            ((consumedBytes.coerceAtLeast(0L) * 100L) / targetBytes).toInt().coerceIn(0, 100)
        } else 0
        val contentText = if (showProgress) {
            val progressText = if (hasFiniteTarget) "$progress%" else "持续运行"
            "$taskName · ${formatBytes(speedBytesPerSec)}/s · $progressText"
        } else {
            "测速任务在后台运行"
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(if (showProgress) "网络面板X 正在测速" else "网络面板X 后台运行")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .apply {
                if (showProgress && hasFiniteTarget) setProgress(100, progress, false)
            }
            .build()
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

    companion object {
        private const val CHANNEL_ID = "traffic_keepalive_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val EXTRA_SHOW_PROGRESS = "show_progress"
        private const val EXTRA_TASK_NAME = "task_name"
        private const val EXTRA_CONSUMED_BYTES = "consumed_bytes"
        private const val EXTRA_TARGET_BYTES = "target_bytes"
        private const val EXTRA_SPEED_BYTES_PER_SEC = "speed_bytes_per_sec"

        fun start(
            context: Context,
            keepScreenAwake: Boolean,
            showProgress: Boolean,
            taskName: String,
            consumedBytes: Long,
            targetBytes: Long,
            speedBytesPerSec: Long,
        ) {
            val intent = serviceIntent(
                context = context,
                keepScreenAwake = keepScreenAwake,
                showProgress = showProgress,
                taskName = taskName,
                consumedBytes = consumedBytes,
                targetBytes = targetBytes,
                speedBytesPerSec = speedBytesPerSec,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun update(
            context: Context,
            keepScreenAwake: Boolean,
            showProgress: Boolean,
            taskName: String,
            consumedBytes: Long,
            targetBytes: Long,
            speedBytesPerSec: Long,
        ) {
            context.startService(
                serviceIntent(
                    context = context,
                    keepScreenAwake = keepScreenAwake,
                    showProgress = showProgress,
                    taskName = taskName,
                    consumedBytes = consumedBytes,
                    targetBytes = targetBytes,
                    speedBytesPerSec = speedBytesPerSec,
                ),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrafficKeepAliveService::class.java))
        }

        private fun serviceIntent(
            context: Context,
            keepScreenAwake: Boolean,
            showProgress: Boolean,
            taskName: String,
            consumedBytes: Long,
            targetBytes: Long,
            speedBytesPerSec: Long,
        ): Intent = Intent(context, TrafficKeepAliveService::class.java).apply {
            putExtra(EXTRA_KEEP_SCREEN_AWAKE, keepScreenAwake)
            putExtra(EXTRA_SHOW_PROGRESS, showProgress)
            putExtra(EXTRA_TASK_NAME, taskName)
            putExtra(EXTRA_CONSUMED_BYTES, consumedBytes)
            putExtra(EXTRA_TARGET_BYTES, targetBytes)
            putExtra(EXTRA_SPEED_BYTES_PER_SEC, speedBytesPerSec)
        }
    }
}
