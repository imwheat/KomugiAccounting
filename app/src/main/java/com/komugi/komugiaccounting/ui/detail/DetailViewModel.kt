package com.komugi.komugiaccounting.ui.detail

import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import kotlinx.coroutines.flow.StateFlow

class DetailViewModel(private val repository: AppDataRepository) {
    val data: StateFlow<AppData> = repository.data
    fun deleteRecord(recordId: String) = repository.deleteRecord(recordId)
}
