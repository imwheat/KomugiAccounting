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
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import com.komugi.komugiaccounting.util.XlsxWorkbookBuilder
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

    fun deleteMember(memberId: String): String? {
        val current = _data.value
        val member = current.members.firstOrNull { it.id == memberId } ?: return "成员不存在"
        if (member.isSystem) return "系统预置成员不可删除，可选择禁用"
        if (current.records.any { it.memberId == memberId }) return "已有账单关联该成员，不能删除"
        if (current.templates.any { it.memberId == memberId }) return "已有模板关联该成员，不能删除"
        update { data -> data.copy(members = data.members.filterNot { it.id == memberId }) }
        return null
    }

    fun setCategoryEnabled(categoryId: String, enabled: Boolean) = update { current ->
        current.copy(categories = current.categories.map { if (it.id == categoryId) it.copy(enabled = enabled) else it })
    }

    fun deleteCategory(categoryId: String): String? {
        val current = _data.value
        val category = current.categories.firstOrNull { it.id == categoryId } ?: return "分类不存在"
        if (category.isSystem) return "系统预置分类不可删除，可选择禁用"
        if (current.records.any { it.categoryId == categoryId }) return "已有账单关联该分类，不能删除"
        if (current.templates.any { it.categoryId == categoryId }) return "已有模板关联该分类，不能删除"
        update { data -> data.copy(categories = data.categories.filterNot { it.id == categoryId }) }
        return null
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

    fun moveCategory(categoryId: String, direction: Int) = update { current ->
        if (direction == 0) return@update current
        val category = current.categories.firstOrNull { it.id == categoryId } ?: return@update current
        val ordered = current.categories
            .filter { it.type == category.type }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        val index = ordered.indexOfFirst { it.id == categoryId }
        val targetIndex = (index + direction).coerceIn(0, ordered.lastIndex)
        if (index < 0 || index == targetIndex) return@update current
        val target = ordered[targetIndex]
        current.copy(
            categories = current.categories.map {
                when (it.id) {
                    category.id -> it.copy(sortOrder = target.sortOrder)
                    target.id -> it.copy(sortOrder = category.sortOrder)
                    else -> it
                }
            }
        )
    }

    fun reorderCategories(type: RecordType, orderedIds: List<String>) = update { current ->
        val sortMap = orderedIds.withIndex().associate { (index, id) -> id to index + 1 }
        current.copy(
            categories = current.categories.map { category ->
                if (category.type == type && category.id in sortMap) {
                    category.copy(sortOrder = sortMap.getValue(category.id))
                } else {
                    category
                }
            }
        )
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

    fun exportRecordsCsv(startTime: Long? = null, endTime: Long? = null): String {
        val current = _data.value
        val categories = current.categories.associateBy { it.id }
        val members = current.members.associateBy { it.id }
        val records = current.exportRecords(startTime, endTime)
        val rows = buildList {
            add(listOf("日期", "时间", "类型", "分类", "成员", "金额", "备注"))
            records.forEach { record ->
                add(
                    listOf(
                        DateTimeUtil.formatDate(record.dateTime),
                        DateTimeUtil.formatTime(record.dateTime),
                        if (record.type == RecordType.INCOME) "收入" else "支出",
                        categories[record.categoryId]?.name ?: "未分类",
                        members[record.memberId]?.name ?: "未知成员",
                        AmountUtil.formatPlain(record.amount),
                        record.remark
                    )
                )
            }
        }
        return "\uFEFF" + rows.joinToString("\n") { row -> row.joinToString(",") { it.csvCell() } }
    }

    fun exportWorkbookXlsx(startTime: Long? = null, endTime: Long? = null): ByteArray {
        val current = _data.value
        val categories = current.categories.associateBy { it.id }
        val members = current.members.associateBy { it.id }
        val records = current.exportRecords(startTime, endTime)
        val detailRows = buildList {
            add(listOf("日期", "时间", "类型", "分类", "成员", "金额（元）", "备注"))
            records.forEach { record ->
                add(
                    listOf(
                        DateTimeUtil.formatDate(record.dateTime),
                        DateTimeUtil.formatTime(record.dateTime),
                        if (record.type == RecordType.INCOME) "收入" else "支出",
                        categories[record.categoryId]?.name ?: "未分类",
                        members[record.memberId]?.name ?: "未知成员",
                        AmountUtil.formatPlain(record.amount),
                        record.remark
                    )
                )
            }
        }
        val monthRows = buildList {
            add(listOf("月份", "收入（元）", "支出（元）", "结余（元）"))
            records
                .groupBy { DateTimeUtil.formatDate(it.dateTime).substring(0, 7) }
                .toSortedMap(compareByDescending { it })
                .forEach { (month, records) ->
                    val income = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
                    val expense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
                    add(listOf(month, AmountUtil.formatPlain(income), AmountUtil.formatPlain(expense), AmountUtil.formatPlain(income - expense)))
                }
        }
        val totalExpense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }.takeIf { it > 0L } ?: 1L
        val categoryRows = buildList {
            add(listOf("分类", "类型", "金额（元）", "占比"))
            records
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, records) ->
                    val category = categories[categoryId] ?: return@mapNotNull null
                    val amount = records.sumOf { it.amount }
                    val percent = if (category.type == RecordType.EXPENSE) amount * 100.0 / totalExpense else 0.0
                    listOf(
                        category.name,
                        if (category.type == RecordType.INCOME) "收入" else "支出",
                        AmountUtil.formatPlain(amount),
                        if (category.type == RecordType.EXPENSE) "%.2f%%".format(percent) else "-"
                    )
                }
                .sortedByDescending { it[2].toDoubleOrNull() ?: 0.0 }
                .forEach(::add)
        }

        return XlsxWorkbookBuilder.build(
            listOf(
                "账单明细" to detailRows,
                "月度汇总" to monthRows,
                "分类汇总" to categoryRows
            )
        )
    }

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

    private fun AppData.exportRecords(startTime: Long?, endTime: Long?): List<TransactionRecord> =
        records.asSequence()
            .filter { startTime == null || it.dateTime >= startTime }
            .filter { endTime == null || it.dateTime < endTime }
            .sortedByDescending { it.dateTime }
            .toList()

    private fun String.csvCell(): String {
        val escaped = replace("\"", "\"\"")
        return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
    }

    companion object {
        @Volatile private var instance: AppDataRepository? = null

        fun get(context: Context): AppDataRepository = instance ?: synchronized(this) {
            instance ?: AppDataRepository(context).also { instance = it }
        }
    }
}
