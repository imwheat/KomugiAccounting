package com.komugi.komugiaccounting

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object NotificationKeepAlive {
    private const val LISTENER_RESET_INTERVAL_MS = 10_000L

    @Volatile
    private var lastListenerResetAt = 0L

    fun keepAlive(context: Context, forceResetListener: Boolean = false) {
        val appContext = context.applicationContext
        if (NotificationAutoBookService.isNotificationListenerEnabled(appContext)) {
            if ((forceResetListener || NotificationAutoBookService.instance == null) && canResetListenerNow()) {
                resetNotificationListenerComponent(appContext)
            }
            NotificationAutoBookService.ensureBound(appContext)
        }
        NotificationListenerForegroundService.start(appContext)
    }

    fun openNotificationListenerSettings(context: Context) {
        startFirstAvailable(
            context,
            listOf(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), appDetailsIntent(context))
        )
    }

    fun openAutoStartSettings(context: Context): Boolean =
        startFirstAvailable(context, autoStartIntents(context))

    fun openBackgroundCleanupSettings(context: Context): Boolean =
        startFirstAvailable(context, backgroundCleanupIntents(context))

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            appDetailsIntent(context)
        }
        startFirstAvailable(context, listOf(intent, appDetailsIntent(context)))
    }

    fun isNotificationChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.areNotificationsEnabled()
    }

    private fun canResetListenerNow(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastListenerResetAt < LISTENER_RESET_INTERVAL_MS) return false
        lastListenerResetAt = now
        return true
    }

    private fun resetNotificationListenerComponent(context: Context) {
        val component = ComponentName(context, NotificationAutoBookService::class.java)
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun startFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        intents.forEach { intent ->
            val launchIntent = intent.addNewTaskIfNeeded(context)
            if (launchIntent.resolveActivity(context.packageManager) != null) {
                val started = runCatching {
                    context.startActivity(launchIntent)
                }.isSuccess
                if (started) return true
            }
        }
        return false
    }

    private fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:${context.packageName}"))

    private fun autoStartIntents(context: Context): List<Intent> {
        return listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")),
            appDetailsIntent(context)
        )
    }

    private fun backgroundCleanupIntents(context: Context): List<Intent> {
        return listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.autotask.AutoTaskManageActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity")),
            Intent().setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            appDetailsIntent(context)
        )
    }

    private fun Intent.addNewTaskIfNeeded(context: Context): Intent =
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else this
}
