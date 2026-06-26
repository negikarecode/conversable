package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM persona_clones ORDER BY timestamp DESC")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity): Long

    @Query("SELECT * FROM persona_clones WHERE id = :id")
    suspend fun getPersonaById(id: Int): PersonaEntity?

    @Query("DELETE FROM persona_clones WHERE id = :id")
    suspend fun deletePersonaById(id: Int)
}
