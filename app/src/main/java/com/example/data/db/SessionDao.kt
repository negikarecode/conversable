package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM conversation_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession)

    @Query("DELETE FROM conversation_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("SELECT AVG(score) FROM conversation_sessions")
    fun getAverageScore(): Flow<Float?>

    @Query("SELECT COUNT(*) FROM conversation_sessions")
    fun getSessionCount(): Flow<Int>
}
