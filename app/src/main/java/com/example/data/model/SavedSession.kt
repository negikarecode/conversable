package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedSessionSkills(
    val empathy: Double,
    val listening: Double,
    val confidence: Double,
    val collaboration: Double
)

@JsonClass(generateAdapter = true)
data class SavedSessionMessage(
    val role: String,
    val content: String,
    val timestamp: String,
    val rapport_at_this_point: Int,
    val was_voice: Boolean
)

@JsonClass(generateAdapter = true)
data class SavedSession(
    val id: String,
    val date: String,
    val scenario_title: String,
    val scenario_category: String,
    val difficulty: String,
    val partner_name: String,
    val partner_gender: String,
    val duration_seconds: Int,
    val turn_count: Int,
    val social_score: Int,
    val rapport_final: Int,
    val rapport_trend: String,
    val skills: SavedSessionSkills,
    val strengths: List<String>,
    val improvements: List<String>,
    val tips: List<String>,
    val messages: List<SavedSessionMessage>,
    val badges_earned: List<String>,
    val xp_earned: Int,
    val voice_mode_used: Boolean
)
