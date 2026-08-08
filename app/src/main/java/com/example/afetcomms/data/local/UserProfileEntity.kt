package com.example.afetcomms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val profileId: Int = SINGLE_PROFILE_ID,
    val accountRole: String,
    val firstName: String,
    val lastName: String,
    val userId: String,
    val userCode: String,
    val familyId: String,
    val memberRelation: String,
    val organizationName: String?,
    val displayName: String
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}
