package com.msgvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.msgvault.app.data.auth.UserProfile
import com.msgvault.app.data.auth.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

data class CloudConversation(
    val address: String = "",
    val contactName: String = "",
    val lastMessage: String = "",
    val lastMessageDate: Long = 0L,
    val messageCount: Int = 0
)

data class CloudMessage(
    val smsId: Long = 0L,
    val address: String = "",
    val body: String = "",
    val date: Long = 0L,
    val type: Int = 1
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _conversations = MutableStateFlow<List<CloudConversation>>(emptyList())
    val conversations: StateFlow<List<CloudConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<CloudMessage>>(emptyList())
    val messages: StateFlow<List<CloudMessage>> = _messages.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userProfileRepository.getAllUsers().collect { users ->
                _uiState.value = _uiState.value.copy(
                    users = users,
                    isLoading = false
                )
            }
        }
    }

    fun loadUserConversations(uid: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("conversations")
                    .orderBy("lastMessageDate", Query.Direction.DESCENDING)
                    .get().await()

                val convos = snapshot.documents.mapNotNull { doc ->
                    CloudConversation(
                        address = doc.getString("address") ?: "",
                        contactName = doc.getString("contactName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageDate = doc.getLong("lastMessageDate") ?: 0L,
                        messageCount = (doc.getLong("messageCount") ?: 0L).toInt()
                    )
                }
                _conversations.value = convos
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
            _isLoadingDetail.value = false
        }
    }

    fun loadUserMessages(uid: String, address: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("messages")
                    .whereEqualTo("address", address)
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get().await()

                val msgs = snapshot.documents.mapNotNull { doc ->
                    CloudMessage(
                        smsId = doc.getLong("smsId") ?: 0L,
                        address = doc.getString("address") ?: "",
                        body = doc.getString("body") ?: "",
                        date = doc.getLong("date") ?: 0L,
                        type = (doc.getLong("type") ?: 1L).toInt()
                    )
                }
                _messages.value = msgs
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
            _isLoadingDetail.value = false
        }
    }

    fun toggleDisabled(uid: String, disabled: Boolean) {
        viewModelScope.launch {
            try {
                userProfileRepository.toggleUserDisabled(uid, disabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            try {
                userProfileRepository.deleteUserProfile(uid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
