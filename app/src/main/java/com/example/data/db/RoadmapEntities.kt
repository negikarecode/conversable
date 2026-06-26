package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persona_memories")
data class PersonaMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personaId: String, // ID of the persona (e.g. clone name or scenario ID)
    val memoryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "marketplace_scenarios")
data class MarketplaceScenarioEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val partnerName: String,
    val partnerAvatar: String,
    val systemPrompt: String,
    val initialMessage: String,
    val isBookmarked: Boolean = false,
    val isPublished: Boolean = false,
    val creator: String = "Community",
    val rating: Float = 4.5f,
    val ratingCount: Int = 10,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "analyzed_chats")
data class AnalyzedChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String, // WhatsApp, Telegram, Discord, Email, Screenshot, CopyPaste
    val chatTitle: String,
    val rawText: String,
    val analysisResultJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_coaching_sessions")
data class LiveCoachingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goal: String,
    val scenario: String,
    val strategy: String,
    val conversationTranscript: String, // JSON transcript
    val suggestionsShown: Int = 0,
    val feedback: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "contact_reminders")
data class ContactReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationship: String, // Friend, Family, Boss, etc.
    val lastInteractionTimestamp: Long,
    val reminderIntervalDays: Int,
    val warmthScore: Int, // 0 to 100 warmth meter
    val notes: String = ""
)

@Entity(tableName = "group_meeting_sessions")
data class GroupMeetingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val participantsJson: String, // JSON array of participants
    val transcriptJson: String, // JSON array of meeting dialogue
    val score: Int,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocal_camera_sessions")
data class VocalCameraSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fillerWordCount: Int,
    val wpm: Int, // words per minute
    val gazeScore: Int, // gaze steady score percentage
    val postureFeedback: String,
    val speechClarityScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)
