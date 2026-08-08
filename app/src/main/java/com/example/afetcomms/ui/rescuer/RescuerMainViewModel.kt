package com.example.afetcomms.ui.rescuer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageStatus
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.UUID

data class RescuerProfileUiState(
    val displayName: String,
    val rescuerId: String,
    val organization: String
) {
    fun summary(): String = "$displayName · $rescuerId · $organization"
}

class RescuerMainViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _profile = MutableLiveData<RescuerProfileUiState>()
    val profile: LiveData<RescuerProfileUiState> = _profile

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    private val _selectedHelpCall = MutableLiveData<MessageEntity?>()
    val selectedHelpCall: LiveData<MessageEntity?> = _selectedHelpCall

    val nearbyHelpCalls: LiveData<List<MessageEntity>> =
        app.messageRepository.getNearbyHelpCalls()

    val useFakeTransport: Boolean
        get() = app.useFakeTransport

    fun loadProfile() {
        val prefs = app.getSharedPreferences(AppPreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        _profile.value = RescuerProfileUiState(
            displayName = prefs.getString(AppPreferences.KEY_USER_NAME, "") ?: "",
            rescuerId = prefs.getString(AppPreferences.KEY_USER_ID, "") ?: "",
            organization = prefs.getString(AppPreferences.KEY_ORGANIZATION_NAME, "") ?: ""
        )
    }

    fun selectHelpCall(message: MessageEntity) {
        _selectedHelpCall.value = message
    }

    fun sendSafeCheckin(content: String, coords: LocationHelper.Coordinates?) {
        val helpCall = _selectedHelpCall.value
        if (helpCall == null) {
            _toastMessage.postValue(R.string.rescuer_safe_select_first)
            return
        }
        val profile = _profile.value ?: return
        val fullContent = content + LocationHelper.formatForMessage(coords)
        val message = MessageEntity(
            msgId = UUID.randomUUID().toString(),
            senderId = profile.rescuerId,
            familyId = helpCall.familyId,
            type = MessageType.CHECKIN,
            content = fullContent,
            createdAt = System.currentTimeMillis(),
            ttlSeconds = 300,
            priority = 1,
            status = MessageStatus.OUTBOX,
            senderType = SenderType.RESCUER.storageValue,
            senderDisplayName = profile.displayName,
            rescuerId = profile.rescuerId
        )
        viewModelScope.launch {
            app.transportCoordinator.sendMessage(message, targetSenderId = helpCall.senderId)
            _toastMessage.postValue(
                if (useFakeTransport) R.string.rescuer_safe_sent_sim else R.string.rescuer_safe_sent
            )
        }
    }

    fun sendRescuerSos(baseContent: String, coords: LocationHelper.Coordinates?) {
        val profile = _profile.value ?: return
        val content = baseContent + LocationHelper.formatForMessage(coords)
        val message = MessageEntity(
            msgId = UUID.randomUUID().toString(),
            senderId = profile.rescuerId,
            familyId = profile.organization.ifBlank { "RESCUER_NET" },
            type = MessageType.SOS,
            content = content,
            createdAt = System.currentTimeMillis(),
            ttlSeconds = 300,
            priority = 1,
            status = MessageStatus.OUTBOX,
            senderType = SenderType.RESCUER.storageValue,
            senderDisplayName = profile.displayName,
            rescuerId = profile.rescuerId
        )
        viewModelScope.launch {
            app.transportCoordinator.sendMessage(message)
            _toastMessage.postValue(
                if (useFakeTransport) R.string.rescuer_sos_sent_sim else R.string.rescuer_sos_sent
            )
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }
}
