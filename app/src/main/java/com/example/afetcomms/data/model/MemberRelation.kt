package com.example.afetcomms.data.model

import androidx.annotation.StringRes
import com.example.afetcomms.R

enum class MemberRelation(val storageValue: String, @StringRes val labelResId: Int) {
    ANNE("ANNE", R.string.relation_anne),
    BABA("BABA", R.string.relation_baba),
    COCUK("COCUK", R.string.relation_cocuk),
    KARDES("KARDES", R.string.relation_kardes),
    BUYUKANNE("BUYUKANNE", R.string.relation_buyukanne),
    BUYUKBABA("BUYUKBABA", R.string.relation_buyukbaba),
    DIGER("DIGER", R.string.relation_diger);

    companion object {
        fun fromStorage(value: String?): MemberRelation {
            return entries.find { it.storageValue == value } ?: DIGER
        }
    }
}
