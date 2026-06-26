package com.komugi.komugiaccounting

object AppVisibilityTracker {
    @Volatile
    var isForeground: Boolean = false
        private set

    fun markForeground() {
        isForeground = true
    }

    fun markBackground() {
        isForeground = false
    }
}
