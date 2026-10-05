package com.msgvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msgvault.app.data.db.entity.ConversationEntity
import com.msgvault.app.data.repository.SmsRepository
import com.msgvault.app.data.repository.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isSyncing: Boolean = false,
    val syncResult: SyncResult? = null,
    val error: String? = null,
    val messageCount: Int = 0,
    val conversationCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SmsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getConversations()
            else repository.searchConversations(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadStats()
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            try {
                val result = repository.syncAllMessages()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result
                )
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    fun syncNew() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            try {
                val result = repository.syncNewMessages()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result
                )
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSyncResult() {
        _uiState.value = _uiState.value.copy(syncResult = null)
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
