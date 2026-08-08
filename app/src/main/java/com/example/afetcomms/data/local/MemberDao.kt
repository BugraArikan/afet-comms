package com.example.afetcomms.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Query("SELECT * FROM members WHERE userId = :userId LIMIT 1")
    suspend fun getMemberById(userId: String): MemberEntity?

    @Query("SELECT * FROM members WHERE familyId = :familyId ORDER BY displayName ASC")
    fun getMembersByFamily(familyId: String): LiveData<List<MemberEntity>>

    @Query(
        """
        SELECT * FROM members WHERE familyId = :familyId
        ORDER BY activeSos DESC, displayName ASC
        """
    )
    fun observeFamilyStatusBoard(familyId: String): LiveData<List<MemberEntity>>

    @Query("DELETE FROM members WHERE userId = :userId")
    suspend fun deleteMember(userId: String)

    @Query(
        """
        UPDATE members SET
            connectionStatus = :connectionStatus,
            lastSeenAtMillis = :lastSeenAtMillis,
            lastLatitude = :lastLatitude,
            lastLongitude = :lastLongitude,
            lastLocationAtMillis = :lastLocationAtMillis,
            activeSos = :activeSos
        WHERE userId = :userId
        """
    )
    suspend fun updateMemberPresence(
        userId: String,
        connectionStatus: String,
        lastSeenAtMillis: Long,
        lastLatitude: Double?,
        lastLongitude: Double?,
        lastLocationAtMillis: Long?,
        activeSos: Boolean
    )

    @Query(
        """
        UPDATE members SET connectionStatus = :awayStatus
        WHERE familyId = :familyId
        AND (lastSeenAtMillis IS NULL OR lastSeenAtMillis < :cutoffMillis)
        AND connectionStatus = :connectedStatus
        """
    )
    suspend fun markStaleMembersAway(
        familyId: String,
        cutoffMillis: Long,
        connectedStatus: String,
        awayStatus: String
    )

    @Query("UPDATE members SET activeSos = 0 WHERE userId = :userId")
    suspend fun clearActiveSos(userId: String)
}
