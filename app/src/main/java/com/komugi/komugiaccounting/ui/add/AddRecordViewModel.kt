package com.komugi.komugiaccounting.ui.add

import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AddRecordViewModel(private val repository: AppDataRepository) {
    val data: StateFlow<AppData> = repository.data

    fun addMember(name: String) = repository.addMember(name)

    fun saveTemplate(
        type: RecordType,
        amountText: String,
        categoryId: String,
        memberId: String,
        remark: String
    ): String? {
        val current = data.value
        if (current.categories.none { it.id == categoryId && it.type == type && it.enabled }) return "请选择分类"
        if (current.members.none { it.id == memberId && it.enabled }) return "请选择成员"
        val amount = amountText.takeIf { it.isNotBlank() }?.let { AmountUtil.parseToCents(it) }
        if (amountText.isNotBlank() && amount == null) return "金额格式不正确"
        val categoryName = current.categories.firstOrNull { it.id == categoryId }?.name ?: "模板"
        val cleanRemark = remark.trim()
        val templateName = cleanRemark.takeIf { it.isNotBlank() } ?: categoryName
        repository.upsertTemplate(
            Template(
                id = UUID.randomUUID().toString(),
                name = templateName,
                type = type,
                amount = amount,
                categoryId = categoryId,
                memberId = memberId,
                remark = cleanRemark
            )
        )
        return null
    }

    fun saveRecord(
        type: RecordType,
        amountText: String,
        categoryId: String,
        memberId: String,
        dateTimeText: String,
        remark: String,
        recordId: String? = null
    ): String? {
        val current = data.value
        if (current.categories.none { it.id == categoryId && it.type == type && it.enabled }) return "请选择分类"
        if (current.members.none { it.id == memberId && it.enabled }) return "请选择成员"
        val amount = AmountUtil.parseToCents(amountText) ?: return "请输入大于 0 的金额"
        val dateTime = DateTimeUtil.parseDateTime(dateTimeText) ?: return "时间格式应为 yyyy-MM-dd HH:mm"
        val now = DateTimeUtil.now()
        val existing = recordId?.let { id -> current.records.firstOrNull { it.id == id } }
        val nextRecord = TransactionRecord(
            id = existing?.id ?: UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            categoryId = categoryId,
            memberId = memberId,
            remark = remark.trim(),
            dateTime = dateTime,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        if (existing == null) {
            repository.addRecord(nextRecord)
        } else {
            repository.updateRecord(nextRecord)
        }
        return null
    }

    fun record(recordId: String?): TransactionRecord? {
        if (recordId == null) return null
        return data.value.records.firstOrNull { it.id == recordId }
    }
}
