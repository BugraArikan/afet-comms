package com.example.afetcomms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.afetcomms.data.model.MemberConnectionStatus

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey
    val userId: String,
    val userCode: String,
    val displayName: String,
    val familyId: String,
    val relationRole: String,
    val connectionStatus: String = MemberConnectionStatus.AWAY.storageValue,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationAtMillis: Long? = null,
    val activeSos: Boolean = false,
    val lastSeenAtMillis: Long? = null
)
