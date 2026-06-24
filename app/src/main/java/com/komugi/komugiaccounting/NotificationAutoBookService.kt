package com.komugi.komugiaccounting

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.komugi.komugiaccounting.data.repository.AppDataRepository

class NotificationAutoBookService : NotificationListenerService() {
    companion object {
        @Volatile
        var instance: NotificationAutoBookService? = null
            private set

        data class NotificationData(
            val packageName: String,
            val title: String,
            val text: String,
            val postTime: Long,
            val key: String,
            val isClearable: Boolean,
            val iconResId: Int = 0
        )
    }

    interface NotificationListener {
        fun onNewNotification(data: NotificationData)
        fun onNotificationRemoved(key: String)
    }

    private val listeners = mutableListOf<NotificationListener>()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        listeners.clear()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        requestRebind(ComponentName(this, NotificationAutoBookService::class.java))
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val data = parseNotification(sbn) ?: return
        AppDataRepository.get(applicationContext).handleNotification(
            title = data.title,
            text = data.text,
            postTime = data.postTime
        )
        listeners.forEach { it.onNewNotification(data) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        listeners.forEach { it.onNotificationRemoved(sbn.key) }
    }

    fun getCurrentNotifications(): List<NotificationData> =
        runCatching {
            activeNotifications
                ?.mapNotNull(::parseNotification)
                .orEmpty()
                .sortedByDescending { it.postTime }
        }.getOrDefault(emptyList())

    fun addListener(listener: NotificationListener) {
        if (listener !in listeners) listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    private fun parseNotification(sbn: StatusBarNotification): NotificationData? {
        val notification = sbn.notification ?: return null
        if (sbn.packageName == packageName) return null

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
            .filter { it.isMeaningfulNotificationText(title) }
            .distinct()
            .joinToString("\n")

        if (title.isBlank() && text.isBlank()) return null

        return NotificationData(
            packageName = sbn.packageName,
            title = title.ifBlank { sbn.packageName },
            text = text,
            postTime = sbn.postTime,
            key = sbn.key,
            isClearable = sbn.isClearable,
            iconResId = notification.icon
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
                is Array<*> -> value.flatMap { it.readTextValue(maxDepth - 1) }
                is Iterable<*> -> value.flatMap { it.readTextValue(maxDepth - 1) }
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

    private fun String.isMeaningfulNotificationText(title: String): Boolean {
        if (isBlank() || this == title) return false
        val noiseParts = listOf(
            "android.app.Notification",
            "androidx.core.app.NotificationCompat",
            "android.app.PendingIntent",
            "android.graphics.drawable.Icon",
            "android.os.Bundle",
            "android.widget.RemoteViews",
            "Icon(typ=",
            "PendingIntent{",
            "Bundle["
        )
        if (noiseParts.any { contains(it) }) return false
        return true
    }
}
