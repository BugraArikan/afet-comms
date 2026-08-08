package com.example.afetcomms.util

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.absoluteValue

object FamilyCodeGenerator {

    private const val FAMILY_CODE_LENGTH = 8
    private const val INVITE_BODY_LENGTH = 4
    private const val INVITE_CHECKSUM_LENGTH = 2
    private const val USER_CODE_BODY_LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val PAIR_SALT = "AfetComms.FamilyPair.v1"

    private val secureRandom = SecureRandom()

    data class FamilyCredentials(
        val familyCode: String,
        val inviteToken: String
    )

    fun generateFamilyCredentials(): FamilyCredentials {
        val familyCode = randomCode(FAMILY_CODE_LENGTH)
        val inviteToken = generateInviteToken(familyCode)
        return FamilyCredentials(familyCode, inviteToken)
    }

    fun generateUserCode(): String {
        return "USR-${randomCode(USER_CODE_BODY_LENGTH)}"
    }

    fun generateInviteToken(familyCode: String): String {
        val body = randomCode(INVITE_BODY_LENGTH)
        val checksum = pairChecksum(familyCode, body)
        return body + checksum
    }

    fun isValidFamilyCode(code: String): Boolean {
        val normalized = normalizeFamilyCode(code)
        return normalized.length == FAMILY_CODE_LENGTH &&
            normalized.all { it in ALPHABET }
    }

    fun isValidInviteToken(familyCode: String, inviteToken: String): Boolean {
        val normalizedFamily = normalizeFamilyCode(familyCode)
        val normalizedInvite = normalizeInviteToken(inviteToken)
        if (!isValidFamilyCode(normalizedFamily)) return false
        if (normalizedInvite.length != INVITE_BODY_LENGTH + INVITE_CHECKSUM_LENGTH) return false
        if (!normalizedInvite.all { it in ALPHABET }) return false
        val body = normalizedInvite.take(INVITE_BODY_LENGTH)
        val checksum = normalizedInvite.takeLast(INVITE_CHECKSUM_LENGTH)
        return pairChecksum(normalizedFamily, body) == checksum
    }

    fun isValidUserCode(code: String): Boolean {
        val normalized = code.trim().uppercase()
        return normalized.matches(Regex("USR-[${ALPHABET}]{${USER_CODE_BODY_LENGTH}}"))
    }

    fun normalizeFamilyCode(code: String): String =
        code.trim().uppercase().replace("-", "").replace(" ", "")

    fun formatFamilyCodeForDisplay(code: String): String {
        val normalized = normalizeFamilyCode(code)
        if (normalized.length != FAMILY_CODE_LENGTH) return normalized
        return "${normalized.take(4)}-${normalized.drop(4)}"
    }

    fun normalizeInviteToken(token: String): String =
        token.trim().uppercase().replace("-", "").replace(" ", "")

    private fun randomCode(length: Int): String {
        return buildString(length) {
            repeat(length) {
                append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
            }
        }
    }

    private fun pairChecksum(familyCode: String, inviteBody: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$PAIR_SALT|$familyCode|$inviteBody".toByteArray())
        return buildString(INVITE_CHECKSUM_LENGTH) {
            repeat(INVITE_CHECKSUM_LENGTH) { index ->
                val byteIndex = index % digest.size
                append(ALPHABET[digest[byteIndex].toInt().absoluteValue % ALPHABET.length])
            }
        }
    }
}
