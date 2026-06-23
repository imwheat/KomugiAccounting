package com.komugi.komugiaccounting

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.komugi.komugiaccounting.data.repository.AppDataRepository

class NotificationAutoBookService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        AppDataRepository.get(applicationContext).handleNotification(title, text, sbn.postTime)
    }
}
