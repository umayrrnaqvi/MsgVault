package com.msgvault.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msgvault.app.data.db.entity.MessageEntity
import com.msgvault.app.data.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: SmsRepository
) : ViewModel() {

    val address: String = savedStateHandle.get<String>("address") ?: ""
    val contactName: String = savedStateHandle.get<String>("contactName") ?: address

    val messages: StateFlow<List<MessageEntity>> = repository
        .getMessagesByAddress(address)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
