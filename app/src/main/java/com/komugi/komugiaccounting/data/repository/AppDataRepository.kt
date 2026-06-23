package com.komugi.komugiaccounting.data.repository

import android.content.Context
import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.AppSettings
import com.komugi.komugiaccounting.data.model.AutoBookRule
import com.komugi.komugiaccounting.data.model.AutoBookTodo
import com.komugi.komugiaccounting.data.model.AutomationFrequency
import com.komugi.komugiaccounting.data.model.AutomationRule
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.storage.JsonFileStorage
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.CsvTableBuilder
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

    init {
        runDueAutomationRules()
    }

    fun addRecord(record: TransactionRecord) = update { current ->
        current.copy(
            records = (current.records + record).sortedByDescending { it.dateTime },
            settings = current.settings.copy(
                lastExpenseCategoryId = if (record.type == RecordType.EXPENSE) record.categoryId else current.settings.lastExpenseCategoryId,
                lastIncomeCategoryId = if (record.type == RecordType.INCOME) record.categoryId else current.settings.lastIncomeCategoryId,
                lastMemberId = record.memberId,
                recentCategoryIds = (listOf(record.categoryId) + current.settings.recentCategoryIds)
                    .distinct()
                    .take(10)
            )
        )
    }

    fun updateRecord(record: TransactionRecord) = update { current ->
        current.copy(
            records = current.records.map { if (it.id == record.id) record else it }.sortedByDescending { it.dateTime },
            settings = current.settings.copy(
                lastExpenseCategoryId = if (record.type == RecordType.EXPENSE) record.categoryId else current.settings.lastExpenseCategoryId,
                lastIncomeCategoryId = if (record.type == RecordType.INCOME) record.categoryId else current.settings.lastIncomeCategoryId,
                lastMemberId = record.memberId,
                recentCategoryIds = (listOf(record.categoryId) + current.settings.recentCategoryIds)
                    .distinct()
                    .take(10)
            )
        )
    }

    fun deleteRecord(recordId: String) = update { current ->
        current.copy(records = current.records.filterNot { it.id == recordId })
    }

    fun setRecordRefunded(recordId: String, refunded: Boolean) = update { current ->
        current.copy(
            records = current.records.map { record ->
                if (record.id == recordId) {
                    record.copy(isRefunded = refunded, updatedAt = DateTimeUtil.now())
                } else {
                    record
                }
            }
        )
    }

    fun addMember(name: String): String? {
        val cleanName = name.trim()
        val current = _data.value
        if (cleanName.isEmpty()) return "成员名称不能为空"
        if (current.members.any { it.name == cleanName }) return "成员已经存在"
        update { data ->
            data.copy(members = data.members + Member(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                avatarColor = listOf("#42A5F5", "#EC407A", "#FFCA28", "#66BB6A", "#AB47BC").random()
            ))
        }
        return null
    }

    fun addMemberLegacy(name: String) = update { current ->
        val cleanName = name.trim()
        if (cleanName.isEmpty() || current.members.any { it.name == cleanName }) return@update current
        current.copy(members = current.members + Member(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            avatarColor = listOf("#42A5F5", "#EC407A", "#FFCA28", "#66BB6A", "#AB47BC").random()
        ))
    }

    fun updateMember(memberId: String, name: String, avatarColor: String? = null): String? {
        val cleanName = name.trim()
        val current = _data.value
        if (cleanName.isEmpty()) return "成员名称不能为空"
        if (current.members.any { it.id != memberId && it.name == cleanName }) return "成员已经存在"
        update { data ->
            data.copy(
                members = data.members.map { member ->
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
        return null
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

    fun addCategory(name: String, type: RecordType): String? = addCategory(name, type, if (type == RecordType.EXPENSE) "其他杂项" else "其他收入")

    fun addCategory(name: String, type: RecordType, groupName: String): String? {
        val cleanName = name.trim()
        val cleanGroupName = groupName.trim()
        val current = _data.value
        if (cleanName.isEmpty()) return "分类名称不能为空"
        if (cleanGroupName.isEmpty()) return "请选择所属分组"
        if (current.categories.any { it.type == type && it.groupName == cleanGroupName && it.name == cleanName }) return "分类已经存在"
        val nextOrder = (current.categories.filter { it.type == type }.maxOfOrNull { it.sortOrder } ?: 0) + 1
        update { data ->
            data.copy(
                categories = data.categories + Category(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    type = type,
                    iconName = cleanName.firstIconText(),
                    color = if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A",
                    sortOrder = nextOrder,
                    groupName = cleanGroupName,
                    isSystem = false
                )
            )
        }
        return null
    }

    fun addCategoryGroup(name: String, type: RecordType): String? {
        val cleanName = name.trim()
        val current = _data.value
        if (cleanName.isEmpty()) return "分组名称不能为空"
        if (current.categories.any { it.type == type && it.groupName == cleanName }) return "分组已经存在"
        val nextOrder = (current.categories.filter { it.type == type }.maxOfOrNull { it.sortOrder } ?: 0) + 1
        update { data ->
            data.copy(
                categories = data.categories + Category(
                    id = UUID.randomUUID().toString(),
                    name = "__group__$cleanName",
                    type = type,
                    iconName = cleanName.firstIconText(),
                    color = if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A",
                    sortOrder = nextOrder,
                    groupName = cleanName,
                    enabled = false,
                    isSystem = false
                )
            )
        }
        return null
    }

    fun updateCategory(categoryId: String, name: String): String? {
        val cleanName = name.trim()
        val current = _data.value
        val category = current.categories.firstOrNull { it.id == categoryId } ?: return "分类不存在"
        if (cleanName.isEmpty()) return "分类名称不能为空"
        if (current.categories.any { it.id != categoryId && it.type == category.type && it.groupName == category.groupName && it.name == cleanName }) return "分类已经存在"
        update { data -> data.copy(categories = data.categories.map { if (it.id == categoryId) it.copy(name = cleanName) else it }) }
        return null
    }

    fun updateCategoryStyle(categoryId: String, iconName: String, color: String, iconImageUri: String): String? {
        val cleanColor = color.trim()
        val current = _data.value
        if (current.categories.none { it.id == categoryId }) return "分类不存在"
        if (!cleanColor.matches(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$"))) return "颜色格式应为 #RRGGBB"
        update { data ->
            data.copy(
                categories = data.categories.map { category ->
                    if (category.id == categoryId) {
                        category.copy(
                            iconName = iconName.trim().ifBlank { category.displayNameForIcon().firstIconText() },
                            color = cleanColor,
                            iconImageUri = iconImageUri.trim()
                        )
                    } else {
                        category
                    }
                }
            )
        }
        return null
    }

    fun updateCategoryGroupStyle(type: RecordType, groupName: String, iconName: String, color: String, iconImageUri: String): String? {
        val cleanGroupName = groupName.trim()
        val cleanColor = color.trim()
        if (cleanGroupName.isEmpty()) return "分组不存在"
        if (!cleanColor.matches(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$"))) return "颜色格式应为 #RRGGBB"
        val current = _data.value
        if (current.categories.none { it.type == type && it.groupName == cleanGroupName }) return "分组不存在"
        update { data ->
            data.copy(
                categories = data.categories.map { category ->
                    if (category.type == type && category.groupName == cleanGroupName) {
                        category.copy(
                            iconName = iconName.trim().ifBlank { cleanGroupName.firstIconText() },
                            color = cleanColor,
                            iconImageUri = iconImageUri.trim()
                        )
                    } else {
                        category
                    }
                }
            )
        }
        return null
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

    fun upsertAutomationRule(rule: AutomationRule): String? {
        val cleanName = rule.name.trim()
        val current = _data.value
        if (cleanName.isEmpty()) return "请输入名称"
        if (rule.amount <= 0L) return "请输入金额"
        if (current.categories.none { it.id == rule.categoryId && it.type == rule.type }) return "请选择分类"
        if (current.members.none { it.id == rule.memberId }) return "请选择成员"
        val cleanRule = rule.copy(
            name = cleanName,
            remark = rule.remark.trim(),
            month = rule.month.coerceIn(1, 12),
            day = rule.day.coerceIn(1, 31)
        )
        update { data ->
            val exists = data.automationRules.any { it.id == cleanRule.id }
            data.copy(
                automationRules = if (exists) {
                    data.automationRules.map { if (it.id == cleanRule.id) cleanRule else it }
                } else {
                    data.automationRules + cleanRule
                }
            )
        }
        runDueAutomationRules()
        return null
    }

    fun deleteAutomationRule(ruleId: String) = update { current ->
        current.copy(automationRules = current.automationRules.filterNot { it.id == ruleId })
    }

    fun setAutomationRuleEnabled(ruleId: String, enabled: Boolean) = update { current ->
        current.copy(
            automationRules = current.automationRules.map { rule ->
                if (rule.id == ruleId) rule.copy(enabled = enabled) else rule
            }
        )
    }

    fun upsertAutoBookRule(rule: AutoBookRule): String? {
        val cleanName = rule.name.trim()
        val cleanPattern = rule.textPattern.trim()
        if (cleanName.isEmpty()) return "请输入规则名称"
        if (cleanPattern.isEmpty() || !cleanPattern.contains("XXX")) return "通知文本规则需要包含 XXX"
        val cleanRule = rule.copy(
            name = cleanName,
            titleKeyword = rule.titleKeyword.trim(),
            textPattern = cleanPattern
        )
        update { data ->
            val exists = data.autoBookRules.any { it.id == cleanRule.id }
            data.copy(
                autoBookRules = if (exists) {
                    data.autoBookRules.map { if (it.id == cleanRule.id) cleanRule else it }
                } else {
                    data.autoBookRules + cleanRule
                }
            )
        }
        return null
    }

    fun deleteAutoBookRule(ruleId: String) = update { current ->
        current.copy(autoBookRules = current.autoBookRules.filterNot { it.id == ruleId })
    }

    fun setAutoBookRuleEnabled(ruleId: String, enabled: Boolean) = update { current ->
        current.copy(autoBookRules = current.autoBookRules.map { if (it.id == ruleId) it.copy(enabled = enabled) else it })
    }

    fun handleNotification(title: String, text: String, postTime: Long) {
        update { current ->
            val newTodos = current.autoBookRules
                .filter { it.enabled }
                .mapNotNull { rule ->
                    if (rule.titleKeyword.isNotBlank() && !title.contains(rule.titleKeyword)) return@mapNotNull null
                    val amount = rule.extractAmount(text) ?: return@mapNotNull null
                    AutoBookTodo(
                        id = UUID.randomUUID().toString(),
                        ruleId = rule.id,
                        ruleName = rule.name,
                        type = rule.type,
                        amount = amount,
                        notificationTitle = title,
                        notificationText = text,
                        dateTime = postTime
                    )
                }
            if (newTodos.isEmpty()) current else current.copy(autoBookTodos = current.autoBookTodos + newTodos)
        }
    }

    fun ignoreAutoBookTodo(todoId: String) = update { current ->
        current.copy(autoBookTodos = current.autoBookTodos.filterNot { it.id == todoId })
    }

    fun addAutoBookTodoWithTemplate(todoId: String, templateId: String): String? {
        val current = _data.value
        val todo = current.autoBookTodos.firstOrNull { it.id == todoId } ?: return "待办不存在"
        val template = current.templates.firstOrNull { it.id == templateId } ?: return "模板不存在"
        if (template.type != todo.type) return "模板类型不匹配"
        val now = DateTimeUtil.now()
        val record = TransactionRecord(
            id = UUID.randomUUID().toString(),
            type = todo.type,
            amount = todo.amount,
            categoryId = template.categoryId,
            memberId = template.memberId,
            remark = template.remark.ifBlank { todo.ruleName },
            dateTime = todo.dateTime,
            createdAt = now,
            updatedAt = now
        )
        update { data ->
            data.copy(
                records = (data.records + record).sortedByDescending { it.dateTime },
                autoBookTodos = data.autoBookTodos.filterNot { it.id == todoId }
            )
        }
        return null
    }

    fun removeAutoBookTodo(todoId: String) = ignoreAutoBookTodo(todoId)

    fun runDueAutomationRules() {
        update { current ->
            val today = DateTimeUtil.formatDate(DateTimeUtil.now())
            val now = DateTimeUtil.now()
            val generatedRecords = mutableListOf<TransactionRecord>()
            val nextRules = current.automationRules.map { rule ->
                if (!rule.enabled || rule.lastRunDate == today || !rule.isDueToday(now)) {
                    rule
                } else {
                    generatedRecords += TransactionRecord(
                        id = UUID.randomUUID().toString(),
                        type = rule.type,
                        amount = rule.amount,
                        categoryId = rule.categoryId,
                        memberId = rule.memberId,
                        remark = rule.remark.ifBlank { rule.name },
                        dateTime = now,
                        createdAt = now,
                        updatedAt = now
                    )
                    rule.copy(lastRunDate = today)
                }
            }
            if (generatedRecords.isEmpty()) {
                current
            } else {
                current.copy(
                    records = (current.records + generatedRecords).sortedByDescending { it.dateTime },
                    automationRules = nextRules
                )
            }
        }
    }

    fun updateSettings(settings: AppSettings) = update { it.copy(settings = settings) }

    fun exportJson(): String = storage.exportJson(_data.value)

    fun exportRecordsCsv(startTime: Long? = null, endTime: Long? = null): String {
        val current = _data.value
        val categories = current.categories.associateBy { it.id }
        val members = current.members.associateBy { it.id }
        val records = current.exportRecords(startTime, endTime)
        val rows = buildList {
            add(listOf("日期", "时间", "类型", "分类", "成员", "金额", "退款", "备注"))
            records.forEach { record ->
                add(
                    listOf(
                        DateTimeUtil.formatDate(record.dateTime),
                        DateTimeUtil.formatTime(record.dateTime),
                        if (record.type == RecordType.INCOME) "收入" else "支出",
                        categories[record.categoryId]?.name ?: "未分类",
                        members[record.memberId]?.name ?: "未知成员",
                        AmountUtil.formatPlain(record.effectiveAmount),
                        if (record.isRefunded) "已退款" else "",
                        record.remark
                    )
                )
            }
        }
        return CsvTableBuilder.build(rows)
    }

    fun exportWorkbookXlsx(startTime: Long? = null, endTime: Long? = null): ByteArray {
        val current = _data.value
        val categories = current.categories.associateBy { it.id }
        val members = current.members.associateBy { it.id }
        val records = current.exportRecords(startTime, endTime)
        val detailRows = buildList {
            add(listOf("日期", "时间", "类型", "分类", "成员", "金额（元）", "退款", "备注"))
            records.forEach { record ->
                add(
                    listOf(
                        DateTimeUtil.formatDate(record.dateTime),
                        DateTimeUtil.formatTime(record.dateTime),
                        if (record.type == RecordType.INCOME) "收入" else "支出",
                        categories[record.categoryId]?.name ?: "未分类",
                        members[record.memberId]?.name ?: "未知成员",
                        AmountUtil.formatPlain(record.effectiveAmount),
                        if (record.isRefunded) "已退款" else "",
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
                    val income = records.filter { it.type == RecordType.INCOME }.sumOf { it.effectiveAmount }
                    val expense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.effectiveAmount }
                    add(listOf(month, AmountUtil.formatPlain(income), AmountUtil.formatPlain(expense), AmountUtil.formatPlain(income - expense)))
                }
        }
        val totalExpense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.effectiveAmount }.takeIf { it > 0L } ?: 1L
        val categoryRows = buildList {
            add(listOf("分类", "类型", "金额（元）", "占比"))
            records
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, records) ->
                    val category = categories[categoryId] ?: return@mapNotNull null
                    val amount = records.sumOf { it.effectiveAmount }
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

    private fun AutomationRule.isDueToday(now: Long): Boolean {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        return when (frequency) {
            AutomationFrequency.DAILY -> true
            AutomationFrequency.MONTHLY -> {
                val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                calendar.get(java.util.Calendar.DAY_OF_MONTH) == day.coerceAtMost(maxDay)
            }
            AutomationFrequency.YEARLY -> {
                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
                val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                currentMonth == month && calendar.get(java.util.Calendar.DAY_OF_MONTH) == day.coerceAtMost(maxDay)
            }
        }
    }

    private fun AutoBookRule.extractAmount(text: String): Long? {
        val parts = textPattern.split("XXX")
        if (parts.size != 2) return null
        val start = parts[0]
        val end = parts[1]
        val startIndex = if (start.isBlank()) 0 else text.indexOf(start).takeIf { it >= 0 }?.plus(start.length) ?: return null
        val endIndex = if (end.isBlank()) text.length else text.indexOf(end, startIndex).takeIf { it >= startIndex } ?: return null
        val amountText = text.substring(startIndex, endIndex).trim().replace(",", "")
        return AmountUtil.parseToCents(amountText)
    }

    private fun String.firstIconText(): String =
        trim().firstOrNull()?.toString().orEmpty()

    private fun Category.displayNameForIcon(): String =
        if (name.startsWith("__group__")) groupName else name

    private fun AppData.exportRecords(startTime: Long?, endTime: Long?): List<TransactionRecord> =
        records.asSequence()
            .filter { startTime == null || it.dateTime >= startTime }
            .filter { endTime == null || it.dateTime < endTime }
            .sortedByDescending { it.dateTime }
            .toList()

    companion object {
        @Volatile private var instance: AppDataRepository? = null

        fun get(context: Context): AppDataRepository = instance ?: synchronized(this) {
            instance ?: AppDataRepository(context).also { instance = it }
        }
    }
}
