package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_sessions")
data class ConversationSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioId: String,
    val scenarioTitle: String,
    val category: String,
    val partnerName: String,
    val transcriptJson: String, // Stored as a JSON string of messages
    val score: Int,
    val empathyScore: Int,
    val flowScore: Int,
    val goalAchievementScore: Int,
    val whatWentWell: String,
    val missedOpportunities: String,
    val replayTips: String,
    val suggestionsShown: Int = 0,
    val suggestionsUsed: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
