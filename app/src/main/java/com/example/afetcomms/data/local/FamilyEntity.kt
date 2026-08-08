package com.example.afetcomms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey
    val familyCode: String,
    val inviteToken: String,
    val createdAtMillis: Long,
    val createdByUserId: String
)
