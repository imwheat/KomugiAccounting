package com.komugi.komugiaccounting

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AutoBookTodoBadgeNotifier {
    private const val CHANNEL_ID = "auto_book_todo_badge_channel_v2"
    private const val CHANNEL_NAME = "自动记账待办"
    private const val NOTIFICATION_ID = 2001

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sync(context: Context, todoCount: Int) {
        val appContext = context.applicationContext
        val badgeCount = todoCount.coerceAtLeast(0)

        if (badgeCount <= 0) {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
            updateLauncherBadgeCompat(appContext, 0)
            return
        }

        if (canPostNotifications(appContext)) {
            createChannel(appContext)
            NotificationManagerCompat.from(appContext).notify(
                NOTIFICATION_ID,
                createNotification(appContext, badgeCount)
            )
        }

        updateLauncherBadgeCompat(appContext, badgeCount)
    }

    private fun createNotification(context: Context, todoCount: Int) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("自动记账待办")
            .setContentText("还有 ${todoCount.badgeText()} 个待办未处理")
            .setContentIntent(createPendingIntent(context))
            .setNumber(todoCount.coerceAtMost(99))
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "用于在桌面图标显示未处理自动记账待办数量"
            setShowBadge(true)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        val hasRuntimePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return hasRuntimePermission && manager.areNotificationsEnabled()
    }

    private fun updateLauncherBadgeCompat(context: Context, todoCount: Int) {
        val displayCount = todoCount.coerceIn(0, 99)
        val launcher = ComponentName(context, SplashActivity::class.java)
        updateSamsungAndCommonBadger(context, launcher, displayCount)
        updateSony(context, launcher, displayCount)
        updateHtc(context, launcher, displayCount)
        updateNova(context, launcher, displayCount)
        updateHuawei(context, launcher, displayCount)
    }

    private fun updateSamsungAndCommonBadger(context: Context, launcher: ComponentName, count: Int) {
        sendBadgeBroadcast(
            context,
            Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", launcher.packageName)
                putExtra("badge_count_class_name", launcher.className)
            }
        )
        sendBadgeBroadcast(
            context,
            Intent("me.leolin.shortcutbadger.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", launcher.packageName)
                putExtra("badge_count_class_name", launcher.className)
            }
        )
    }

    private fun updateSony(context: Context, launcher: ComponentName, count: Int) {
        sendBadgeBroadcast(
            context,
            Intent("com.sonyericsson.home.action.UPDATE_BADGE").apply {
                putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
                putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcher.className)
                putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", count.badgeText())
                putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", launcher.packageName)
            }
        )
    }

    private fun updateHtc(context: Context, launcher: ComponentName, count: Int) {
        sendBadgeBroadcast(
            context,
            Intent("com.htc.launcher.action.SET_NOTIFICATION").apply {
                putExtra("com.htc.launcher.extra.COMPONENT", launcher.flattenToShortString())
                putExtra("com.htc.launcher.extra.COUNT", count)
            }
        )
        sendBadgeBroadcast(
            context,
            Intent("com.htc.launcher.action.UPDATE_SHORTCUT").apply {
                putExtra("packagename", launcher.packageName)
                putExtra("count", count)
            }
        )
    }

    private fun updateNova(context: Context, launcher: ComponentName, count: Int) {
        sendBadgeBroadcast(
            context,
            Intent("com.teslacoilsw.launcher.UPDATE_BADGE").apply {
                putExtra("com.teslacoilsw.launcher.extra.COMPONENT", launcher.flattenToShortString())
                putExtra("com.teslacoilsw.launcher.extra.COUNT", count)
            }
        )
    }

    private fun updateHuawei(context: Context, launcher: ComponentName, count: Int) {
        runCatching {
            val extras = Bundle().apply {
                putString("package", launcher.packageName)
                putString("class", launcher.className)
                putInt("badgenumber", count)
            }
            context.contentResolver.call(
                android.net.Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                extras
            )
        }
    }

    private fun sendBadgeBroadcast(context: Context, intent: Intent) {
        runCatching { context.sendBroadcast(intent) }
    }

    private fun Int.badgeText(): String = if (this >= 100) "99+" else coerceAtLeast(0).toString()
}
