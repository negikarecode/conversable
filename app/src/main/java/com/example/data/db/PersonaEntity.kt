package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persona_clones")
data class PersonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationshipType: String,
    val avatarUri: String? = null,
    val notes: String? = null,
    val rawChatData: String? = null,
    val communicationStyleScore: Int = 50,
    val humorScore: Int = 50,
    val friendlinessScore: Int = 50,
    val emotionalExpressivenessScore: Int = 50,
    val confidenceScore: Int = 50,
    val detailedSummary: String = "",
    val personalitySummary: String = "",
    val typicalVocabulary: String = "",
    val favoritePhrases: String = "",
    val responsePatterns: String = "",
    val emotionalTraits: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
