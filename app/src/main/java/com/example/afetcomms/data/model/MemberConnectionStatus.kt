package com.example.afetcomms.data.model

import androidx.annotation.StringRes
import com.example.afetcomms.R

enum class MemberConnectionStatus(val storageValue: String, @StringRes val labelResId: Int) {
    CONNECTED("CONNECTED", R.string.member_status_connected),
    AWAY("AWAY", R.string.member_status_away);

    companion object {
        fun fromStorage(value: String?): MemberConnectionStatus {
            return entries.find { it.storageValue == value } ?: AWAY
        }
    }
}
