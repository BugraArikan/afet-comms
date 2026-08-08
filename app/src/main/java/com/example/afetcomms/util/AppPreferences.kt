package com.example.afetcomms.util

import android.content.Context
import com.example.afetcomms.BuildConfig

object AppPreferences {
    const val PREFS_NAME = "user_prefs"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_CODE = "user_code"
    const val KEY_MEMBER_RELATION = "member_relation"
    const val KEY_FAMILY_CODE = "family_code"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_SETUP_COMPLETE = "setup_complete"
    const val KEY_ACCOUNT_ROLE = "account_role"
    const val KEY_FIRST_NAME = "first_name"
    const val KEY_LAST_NAME = "last_name"
    const val KEY_ORGANIZATION_NAME = "organization_name"
    const val KEY_USE_FAKE_TRANSPORT = "use_fake_transport"
    const val KEY_SOS_ALERTS_ENABLED = "sos_alerts_enabled"
    const val KEY_SOS_SOUND_ENABLED = "sos_sound_enabled"
    const val KEY_SIM_AUTO_REPLY = "sim_auto_reply"
    /** true = Güvendeyim'e de sahte yanıt; false = yalnızca SOS */
    const val KEY_SIM_REPLY_CHECKIN = "sim_reply_checkin"

    fun simAutoReplyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SIM_AUTO_REPLY, true)
    }

    fun simReplyToCheckIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SIM_REPLY_CHECKIN, false)
    }

    fun useFakeTransport(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_USE_FAKE_TRANSPORT)) {
            prefs.getBoolean(KEY_USE_FAKE_TRANSPORT, BuildConfig.USE_FAKE_TRANSPORT)
        } else {
            BuildConfig.USE_FAKE_TRANSPORT
        }
    }

    fun setUseFakeTransport(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_USE_FAKE_TRANSPORT, enabled)
            .apply()
    }

    fun sosAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOS_ALERTS_ENABLED, true)
    }

    fun sosSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOS_SOUND_ENABLED, true)
    }
}
