package com.komugi.komugiaccounting

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationAutoBookService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching {
            activeNotifications?.forEach { sbn ->
                serviceScope.launch { processNotification(sbn) }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        serviceScope.launch { processNotification(sbn) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun shouldProcess(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        if (sbn.packageName == packageName) return false
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        return true
    }

    private fun processNotification(sbn: StatusBarNotification) {
        if (!shouldProcess(sbn)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle.EMPTY
        val title = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString().orEmpty()
        ).firstOrNull { it.isNotBlank() }.orEmpty()
        val text = buildList {
            add(notification.tickerText?.toString().orEmpty())
            addAll(extras.readKnownNotificationText())
            addAll(extras.readMessages())
            addAll(extras.readAllTextExtras(maxDepth = 2))
        }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != title }
            .distinct()
            .joinToString("\n")

        if (title.isBlank() && text.isBlank()) return

        AppDataRepository.get(applicationContext).handleNotification(
            title = title.ifBlank { sbn.packageName },
            text = text,
            postTime = sbn.postTime
        )
    }

    private fun Bundle.readKnownNotificationText(): List<String> = listOf(
        getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
        getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
        getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
        getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty(),
        getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() }.orEmpty()
    )

    @Suppress("DEPRECATION")
    private fun Bundle.readMessages(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return emptyList()
        val messages: Array<Parcelable> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            getParcelableArray(Notification.EXTRA_MESSAGES)?.filterIsInstance<Parcelable>()?.toTypedArray()
        } ?: return emptyList()

        return messages.mapNotNull { parcel ->
            val bundle = parcel as? Bundle ?: return@mapNotNull null
            val sender = bundle.getCharSequence("sender_name")?.toString().orEmpty()
            val text = bundle.getCharSequence("text")?.toString().orEmpty()
            if (text.isBlank()) null else if (sender.isBlank()) text else "$sender: $text"
        }
    }

    private fun Bundle.readAllTextExtras(maxDepth: Int): List<String> {
        if (maxDepth <= 0) return emptyList()
        return keySet().flatMap { key ->
            if (key.contains("icon", ignoreCase = true) || key.contains("intent", ignoreCase = true)) {
                return@flatMap emptyList()
            }
            when (val value = get(key)) {
                is CharSequence -> listOf(value.toString())
                is Bundle -> value.readAllTextExtras(maxDepth - 1)
                is Array<*> -> value.flatMap { element -> element.readTextValue(maxDepth - 1) }
                is Iterable<*> -> value.flatMap { element -> element.readTextValue(maxDepth - 1) }
                else -> emptyList()
            }
        }
    }

    private fun Any?.readTextValue(maxDepth: Int): List<String> = when (this) {
        null -> emptyList()
        is Bundle -> readAllTextExtras(maxDepth)
        is CharSequence -> listOf(toString())
        else -> listOf(toString())
    }
}
