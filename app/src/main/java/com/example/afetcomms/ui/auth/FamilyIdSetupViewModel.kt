package com.example.afetcomms.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MemberEntity
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.data.repo.FamilyJoinResult
import com.example.afetcomms.util.FamilyCodeGenerator
import kotlinx.coroutines.launch

enum class FamilySetupMode { CREATE, JOIN }

data class GeneratedCodesUi(
    val familyCodeDisplay: String,
    val inviteToken: String,
    val userCode: String
)

class FamilyIdSetupViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _registrationComplete = MutableLiveData(false)
    val registrationComplete: LiveData<Boolean> = _registrationComplete

    private val _errorMessage = MutableLiveData<Int?>()
    val errorMessage: LiveData<Int?> = _errorMessage

    private val _generatedCodes = MutableLiveData<GeneratedCodesUi?>()
    val generatedCodes: LiveData<GeneratedCodesUi?> = _generatedCodes

    private var pendingFamilyCode: String? = null
    private var pendingInviteToken: String? = null
    private var pendingUserCode: String? = null

    fun createFamily(firstName: String, lastName: String, relation: MemberRelation) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = R.string.registration_validation
            return
        }
        viewModelScope.launch {
            val userCode = FamilyCodeGenerator.generateUserCode()
            val credentials = app.familyRepository.createFamily(userCode)
            pendingFamilyCode = credentials.familyCode
            pendingInviteToken = credentials.inviteToken
            pendingUserCode = userCode
            _generatedCodes.postValue(
                GeneratedCodesUi(
                    familyCodeDisplay = FamilyCodeGenerator.formatFamilyCodeForDisplay(credentials.familyCode),
                    inviteToken = credentials.inviteToken,
                    userCode = userCode
                )
            )
        }
    }

    fun finalizeCreate(firstName: String, lastName: String, relation: MemberRelation) {
        if (pendingFamilyCode == null || pendingUserCode == null) {
            _errorMessage.value = R.string.error_family_not_generated
            return
        }
        viewModelScope.launch {
            completeFamilyRegistration(firstName, lastName, relation)
        }
    }

    fun joinFamily(
        firstName: String,
        lastName: String,
        relation: MemberRelation,
        familyCodeInput: String,
        inviteTokenInput: String
    ) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = R.string.registration_validation
            return
        }
        viewModelScope.launch {
            when (val result = app.familyRepository.validateJoin(familyCodeInput, inviteTokenInput)) {
                is FamilyJoinResult.Success -> {
                    app.familyRepository.registerJoinedFamily(result.family)
                    pendingFamilyCode = result.family.familyCode
                    pendingInviteToken = result.family.inviteToken
                    pendingUserCode = FamilyCodeGenerator.generateUserCode()
                    completeFamilyRegistration(firstName, lastName, relation)
                }
                FamilyJoinResult.InvalidFamilyCodeFormat -> {
                    _errorMessage.postValue(R.string.error_invalid_family_code)
                }
                FamilyJoinResult.InvalidInviteToken -> {
                    _errorMessage.postValue(R.string.error_invalid_invite_token)
                }
                FamilyJoinResult.InviteMismatch -> {
                    _errorMessage.postValue(R.string.error_invite_mismatch)
                }
                FamilyJoinResult.FamilyAlreadyExists -> {
                    _errorMessage.postValue(R.string.error_family_exists)
                }
            }
        }
    }

    private suspend fun completeFamilyRegistration(
        firstName: String,
        lastName: String,
        relation: MemberRelation
    ) {
        val familyCode = pendingFamilyCode ?: return
        val userCode = pendingUserCode ?: return
        val displayName = "$firstName $lastName".trim()

        app.userSessionRepository.saveFamilyProfile(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            familyId = familyCode,
            memberUserId = userCode,
            userCode = userCode,
            memberRelation = relation
        )
        app.memberRepository.insertMember(
            MemberEntity(
                userId = userCode,
                userCode = userCode,
                displayName = displayName,
                familyId = familyCode,
                relationRole = relation.storageValue
            )
        )
        _registrationComplete.postValue(true)
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}
