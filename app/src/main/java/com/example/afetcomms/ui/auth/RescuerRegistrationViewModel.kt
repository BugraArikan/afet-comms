package com.example.afetcomms.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MemberEntity
import kotlinx.coroutines.launch

class RescuerRegistrationViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _registrationComplete = MutableLiveData(false)
    val registrationComplete: LiveData<Boolean> = _registrationComplete

    private val _errorMessage = MutableLiveData<Int?>()
    val errorMessage: LiveData<Int?> = _errorMessage

    fun register(
        firstName: String,
        lastName: String,
        organization: String,
        rescuerId: String
    ) {
        if (firstName.isBlank() || lastName.isBlank() || organization.isBlank() || rescuerId.isBlank()) {
            _errorMessage.value = R.string.registration_validation
            return
        }
        viewModelScope.launch {
            app.userSessionRepository.saveRescuerProfile(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                organizationName = organization.trim(),
                rescuerId = rescuerId.trim().uppercase()
            )
            app.memberRepository.insertMember(
                MemberEntity(
                    userId = rescuerId.trim().uppercase(),
                    userCode = rescuerId.trim().uppercase(),
                    displayName = "$firstName $lastName".trim(),
                    familyId = organization.trim(),
                    relationRole = "DIGER"
                )
            )
            _registrationComplete.postValue(true)
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}
