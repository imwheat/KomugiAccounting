package com.komugi.komugiaccounting.data.repository

import android.content.Context
import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.AppSettings
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.storage.JsonFileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    fun updateRecord(record: TransactionRecord) = update { current ->
        current.copy(
            records = current.records.map { if (it.id == record.id) record else it }.sortedByDescending { it.dateTime },
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
            id = UUID.randomUUID().toString(),
            name = cleanName,
            avatarColor = listOf("#42A5F5", "#EC407A", "#FFCA28", "#66BB6A", "#AB47BC").random()
        ))
    }

    fun updateMember(memberId: String, name: String, avatarColor: String? = null) = update { current ->
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@update current
        current.copy(
            members = current.members.map { member ->
                if (member.id == memberId) {
                    member.copy(
                        name = cleanName,
                        avatarColor = avatarColor?.takeIf { it.isNotBlank() } ?: member.avatarColor
                    )
                } else {
                    member
                }
            }
        )
    }

    fun setMemberEnabled(memberId: String, enabled: Boolean) = update { current ->
        current.copy(members = current.members.map { if (it.id == memberId) it.copy(enabled = enabled) else it })
    }

    fun setCategoryEnabled(categoryId: String, enabled: Boolean) = update { current ->
        current.copy(categories = current.categories.map { if (it.id == categoryId) it.copy(enabled = enabled) else it })
    }

    fun addCategory(name: String, type: RecordType) = update { current ->
        val cleanName = name.trim()
        if (cleanName.isEmpty() || current.categories.any { it.type == type && it.name == cleanName }) return@update current
        val nextOrder = (current.categories.filter { it.type == type }.maxOfOrNull { it.sortOrder } ?: 0) + 1
        current.copy(
            categories = current.categories + Category(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                type = type,
                iconName = "MoreHoriz",
                color = if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A",
                sortOrder = nextOrder,
                isSystem = false
            )
        )
    }

    fun updateCategory(categoryId: String, name: String) = update { current ->
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@update current
        current.copy(categories = current.categories.map { if (it.id == categoryId) it.copy(name = cleanName) else it })
    }

    fun upsertTemplate(template: Template) = update { current ->
        val cleanName = template.name.trim()
        if (cleanName.isEmpty()) return@update current
        val cleanTemplate = template.copy(name = cleanName, remark = template.remark.trim())
        val exists = current.templates.any { it.id == template.id }
        current.copy(
            templates = if (exists) {
                current.templates.map { if (it.id == template.id) cleanTemplate else it }
            } else {
                current.templates + cleanTemplate
            }
        )
    }

    fun deleteTemplate(templateId: String) = update { current ->
        current.copy(templates = current.templates.filterNot { it.id == templateId })
    }

    fun updateSettings(settings: AppSettings) = update { it.copy(settings = settings) }

    fun exportJson(): String = storage.exportJson(_data.value)

    fun importJson(jsonText: String): Result<Unit> = runCatching {
        val imported = storage.importJson(jsonText)
        _data.value = imported
        storage.saveData(imported)
    }

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
