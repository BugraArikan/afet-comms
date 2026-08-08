package com.example.afetcomms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE profileId = :id LIMIT 1")
    suspend fun getProfile(id: Int = UserProfileEntity.SINGLE_PROFILE_ID): UserProfileEntity?

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}
