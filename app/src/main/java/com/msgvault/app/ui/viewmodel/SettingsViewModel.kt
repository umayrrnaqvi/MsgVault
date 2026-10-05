package com.msgvault.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msgvault.app.data.export.ExportFormat
import com.msgvault.app.data.export.ExportManager
import com.msgvault.app.data.repository.SmsRepository
import com.msgvault.app.data.worker.BackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val messageCount: Int = 0,
    val conversationCount: Int = 0,
    val autoBackupEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val exportUri: Uri? = null,
    val exportFormat: ExportFormat? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SmsRepository,
    private val exportManager: ExportManager,
    private val backupScheduler: BackupScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun exportAll(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)
            try {
                val uri = exportManager.exportAll(format)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportUri = uri,
                    exportFormat = format
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = e.message ?: "Export failed"
                )
            }
        }
    }

    fun clearExport() {
        _uiState.value = _uiState.value.copy(exportUri = null, exportFormat = null)
    }

    fun toggleAutoBackup() {
        val newState = !_uiState.value.autoBackupEnabled
        if (newState) {
            backupScheduler.schedulePeriodicBackup()
        } else {
            backupScheduler.cancelPeriodicBackup()
        }
        _uiState.value = _uiState.value.copy(autoBackupEnabled = newState)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAll()
            loadStats()
        }
    }

    fun createShareIntent(context: Context) {
        val uri = _uiState.value.exportUri ?: return
        val format = _uiState.value.exportFormat ?: return
        val intent = exportManager.createShareIntent(uri, format)
        context.startActivity(intent)
    }

    private fun loadStats() {
        viewModelScope.launch {
            val (messages, conversations) = repository.getStats()
            _uiState.value = _uiState.value.copy(
                messageCount = messages,
                conversationCount = conversations
            )
        }
    }
}
