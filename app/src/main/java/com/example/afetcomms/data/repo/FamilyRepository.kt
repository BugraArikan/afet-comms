package com.example.afetcomms.data.repo

import com.example.afetcomms.data.local.FamilyDao
import com.example.afetcomms.data.local.FamilyEntity
import com.example.afetcomms.util.FamilyCodeGenerator

sealed class FamilyJoinResult {
    data class Success(val family: FamilyEntity) : FamilyJoinResult()
    data object InvalidFamilyCodeFormat : FamilyJoinResult()
    data object InvalidInviteToken : FamilyJoinResult()
    data object InviteMismatch : FamilyJoinResult()
    data object FamilyAlreadyExists : FamilyJoinResult()
}

class FamilyRepository(private val familyDao: FamilyDao) {

    suspend fun createFamily(createdByUserId: String): FamilyCodeGenerator.FamilyCredentials {
        var credentials = FamilyCodeGenerator.generateFamilyCredentials()
        var attempts = 0
        while (familyDao.getFamilyByCode(credentials.familyCode) != null && attempts < 8) {
            credentials = FamilyCodeGenerator.generateFamilyCredentials()
            attempts++
        }
        familyDao.insertFamily(
            FamilyEntity(
                familyCode = credentials.familyCode,
                inviteToken = credentials.inviteToken,
                createdAtMillis = System.currentTimeMillis(),
                createdByUserId = createdByUserId
            )
        )
        return credentials
    }

    suspend fun validateJoin(familyCodeInput: String, inviteTokenInput: String): FamilyJoinResult {
        val familyCode = FamilyCodeGenerator.normalizeFamilyCode(familyCodeInput)
        val inviteToken = FamilyCodeGenerator.normalizeInviteToken(inviteTokenInput)

        if (!FamilyCodeGenerator.isValidFamilyCode(familyCode)) {
            return FamilyJoinResult.InvalidFamilyCodeFormat
        }
        if (!FamilyCodeGenerator.isValidInviteToken(familyCode, inviteToken)) {
            return FamilyJoinResult.InvalidInviteToken
        }

        val existing = familyDao.getFamilyByCode(familyCode)
        if (existing != null) {
            return if (existing.inviteToken == inviteToken) {
                FamilyJoinResult.Success(existing)
            } else {
                FamilyJoinResult.InviteMismatch
            }
        }

        return FamilyJoinResult.Success(
            FamilyEntity(
                familyCode = familyCode,
                inviteToken = inviteToken,
                createdAtMillis = System.currentTimeMillis(),
                createdByUserId = ""
            )
        )
    }

    suspend fun registerJoinedFamily(family: FamilyEntity) {
        if (familyDao.getFamilyByCode(family.familyCode) == null) {
            familyDao.insertFamily(family)
        }
    }

    suspend fun getFamily(familyCode: String): FamilyEntity? {
        return familyDao.getFamilyByCode(
            FamilyCodeGenerator.normalizeFamilyCode(familyCode)
        )
    }

    suspend fun clearFamilies() {
        familyDao.clearFamilies()
    }
}
