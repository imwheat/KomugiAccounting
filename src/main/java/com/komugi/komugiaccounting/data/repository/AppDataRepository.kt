package com.komugi.komugiaccounting.data.repository

import android.content.Context
import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.AppSettings
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.storage.JsonFileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppDataRepository private constructor(context: Context) {
    private val storage = JsonFileStorage(context.applicationContext)
    private val _data = MutableStateFlow(storage.loadData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    fun addRecord(record: TransactionRecord) = update { current ->
        current.copy(
            records = (current.records + record).sortedByDescending { it.dateTime },
            settings = current.settings.copy(
                lastExpenseCategoryId = if (record.type == RecordType.EXPENSE) record.categoryId else current.settings.lastExpenseCategoryId,
                lastIncomeCategoryId = if (record.type == RecordType.INCOME) record.categoryId else current.settings.lastIncomeCategoryId,
                lastMemberId = record.memberId
            )
        )
    }

    fun deleteRecord(recordId: String) = update { current ->
        current.copy(records = current.records.filterNot { it.id == recordId })
    }

    fun addMember(name: String) = update { current ->
        val cleanName = name.trim()
        if (cleanName.isEmpty() || current.members.any { it.name == cleanName }) return@update current
        current.copy(members = current.members + Member(
            id = java.util.UUID.randomUUID().toString(),
            name = cleanName,
            avatarColor = listOf("#42A5F5", "#EC407A", "#FFCA28", "#66BB6A", "#AB47BC").random()
        ))
    }

    fun updateSettings(settings: AppSettings) = update { it.copy(settings = settings) }

    private fun update(block: (AppData) -> AppData) {
        val next = block(_data.value)
        _data.value = next
        storage.saveData(next)
    }

    companion object {
        @Volatile private var instance: AppDataRepository? = null

        fun get(context: Context): AppDataRepository = instance ?: synchronized(this) {
            instance ?: AppDataRepository(context).also { instance = it }
        }
    }
}
