package com.example.afetcomms.ui.messages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageStatus
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.util.MessageTtl
import kotlinx.coroutines.launch

data class MessagesUiState(
    val messages: List<MessageEntity> = emptyList(),
    val receivedCount: Int = 0,
    val outboxCount: Int = 0,
    val failedCount: Int = 0,
    val isEmpty: Boolean = true,
    val canRetryFailed: Boolean = false
)

class MessagesViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _uiState = MediatorLiveData(MessagesUiState())
    val uiState: LiveData<MessagesUiState> = _uiState

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    fun bindMessages(source: LiveData<List<MessageEntity>>) {
        _uiState.addSource(source) { all ->
            val now = System.currentTimeMillis()
            val active = all.filter {
                it.type != MessageType.PRESENCE && !MessageTtl.isExpired(it, now)
            }
            val failed = active.count { it.status == MessageStatus.FAILED }
            val outbox = active.count { it.status == MessageStatus.OUTBOX }
            val received = active.count { it.status == MessageStatus.RECEIVED }
            _uiState.value = MessagesUiState(
                messages = active,
                receivedCount = received,
                outboxCount = outbox,
                failedCount = failed,
                isEmpty = active.isEmpty(),
                canRetryFailed = failed > 0
            )
        }
    }

    fun refreshAndPurge() {
        viewModelScope.launch {
            app.messageRepository.deleteExpiredMessages()
            app.transportCoordinator.flushOutbox()
            _toastMessage.postValue(com.example.afetcomms.R.string.messages_refreshed)
        }
    }

    fun retryFailed() {
        app.transportCoordinator.retryFailed()
        _toastMessage.postValue(com.example.afetcomms.R.string.retry_started)
    }

    fun purgeOnOpen() {
        viewModelScope.launch {
            app.messageRepository.deleteExpiredMessages()
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }
}
