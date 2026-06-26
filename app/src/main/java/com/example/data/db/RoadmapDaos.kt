package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaMemoryDao {
    @Query("SELECT * FROM persona_memories WHERE personaId = :personaId ORDER BY timestamp DESC")
    fun getMemoriesForPersona(personaId: String): Flow<List<PersonaMemoryEntity>>

    @Query("SELECT * FROM persona_memories WHERE personaId = :personaId ORDER BY timestamp DESC")
    suspend fun getMemoriesListForPersona(personaId: String): List<PersonaMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PersonaMemoryEntity): Long

    @Query("DELETE FROM persona_memories WHERE id = :id")
    suspend fun deleteMemory(id: Int)

    @Query("UPDATE persona_memories SET memoryText = :text WHERE id = :id")
    suspend fun updateMemory(id: Int, text: String)

    @Query("DELETE FROM persona_memories WHERE personaId = :personaId")
    suspend fun clearMemoriesForPersona(personaId: String)
}

@Dao
interface MarketplaceScenarioDao {
    @Query("SELECT * FROM marketplace_scenarios ORDER BY timestamp DESC")
    fun getAllMarketplaceScenarios(): Flow<List<MarketplaceScenarioEntity>>

    @Query("SELECT * FROM marketplace_scenarios WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedScenarios(): Flow<List<MarketplaceScenarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: MarketplaceScenarioEntity): Long

    @Query("UPDATE marketplace_scenarios SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("DELETE FROM marketplace_scenarios WHERE id = :id")
    suspend fun deleteScenario(id: String)
}

@Dao
interface AnalyzedChatDao {
    @Query("SELECT * FROM analyzed_chats ORDER BY timestamp DESC")
    fun getAllAnalyzedChats(): Flow<List<AnalyzedChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyzedChat(chat: AnalyzedChatEntity): Long

    @Query("DELETE FROM analyzed_chats WHERE id = :id")
    suspend fun deleteAnalyzedChat(id: Int)
}

@Dao
interface LiveCoachingSessionDao {
    @Query("SELECT * FROM live_coaching_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<LiveCoachingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LiveCoachingSessionEntity): Long

    @Query("DELETE FROM live_coaching_sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)
}

@Dao
interface ContactReminderDao {
    @Query("SELECT * FROM contact_reminders ORDER BY lastInteractionTimestamp DESC")
    fun getAllContactReminders(): Flow<List<ContactReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactReminder(reminder: ContactReminderEntity): Long

    @Query("DELETE FROM contact_reminders WHERE id = :id")
    suspend fun deleteContactReminder(id: Int)

    @Query("UPDATE contact_reminders SET warmthScore = :warmthScore, lastInteractionTimestamp = :lastTime WHERE id = :id")
    suspend fun updateWarmth(id: Int, warmthScore: Int, lastTime: Long)
}

@Dao
interface GroupMeetingSessionDao {
    @Query("SELECT * FROM group_meeting_sessions ORDER BY timestamp DESC")
    fun getAllGroupMeetingSessions(): Flow<List<GroupMeetingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMeetingSession(session: GroupMeetingSessionEntity): Long

    @Query("DELETE FROM group_meeting_sessions WHERE id = :id")
    suspend fun deleteGroupMeetingSession(id: Int)
}

@Dao
interface VocalCameraSessionDao {
    @Query("SELECT * FROM vocal_camera_sessions ORDER BY timestamp DESC")
    fun getAllVocalCameraSessions(): Flow<List<VocalCameraSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocalCameraSession(session: VocalCameraSessionEntity): Long

    @Query("DELETE FROM vocal_camera_sessions WHERE id = :id")
    suspend fun deleteVocalCameraSession(id: Int)
}
