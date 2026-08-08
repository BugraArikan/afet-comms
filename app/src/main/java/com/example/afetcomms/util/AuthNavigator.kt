package com.example.afetcomms.util

import android.content.Context
import android.content.Intent
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.data.repo.UserSessionRepository
import com.example.afetcomms.ui.auth.RoleSelectionActivity
import com.example.afetcomms.ui.main.MainActivity
import com.example.afetcomms.ui.rescuer.RescuerMainActivity

object AuthNavigator {

    fun launchEntry(context: Context) {
        val session = UserSessionRepository(
            context,
            com.example.afetcomms.data.local.AppDatabase.getDatabase(context).userProfileDao()
        )
        val intent = if (!session.isSetupComplete()) {
            Intent(context, RoleSelectionActivity::class.java)
        } else {
            homeIntent(context, session.getAccountRole())
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun homeIntent(context: Context, role: AccountRole?): Intent {
        return when (role) {
            AccountRole.RESCUER -> Intent(context, RescuerMainActivity::class.java)
            AccountRole.FAMILY, null -> Intent(context, MainActivity::class.java)
        }
    }

    fun requireSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(AppPreferences.KEY_SETUP_COMPLETE, false)
    }
}
