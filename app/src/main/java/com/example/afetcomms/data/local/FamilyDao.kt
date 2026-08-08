package com.example.afetcomms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FamilyDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFamily(family: FamilyEntity)

    @Query("SELECT * FROM families WHERE familyCode = :familyCode LIMIT 1")
    suspend fun getFamilyByCode(familyCode: String): FamilyEntity?

    @Query("DELETE FROM families")
    suspend fun clearFamilies()
}
