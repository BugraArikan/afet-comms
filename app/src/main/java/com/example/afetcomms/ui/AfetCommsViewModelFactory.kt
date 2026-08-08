package com.example.afetcomms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.ui.auth.FamilyIdSetupViewModel
import com.example.afetcomms.ui.auth.RescuerRegistrationViewModel
import com.example.afetcomms.ui.family.FamilyStatusViewModel
import com.example.afetcomms.ui.main.MainViewModel
import com.example.afetcomms.ui.messages.MessagesViewModel
import com.example.afetcomms.ui.rescuer.RescuerMainViewModel

class AfetCommsViewModelFactory(
    private val app: AfetCommsApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(app) as T
            modelClass.isAssignableFrom(MessagesViewModel::class.java) -> MessagesViewModel(app) as T
            modelClass.isAssignableFrom(RescuerRegistrationViewModel::class.java) -> RescuerRegistrationViewModel(app) as T
            modelClass.isAssignableFrom(FamilyIdSetupViewModel::class.java) -> FamilyIdSetupViewModel(app) as T
            modelClass.isAssignableFrom(RescuerMainViewModel::class.java) -> RescuerMainViewModel(app) as T
            modelClass.isAssignableFrom(FamilyStatusViewModel::class.java) -> FamilyStatusViewModel(app) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
