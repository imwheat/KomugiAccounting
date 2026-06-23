package com.komugi.komugiaccounting.data.model

data class AutomationRule(
    val id: String,
    val name: String,
    val type: RecordType,
    val frequency: AutomationFrequency,
    val month: Int = 1,
    val day: Int = 1,
    val amount: Long,
    val categoryId: String,
    val memberId: String,
    val remark: String = "",
    val enabled: Boolean = true,
    val lastRunDate: String? = null
)

enum class AutomationFrequency {
    DAILY,
    MONTHLY,
    YEARLY
}
