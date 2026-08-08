package com.example.afetcomms.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.data.local.MemberEntity
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.data.model.MessageStatus
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.FamilyCodeGenerator
import com.example.afetcomms.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.UUID

data class ProfileUiState(
    val userId: String,
    val userCode: String,
    val displayName: String,
    val familyId: String,
    val familyIdDisplay: String,
    val relation: MemberRelation
) {
    fun summaryText(): String =
        "$displayName · $userCode · ${FamilyCodeGenerator.formatFamilyCodeForDisplay(familyId)} · ${relation.storageValue}"
}

class MainViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _profile = MutableLiveData<ProfileUiState>()
    val profile: LiveData<ProfileUiState> = _profile

    private val _pendingCount = MutableLiveData(0)
    val pendingCount: LiveData<Int> = _pendingCount

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    val useFakeTransport: Boolean
        get() = app.useFakeTransport

    fun ensureLocalMemberOnBoard() {
        val profile = _profile.value ?: return
        viewModelScope.launch {
            app.memberRepository.ensureLocalMember(
                userId = profile.userCode,
                userCode = profile.userCode,
                displayName = profile.displayName,
                familyId = profile.familyId,
                relationRole = profile.relation.storageValue
            )
        }
    }

    fun announcePresence(coords: LocationHelper.Coordinates?) {
        app.announceFamilyPresence(coords?.latitude, coords?.longitude)
    }

    fun loadProfile(prefsName: String = AppPreferences.PREFS_NAME) {
        val prefs = app.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
        val familyId = prefs.getString(AppPreferences.KEY_FAMILY_CODE, "") ?: ""
        val userCode = prefs.getString(AppPreferences.KEY_USER_CODE, null)
            ?: prefs.getString(AppPreferences.KEY_USER_ID, "") ?: ""
        _profile.value = ProfileUiState(
            userId = userCode,
            userCode = userCode,
            displayName = prefs.getString(AppPreferences.KEY_USER_NAME, "") ?: "",
            familyId = familyId,
            familyIdDisplay = FamilyCodeGenerator.formatFamilyCodeForDisplay(familyId),
            relation = MemberRelation.fromStorage(
                prefs.getString(AppPreferences.KEY_MEMBER_RELATION, null)
            )
        )
        ensureLocalMemberOnBoard()
    }

    fun observeMembers(familyId: String): LiveData<List<MemberEntity>> {
        return app.memberRepository.getMembersByFamily(familyId)
    }

    fun refreshPendingCount() {
        val familyId = _profile.value?.familyId ?: return
        viewModelScope.launch {
            val outbox = app.messageRepository.countOutboxByFamily(familyId)
            val failed = app.messageRepository.countFailedByFamily(familyId)
            _pendingCount.postValue(outbox + failed)
        }
    }

    fun sendMessage(type: MessageType, baseContent: String, coords: LocationHelper.Coordinates?) {
        val profile = _profile.value ?: return
        val content = baseContent + LocationHelper.formatForMessage(coords)
        val message = MessageEntity(
            msgId = UUID.randomUUID().toString(),
            senderId = profile.userCode.ifBlank { profile.displayName },
            familyId = profile.familyId,
            type = type,
            content = content,
            createdAt = System.currentTimeMillis(),
            ttlSeconds = 300,
            priority = if (type == MessageType.SOS) 1 else 0,
            status = MessageStatus.OUTBOX,
            senderType = SenderType.CITIZEN.storageValue,
            senderDisplayName = profile.displayName
        )
        viewModelScope.launch {
            app.memberRepository.applyOutgoingMessage(
                userId = profile.userCode,
                userCode = profile.userCode,
                displayName = profile.displayName,
                familyId = profile.familyId,
                relationRole = profile.relation.storageValue,
                messageType = type,
                content = content
            )
            app.transportCoordinator.sendMessage(message)
            _toastMessage.postValue(
                when {
                    useFakeTransport && type == MessageType.SOS -> com.example.afetcomms.R.string.sos_sent_sim
                    useFakeTransport -> com.example.afetcomms.R.string.checkin_sent_sim
                    type == MessageType.SOS -> com.example.afetcomms.R.string.sos_sent
                    else -> com.example.afetcomms.R.string.checkin_sent
                }
            )
            refreshPendingCount()
        }
    }

    fun addMember(displayName: String, familyId: String, relation: MemberRelation) {
        viewModelScope.launch {
            val userCode = FamilyCodeGenerator.generateUserCode()
            app.memberRepository.insertMember(
                MemberEntity(
                    userId = userCode,
                    userCode = userCode,
                    displayName = displayName,
                    familyId = familyId,
                    relationRole = relation.storageValue
                )
            )
        }
    }

    fun setRescuerRole(prefsName: String) {
        val prefs = app.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(AppPreferences.KEY_USER_ROLE, "Rescuer").apply()
        loadProfile(prefsName)
    }

    fun consumeToast() {
        _toastMessage.value = null
    }
}
