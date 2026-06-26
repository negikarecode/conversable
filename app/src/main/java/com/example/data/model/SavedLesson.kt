package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class SavedLesson(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "My Biggest Mistakes", "My Best Replies", "Best Conversation Openers"
    val scenarioTitle: String,
    val partnerName: String,
    val originalMessage: String,
    val expertMessage: String,
    val coachAnalysis: String,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis()
)
