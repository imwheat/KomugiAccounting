package com.komugi.komugiaccounting

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.komugi.komugiaccounting.data.repository.AppDataRepository

class NotificationAutoBookService : NotificationListenerService() {
    override fun onListenerConnected() {
        activeNotifications?.forEach(::processNotification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString().orEmpty()
        ).firstOrNull { it.isNotBlank() }.orEmpty()
        val text = buildList {
            add(notification.tickerText?.toString().orEmpty())
            addAll(extras.readKnownNotificationText())
            addAll(extras.readAllTextExtras())
        }.filter { it.isNotBlank() && it != title }.distinct().joinToString("\n")
        if (title.isBlank() && text.isBlank()) return
        AppDataRepository.get(applicationContext).handleNotification(title.ifBlank { sbn.packageName }, text, sbn.postTime)
    }

    private fun Bundle.readKnownNotificationText(): List<String> =
        listOf(
            getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
            getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty(),
            getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() }.orEmpty()
        )

    private fun Bundle.readAllTextExtras(): List<String> =
        keySet().flatMap { key ->
            when (val value = get(key)) {
                is CharSequence -> listOf(value.toString())
                is Array<*> -> value.flatMap {
                    when (it) {
                        is Bundle -> it.readAllTextExtras()
                        null -> emptyList()
                        else -> listOf(it.toString())
                    }
                }
                is Iterable<*> -> value.flatMap {
                    when (it) {
                        is Bundle -> it.readAllTextExtras()
                        null -> emptyList()
                        else -> listOf(it.toString())
                    }
                }
                is Bundle -> value.readAllTextExtras()
                else -> emptyList()
            }
        }
}
