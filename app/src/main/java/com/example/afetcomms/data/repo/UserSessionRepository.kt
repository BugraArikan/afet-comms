package com.example.afetcomms.data.repo

import android.content.Context
import com.example.afetcomms.data.local.UserProfileDao
import com.example.afetcomms.data.local.UserProfileEntity
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.util.AppPreferences

class UserSessionRepository(
    private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val familyRepository: FamilyRepository? = null
) {

    suspend fun saveRescuerProfile(
        firstName: String,
        lastName: String,
        organizationName: String,
        rescuerId: String
    ) {
        val displayName = "$firstName $lastName".trim()
        val profile = UserProfileEntity(
            accountRole = AccountRole.RESCUER.storageValue,
            firstName = firstName,
            lastName = lastName,
            userId = rescuerId,
            userCode = rescuerId,
            familyId = organizationName,
            memberRelation = MemberRelation.DIGER.storageValue,
            organizationName = organizationName,
            displayName = displayName
        )
        persist(profile, legacyRole = "Rescuer")
    }

    suspend fun saveFamilyProfile(
        firstName: String,
        lastName: String,
        familyId: String,
        memberUserId: String,
        userCode: String,
        memberRelation: MemberRelation
    ) {
        val displayName = "$firstName $lastName".trim()
        val profile = UserProfileEntity(
            accountRole = AccountRole.FAMILY.storageValue,
            firstName = firstName,
            lastName = lastName,
            userId = memberUserId,
            userCode = userCode,
            familyId = familyId,
            memberRelation = memberRelation.storageValue,
            organizationName = null,
            displayName = displayName
        )
        persist(profile, legacyRole = memberRelation.storageValue)
    }

    suspend fun getProfile(): UserProfileEntity? = userProfileDao.getProfile()

    fun isSetupComplete(): Boolean {
        val prefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(AppPreferences.KEY_SETUP_COMPLETE, false)) return true
        val legacyName = prefs.getString(AppPreferences.KEY_USER_NAME, null)
        return !legacyName.isNullOrBlank()
    }

    fun getAccountRole(): AccountRole? {
        val prefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(AppPreferences.KEY_ACCOUNT_ROLE, null)
        val role = AccountRole.fromStorage(value)
        if (role != null) return role
        return if (prefs.getString(AppPreferences.KEY_USER_NAME, null) != null) {
            AccountRole.FAMILY
        } else {
            null
        }
    }

    suspend fun clearSession() {
        userProfileDao.clearProfile()
        familyRepository?.clearFamilies()
        context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private suspend fun persist(profile: UserProfileEntity, legacyRole: String) {
        userProfileDao.saveProfile(profile)
        context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(AppPreferences.KEY_SETUP_COMPLETE, true)
            .putString(AppPreferences.KEY_ACCOUNT_ROLE, profile.accountRole)
            .putString(AppPreferences.KEY_FIRST_NAME, profile.firstName)
            .putString(AppPreferences.KEY_LAST_NAME, profile.lastName)
            .putString(AppPreferences.KEY_ORGANIZATION_NAME, profile.organizationName)
            .putString(AppPreferences.KEY_USER_ID, profile.userId)
            .putString(AppPreferences.KEY_USER_CODE, profile.userCode)
            .putString(AppPreferences.KEY_FAMILY_CODE, profile.familyId)
            .putString(AppPreferences.KEY_MEMBER_RELATION, profile.memberRelation)
            .putString(AppPreferences.KEY_USER_NAME, profile.displayName)
            .putString(AppPreferences.KEY_USER_ROLE, legacyRole)
            .apply()
    }
}
