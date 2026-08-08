package com.example.afetcomms.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.AuthNavigator
import com.example.afetcomms.util.FamilyCodeGenerator
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val app = application as AfetCommsApp
        val switchFake = findViewById<SwitchCompat>(R.id.switchFakeTransport)
        val switchSimAutoReply = findViewById<SwitchCompat>(R.id.switchSimAutoReply)
        val switchSimReplyCheckin = findViewById<SwitchCompat>(R.id.switchSimReplyCheckin)
        val switchAlerts = findViewById<SwitchCompat>(R.id.switchSosAlerts)
        val switchSound = findViewById<SwitchCompat>(R.id.switchSosSound)
        val edtUserId = findViewById<EditText>(R.id.edtSettingsUserId)
        val edtFamily = findViewById<EditText>(R.id.edtSettingsFamilyCode)
        val edtName = findViewById<EditText>(R.id.edtSettingsDisplayName)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val prefs = getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
        val isRescuer = app.userSessionRepository.getAccountRole() == AccountRole.RESCUER
        val isFamily = !isRescuer

        switchFake.isChecked = AppPreferences.useFakeTransport(this)
        switchSimAutoReply.isChecked = AppPreferences.simAutoReplyEnabled(this)
        switchSimReplyCheckin.isChecked = AppPreferences.simReplyToCheckIn(this)
        switchAlerts.isChecked = AppPreferences.sosAlertsEnabled(this)
        switchSound.isChecked = AppPreferences.sosSoundEnabled(this)

        val userCode = prefs.getString(AppPreferences.KEY_USER_CODE, null)
            ?: prefs.getString(AppPreferences.KEY_USER_ID, "")
        edtUserId.setText(userCode)
        edtName.setText(prefs.getString(AppPreferences.KEY_USER_NAME, ""))

        if (isFamily) {
            val familyCode = prefs.getString(AppPreferences.KEY_FAMILY_CODE, "") ?: ""
            edtFamily.setText(FamilyCodeGenerator.formatFamilyCodeForDisplay(familyCode))
            edtFamily.isEnabled = false
            edtFamily.hint = getString(R.string.settings_family_code_readonly)
            edtUserId.isEnabled = false
            edtUserId.hint = getString(R.string.settings_user_code_hint)
        } else {
            edtFamily.setText(prefs.getString(AppPreferences.KEY_ORGANIZATION_NAME, ""))
            edtFamily.hint = getString(R.string.hint_organization)
        }

        btnSave.setOnClickListener {
            val displayName = edtName.text.toString().trim()
            if (displayName.isEmpty()) {
                Toast.makeText(this, R.string.settings_validation, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = if (isFamily) {
                userCode.orEmpty()
            } else {
                edtUserId.text.toString().trim()
            }
            val codeOrOrg = if (isFamily) {
                prefs.getString(AppPreferences.KEY_FAMILY_CODE, "").orEmpty()
            } else {
                edtFamily.text.toString().trim()
            }

            if (!isFamily && (userId.isEmpty() || codeOrOrg.isEmpty())) {
                Toast.makeText(this, R.string.settings_validation, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fakeChanged = switchFake.isChecked != AppPreferences.useFakeTransport(this)

            prefs.edit()
                .putString(AppPreferences.KEY_USER_NAME, displayName)
                .putBoolean(AppPreferences.KEY_USE_FAKE_TRANSPORT, switchFake.isChecked)
                .putBoolean(AppPreferences.KEY_SIM_AUTO_REPLY, switchSimAutoReply.isChecked)
                .putBoolean(AppPreferences.KEY_SIM_REPLY_CHECKIN, switchSimReplyCheckin.isChecked)
                .putBoolean(AppPreferences.KEY_SOS_ALERTS_ENABLED, switchAlerts.isChecked)
                .putBoolean(AppPreferences.KEY_SOS_SOUND_ENABLED, switchSound.isChecked)
                .apply()

            if (!isFamily) {
                prefs.edit()
                    .putString(AppPreferences.KEY_USER_ID, userId)
                    .putString(AppPreferences.KEY_USER_CODE, userId)
                    .putString(AppPreferences.KEY_ORGANIZATION_NAME, codeOrOrg)
                    .putString(AppPreferences.KEY_FAMILY_CODE, codeOrOrg)
                    .apply()
            }

            AppPreferences.setUseFakeTransport(this, switchFake.isChecked)

            if (fakeChanged) {
                app.rebuildTransportCoordinator()
            }

            app.restartTransports(codeOrOrg, userId)

            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                app.stopTransports()
                app.familyRepository.clearFamilies()
                app.userSessionRepository.clearSession()
                runOnUiThread {
                    AuthNavigator.launchEntry(this@SettingsActivity)
                    finish()
                }
            }
        }
    }
}
