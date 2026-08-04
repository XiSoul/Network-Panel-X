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
    private var taskRunning = false
    private var taskName = "准备测速"
    private var totalConsumedBytes = 0L
    private var totalTargetBytes = 0L
    private var speedBytesPerSec = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_FROM_NOTIFICATION -> {
                TrafficNotificationCommandBus.pause()
                return START_STICKY
            }
            ACTION_RESUME_FROM_NOTIFICATION -> {
                TrafficNotificationCommandBus.resume()
                return START_STICKY
            }
        }
        keepScreenAwake = intent?.getBooleanExtra(EXTRA_KEEP_SCREEN_AWAKE, true) ?: keepScreenAwake
        showProgress = intent?.getBooleanExtra(EXTRA_SHOW_PROGRESS, true) ?: showProgress
        taskRunning = intent?.getBooleanExtra(EXTRA_TASK_RUNNING, taskRunning) ?: taskRunning
        taskName = intent?.getStringExtra(EXTRA_TASK_NAME)?.ifBlank { taskName } ?: taskName
        totalConsumedBytes = intent?.getLongExtra(EXTRA_TOTAL_CONSUMED_BYTES, totalConsumedBytes) ?: totalConsumedBytes
        totalTargetBytes = intent?.getLongExtra(EXTRA_TOTAL_TARGET_BYTES, totalTargetBytes) ?: totalTargetBytes
        speedBytesPerSec = intent?.getLongExtra(EXTRA_SPEED_BYTES_PER_SEC, speedBytesPerSec) ?: speedBytesPerSec

        if (keepScreenAwake) acquireWakeLock() else releaseWakeLock()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Clear the lock-screen mirror used by the previous build before posting the single service notification.
        notificationManager.cancel(LEGACY_LOCK_SCREEN_NOTIFICATION_ID)
        val notification = buildForegroundNotification()
        if (isForegroundStarted) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundStarted = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        isForegroundStarted = false
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(LEGACY_LOCK_SCREEN_NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台测速与锁屏通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "在状态栏和锁屏界面显示后台测速文字状态"
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
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

    private fun buildForegroundNotification(): Notification {
        val contentText = notificationText()
        val pendingIntent = launchPendingIntent()
        val pausePendingIntent = notificationActionPendingIntent(ACTION_PAUSE_FROM_NOTIFICATION, PAUSE_ACTION_REQUEST_CODE)
        val resumePendingIntent = notificationActionPendingIntent(ACTION_RESUME_FROM_NOTIFICATION, RESUME_ACTION_REQUEST_CODE)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(if (taskRunning && showProgress) "网络面板X 正在测速" else "网络面板X 后台运行")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "暂停", pausePendingIntent)
            .addAction(android.R.drawable.ic_media_play, "继续", resumePendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setPublicVersion(buildPublicNotification(contentText, pendingIntent))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .build()
    }

    private fun launchPendingIntent(): PendingIntent {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val actionIntent = Intent(this, TrafficKeepAliveService::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getService(
            this,
            requestCode,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationText(): String {
        return if (!taskRunning) {
            "后台服务已开启"
        } else if (showProgress) {
            "$taskName · 总流量：${formatBytes(totalConsumedBytes)} / ${formatTargetBytes(totalTargetBytes)} · ${formatBytes(speedBytesPerSec)}/s"
        } else {
            "测速任务在后台运行"
        }
    }

    private fun buildPublicNotification(
        contentText: String,
        pendingIntent: PendingIntent,
    ): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(if (taskRunning) "网络面板X 正在测速" else "网络面板X 后台运行")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
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

    private fun formatTargetBytes(bytes: Long): String = if (bytes > 0L) formatBytes(bytes) else "无限"

    companion object {
        private const val CHANNEL_ID = "traffic_keepalive_channel_v4"
        private const val NOTIFICATION_ID = 1001
        private const val LEGACY_LOCK_SCREEN_NOTIFICATION_ID = 1002
        private const val PAUSE_ACTION_REQUEST_CODE = 1003
        private const val RESUME_ACTION_REQUEST_CODE = 1004
        private const val EXTRA_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val EXTRA_SHOW_PROGRESS = "show_progress"
        private const val EXTRA_TASK_RUNNING = "task_running"
        private const val EXTRA_TASK_NAME = "task_name"
        private const val EXTRA_TOTAL_CONSUMED_BYTES = "total_consumed_bytes"
        private const val EXTRA_TOTAL_TARGET_BYTES = "total_target_bytes"
        private const val EXTRA_SPEED_BYTES_PER_SEC = "speed_bytes_per_sec"

        fun start(
            context: Context,
            keepScreenAwake: Boolean,
            showProgress: Boolean,
            taskRunning: Boolean,
            taskName: String,
            totalConsumedBytes: Long,
            totalTargetBytes: Long,
            speedBytesPerSec: Long,
        ) {
            val intent = serviceIntent(
                context = context,
                keepScreenAwake = keepScreenAwake,
                showProgress = showProgress,
                taskRunning = taskRunning,
                taskName = taskName,
                totalConsumedBytes = totalConsumedBytes,
                totalTargetBytes = totalTargetBytes,
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
            taskRunning: Boolean,
            taskName: String,
            totalConsumedBytes: Long,
            totalTargetBytes: Long,
            speedBytesPerSec: Long,
        ) {
            context.startService(
                serviceIntent(
                    context = context,
                    keepScreenAwake = keepScreenAwake,
                    showProgress = showProgress,
                    taskRunning = taskRunning,
                    taskName = taskName,
                totalConsumedBytes = totalConsumedBytes,
                totalTargetBytes = totalTargetBytes,
                    speedBytesPerSec = speedBytesPerSec,
                ),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrafficKeepAliveService::class.java))
        }

        fun notificationChannelId(): String = CHANNEL_ID

        private fun serviceIntent(
            context: Context,
            keepScreenAwake: Boolean,
            showProgress: Boolean,
            taskRunning: Boolean,
            taskName: String,
            totalConsumedBytes: Long,
            totalTargetBytes: Long,
            speedBytesPerSec: Long,
        ): Intent = Intent(context, TrafficKeepAliveService::class.java).apply {
            putExtra(EXTRA_KEEP_SCREEN_AWAKE, keepScreenAwake)
            putExtra(EXTRA_SHOW_PROGRESS, showProgress)
            putExtra(EXTRA_TASK_RUNNING, taskRunning)
            putExtra(EXTRA_TASK_NAME, taskName)
            putExtra(EXTRA_TOTAL_CONSUMED_BYTES, totalConsumedBytes)
            putExtra(EXTRA_TOTAL_TARGET_BYTES, totalTargetBytes)
            putExtra(EXTRA_SPEED_BYTES_PER_SEC, speedBytesPerSec)
        }
    }
}
