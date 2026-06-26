package com.komugi.komugiaccounting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> NotificationKeepAlive.keepAlive(context, forceResetListener = true)
        }
    }
}
