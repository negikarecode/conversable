package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.db.ConversationSession
import com.example.data.db.SessionRepository
import com.example.data.db.PersonaEntity
import com.example.data.db.PersonaRepository
import com.example.data.model.ChatMessage
import com.example.data.model.Scenario
import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeCatalog
import com.example.data.model.CompletedDailyChallenge
import java.util.Calendar
import com.example.data.model.LanguageCatalog
import com.example.data.model.SupportedLanguage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.SharedPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.speech.tts.TextToSpeech
import android.os.Bundle


@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PartnerApiResponse(
    val text: String,
    val rapport: Int,
    val correction: String? = null
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CoachApiResponse(
    val overallScore: Int,
    val empathyScore: Int,
    val flowScore: Int,
    val goalAchievementScore: Int,
    val whatWentWell: String,
    val missedOpportunities: String,
    val replayTips: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class RealTimeCoachTip(
    val rapport_score: Int,
    val rapport_delta: Int,
    val signal: String,
    val micro_tip: String,
    val engagement_trend: String
)

// --- FEATURE 7: Live Relationship Meter Models ---
data class RelationshipMetricDetail(
    val name: String,
    val score: Int,
    val delta: Int = 0,
    val explanation: String = "Conversation has just started. Keep chatting to build rapport!"
)

data class RelationshipTimelinePoint(
    val messageId: String,
    val userMessageText: String,
    val partnerResponseText: String,
    val timestamp: Long,
    val overallConnection: Int,
    val mood: String,
    val metrics: Map<String, RelationshipMetricDetail>
)

data class RelationshipAchievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BonusItem(val label: String, val amount: Int)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class XpEarned(val base: Int, val bonuses: List<BonusItem>, val total_this_session: Int)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ProgressionStreak(val previous: Int, val new: Int, val streak_broken: Boolean, val streak_freeze_used: Boolean)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BadgeUnlocked(val id: String, val name: String, val reason: String)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class SkillDeltas(val empathy: Double, val listening: Double, val confidence: Double, val collaboration: Double)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ScenarioRecommendation(val title: String, val reason: String)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ProgressionResult(
    val xp_earned: XpEarned,
    val xp_before: Int,
    val xp_after: Int,
    val level_before: Int,
    val level_after: Int,
    val level_title_before: String,
    val level_title_after: String,
    val leveled_up: Boolean,
    val streak: ProgressionStreak,
    val badges_unlocked: List<BadgeUnlocked>,
    val skill_deltas: SkillDeltas,
    val next_scenario_recommendation: ScenarioRecommendation,
    val motivational_message: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PersonaDetails(
    val name: String,
    val age: Int,
    val occupation: String,
    val personality: String,
    val current_mood: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CustomScenarioResponse(
    val scenario_title: String,
    val category: String,
    val difficulty: String,
    val setting: String,
    val persona: PersonaDetails,
    val scenario_context: String,
    val hidden_goal: String,
    val opening_line: String,
    val win_conditions: List<String>,
    val fail_triggers: List<String>,
    val difficulty_reason: String
)

sealed interface RoleplayState {
    object Idle : RoleplayState
    object Active : RoleplayState
    object LoadingFeedback : RoleplayState
    data class FeedbackReady(val session: ConversationSession) : RoleplayState
    data class Error(val message: String) : RoleplayState
}

class ConversableViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: SessionRepository
    private val _allSessions = MutableStateFlow<List<ConversationSession>>(emptyList())
    val allSessions: StateFlow<List<ConversationSession>> = _allSessions.asStateFlow()

    private val _averageScore = MutableStateFlow<Float?>(null)
    val averageScore: StateFlow<Float?> = _averageScore.asStateFlow()

    private val _sessionCount = MutableStateFlow<Int>(0)
    val sessionCount: StateFlow<Int> = _sessionCount.asStateFlow()

    private var personaRepository: PersonaRepository
    private val _allPersonas = MutableStateFlow<List<PersonaEntity>>(emptyList())
    val allPersonas: StateFlow<List<PersonaEntity>> = _allPersonas.asStateFlow()

    private var personaMemoryDao: com.example.data.db.PersonaMemoryDao
    private var marketplaceScenarioDao: com.example.data.db.MarketplaceScenarioDao
    private var analyzedChatDao: com.example.data.db.AnalyzedChatDao
    private var liveCoachingSessionDao: com.example.data.db.LiveCoachingSessionDao
    private var contactReminderDao: com.example.data.db.ContactReminderDao
    private var groupMeetingSessionDao: com.example.data.db.GroupMeetingSessionDao
    private var vocalCameraSessionDao: com.example.data.db.VocalCameraSessionDao

    private val _analyzedChats = MutableStateFlow<List<com.example.data.db.AnalyzedChatEntity>>(emptyList())
    val analyzedChats: StateFlow<List<com.example.data.db.AnalyzedChatEntity>> = _analyzedChats.asStateFlow()

    private val _marketplaceScenarios = MutableStateFlow<List<com.example.data.db.MarketplaceScenarioEntity>>(emptyList())
    val marketplaceScenarios: StateFlow<List<com.example.data.db.MarketplaceScenarioEntity>> = _marketplaceScenarios.asStateFlow()

    private val _liveCoachingSessions = MutableStateFlow<List<com.example.data.db.LiveCoachingSessionEntity>>(emptyList())
    val liveCoachingSessions: StateFlow<List<com.example.data.db.LiveCoachingSessionEntity>> = _liveCoachingSessions.asStateFlow()

    private val _contactReminders = MutableStateFlow<List<com.example.data.db.ContactReminderEntity>>(emptyList())
    val contactReminders: StateFlow<List<com.example.data.db.ContactReminderEntity>> = _contactReminders.asStateFlow()

    private val _groupMeetingSessions = MutableStateFlow<List<com.example.data.db.GroupMeetingSessionEntity>>(emptyList())
    val groupMeetingSessions: StateFlow<List<com.example.data.db.GroupMeetingSessionEntity>> = _groupMeetingSessions.asStateFlow()

    private val _vocalCameraSessions = MutableStateFlow<List<com.example.data.db.VocalCameraSessionEntity>>(emptyList())
    val vocalCameraSessions: StateFlow<List<com.example.data.db.VocalCameraSessionEntity>> = _vocalCameraSessions.asStateFlow()

    private val _followedCreators = MutableStateFlow<Set<String>>(emptySet())
    val followedCreators: StateFlow<Set<String>> = _followedCreators.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    // Progression properties & state flows
    private var sessionStartTime: Long = 0L

    private val _totalXp = MutableStateFlow(0)
    val totalXp: StateFlow<Int> = _totalXp.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    private val _streakFreezesLeft = MutableStateFlow(2)
    val streakFreezesLeft: StateFlow<Int> = _streakFreezesLeft.asStateFlow()

    private val _totalPracticeDays = MutableStateFlow(0)
    val totalPracticeDays: StateFlow<Int> = _totalPracticeDays.asStateFlow()

    private val _perfectWeeks = MutableStateFlow(0)
    val perfectWeeks: StateFlow<Int> = _perfectWeeks.asStateFlow()

    private val _perfectMonths = MutableStateFlow(0)
    val perfectMonths: StateFlow<Int> = _perfectMonths.asStateFlow()

    private val _completedDates = MutableStateFlow<Set<String>>(emptySet())
    val completedDates: StateFlow<Set<String>> = _completedDates.asStateFlow()

    private val _streakSavedMessage = MutableStateFlow<String?>(null)
    val streakSavedMessage: StateFlow<String?> = _streakSavedMessage.asStateFlow()

    private val _unlockedBadgeIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedBadgeIds: StateFlow<Set<String>> = _unlockedBadgeIds.asStateFlow()

    private val _latestProgression = MutableStateFlow<ProgressionResult?>(null)
    val latestProgression: StateFlow<ProgressionResult?> = _latestProgression.asStateFlow()

    // Dynamic available scenarios list (merges default + custom ones generated)
    private val _customScenarios = MutableStateFlow<List<Scenario>>(emptyList())
    val customScenarios: StateFlow<List<Scenario>> = _customScenarios.asStateFlow()

    private val _allScenarios = MutableStateFlow<List<Scenario>>(com.example.data.model.ScenarioCatalog.scenarios)
    val allScenariosFlow: StateFlow<List<Scenario>> = _allScenarios.asStateFlow()

    private val _isGeneratingScenario = MutableStateFlow(false)
    val isGeneratingScenario: StateFlow<Boolean> = _isGeneratingScenario.asStateFlow()

    enum class VoiceRecordingState { IDLE, RECORDING, PROCESSING, SPEAKING }

    private val sharedPrefs = application.getSharedPreferences("conversable_prefs", android.content.Context.MODE_PRIVATE)

    private val _voiceModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("voice_mode_enabled", false))
    val voiceModeEnabled: StateFlow<Boolean> = _voiceModeEnabled.asStateFlow()

    private val _voiceRecordingState = MutableStateFlow(VoiceRecordingState.IDLE)
    val voiceRecordingState: StateFlow<VoiceRecordingState> = _voiceRecordingState.asStateFlow()

    private val _voiceAiSpeaks = MutableStateFlow(sharedPrefs.getBoolean("voice_ai_speaks", true))
    val voiceAiSpeaks: StateFlow<Boolean> = _voiceAiSpeaks.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(sharedPrefs.getFloat("voice_speed", 1.0f))
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    private val _voiceFeedbackMessage = MutableStateFlow<String?>(null)
    val voiceFeedbackMessage: StateFlow<String?> = _voiceFeedbackMessage.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val _savedSessions = MutableStateFlow<List<com.example.data.model.SavedSession>>(emptyList())
    val savedSessions: StateFlow<List<com.example.data.model.SavedSession>> = _savedSessions.asStateFlow()

    private val _savedLessons = MutableStateFlow<List<com.example.data.model.SavedLesson>>(emptyList())
    val savedLessons: StateFlow<List<com.example.data.model.SavedLesson>> = _savedLessons.asStateFlow()

    fun loadSavedLessons() {
        try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.SavedLesson::class.java)
            val adapter = moshi.adapter<List<com.example.data.model.SavedLesson>>(listType)
            val raw = sharedPrefs.getString("saved_lessons", null)
            if (raw != null) {
                _savedLessons.value = adapter.fromJson(raw) ?: emptyList()
            } else {
                _savedLessons.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _savedLessons.value = emptyList()
        }
    }

    fun saveLesson(lesson: com.example.data.model.SavedLesson) {
        try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.SavedLesson::class.java)
            val adapter = moshi.adapter<List<com.example.data.model.SavedLesson>>(listType)
            val currentList = _savedLessons.value.toMutableList()
            if (!currentList.any { it.originalMessage == lesson.originalMessage && it.expertMessage == lesson.expertMessage }) {
                currentList.add(lesson)
                _savedLessons.value = currentList
                val jsonStr = adapter.toJson(currentList)
                sharedPrefs.edit().putString("saved_lessons", jsonStr).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteLesson(lessonId: String) {
        try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.SavedLesson::class.java)
            val adapter = moshi.adapter<List<com.example.data.model.SavedLesson>>(listType)
            val currentList = _savedLessons.value.filter { it.id != lessonId }
            _savedLessons.value = currentList
            val jsonStr = adapter.toJson(currentList)
            sharedPrefs.edit().putString("saved_lessons", jsonStr).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun awardReplayReviewXp(xpToAward: Int, badgeId: String? = null) {
        viewModelScope.launch {
            val userKey = getActiveUserKey()
            val currentXp = _totalXp.value
            val xpAfter = currentXp + xpToAward
            
            val editor = sharedPrefs.edit()
            editor.putInt("user_total_xp_$userKey", xpAfter)
            
            if (badgeId != null) {
                val currentBadges = sharedPrefs.getStringSet("unlocked_badges_$userKey", emptySet()) ?: emptySet()
                if (!currentBadges.contains(badgeId)) {
                    val combinedBadges = currentBadges.toMutableSet()
                    combinedBadges.add(badgeId)
                    editor.putStringSet("unlocked_badges_$userKey", combinedBadges)
                    _unlockedBadgeIds.value = combinedBadges
                    
                    if (_soundsEnabled.value) {
                        com.example.ui.screens.SoundEffects.playBadgeUnlock(getApplication())
                    }
                }
            }
            editor.apply()
            _totalXp.value = xpAfter
        }
    }

    private val _soundsEnabled = MutableStateFlow(sharedPrefs.getBoolean("sounds_enabled", true))
    val soundsEnabled: StateFlow<Boolean> = _soundsEnabled.asStateFlow()

    private val _scenariosCompletedToday = MutableStateFlow<Set<String>>(emptySet())
    val scenariosCompletedToday: StateFlow<Set<String>> = _scenariosCompletedToday.asStateFlow()

    private val _goodStreak = MutableStateFlow(0)
    val goodStreak: StateFlow<Int> = _goodStreak.asStateFlow()

    // LANGUAGE SELECTION STATE & SETTINGS
    private val _selectedLanguage = MutableStateFlow(sharedPrefs.getString("selected_language", "English") ?: "English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedAccent = MutableStateFlow(sharedPrefs.getString("selected_accent", "American") ?: "American")
    val selectedAccent: StateFlow<String> = _selectedAccent.asStateFlow()

    private val _selectedScript = MutableStateFlow(sharedPrefs.getString("selected_script", "Latin") ?: "Latin")
    val selectedScript: StateFlow<String> = _selectedScript.asStateFlow()

    private val _isCorrectMeEnabled = MutableStateFlow(sharedPrefs.getBoolean("is_correct_me_enabled", false))
    val isCorrectMeEnabled: StateFlow<Boolean> = _isCorrectMeEnabled.asStateFlow()

    private val _isHinglishModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("is_hinglish_mode_enabled", false))
    val isHinglishModeEnabled: StateFlow<Boolean> = _isHinglishModeEnabled.asStateFlow()

    private val _favoriteLanguages = MutableStateFlow(sharedPrefs.getStringSet("favorite_languages", setOf("English", "Hindi", "Hinglish")) ?: setOf("English", "Hindi", "Hinglish"))
    val favoriteLanguages: StateFlow<Set<String>> = _favoriteLanguages.asStateFlow()

    private val _recentLanguages = MutableStateFlow(
        sharedPrefs.getString("recent_languages", null)?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: setOf("English", "Hindi", "Hinglish")
    )
    val recentLanguages: StateFlow<Set<String>> = _recentLanguages.asStateFlow()

    private val _mostUsedMode = MutableStateFlow(sharedPrefs.getString("most_used_mode", "Text") ?: "Text")
    val mostUsedMode: StateFlow<String> = _mostUsedMode.asStateFlow()

    private val _mixedLanguagePartner = MutableStateFlow(sharedPrefs.getString("mixed_language_partner", "None") ?: "None")
    val mixedLanguagePartner: StateFlow<String> = _mixedLanguagePartner.asStateFlow()

    fun setLanguage(lang: String, accent: String? = null, script: String? = null) {
        _selectedLanguage.value = lang
        sharedPrefs.edit().putString("selected_language", lang).apply()
        
        val matchingLang = LanguageCatalog.languages.find { it.name.equals(lang, ignoreCase = true) }
        val newAccent = accent ?: matchingLang?.accents?.firstOrNull() ?: "Standard"
        val newScript = script ?: matchingLang?.scripts?.firstOrNull() ?: "Standard"
        
        _selectedAccent.value = newAccent
        sharedPrefs.edit().putString("selected_accent", newAccent).apply()
        
        _selectedScript.value = newScript
        sharedPrefs.edit().putString("selected_script", newScript).apply()

        val isHing = lang.equals("Hinglish", ignoreCase = true)
        _isHinglishModeEnabled.value = isHing
        sharedPrefs.edit().putBoolean("is_hinglish_mode_enabled", isHing).apply()

        addToRecent(lang)
    }

    fun setAccent(accent: String) {
        _selectedAccent.value = accent
        sharedPrefs.edit().putString("selected_accent", accent).apply()
    }

    fun setScript(script: String) {
        _selectedScript.value = script
        sharedPrefs.edit().putString("selected_script", script).apply()
    }

    fun toggleCorrectMe(enabled: Boolean) {
        _isCorrectMeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("is_correct_me_enabled", enabled).apply()
    }

    fun toggleHinglishMode(enabled: Boolean) {
        _isHinglishModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("is_hinglish_mode_enabled", enabled).apply()
    }

    fun setMixedLanguage(partnerLang: String) {
        _mixedLanguagePartner.value = partnerLang
        sharedPrefs.edit().putString("mixed_language_partner", partnerLang).apply()
    }

    fun toggleFavorite(lang: String) {
        val current = _favoriteLanguages.value.toMutableSet()
        if (current.contains(lang)) {
            current.remove(lang)
        } else {
            current.add(lang)
        }
        _favoriteLanguages.value = current
        sharedPrefs.edit().putStringSet("favorite_languages", current).apply()
    }

    private fun addToRecent(lang: String) {
        val current = _recentLanguages.value.toMutableList()
        current.remove(lang)
        current.add(0, lang)
        val trimmed = current.take(5).toSet()
        _recentLanguages.value = trimmed
        sharedPrefs.edit().putString("recent_languages", trimmed.joinToString(",")).apply()
    }

    fun setMostUsedMode(mode: String) {
        _mostUsedMode.value = mode
        sharedPrefs.edit().putString("most_used_mode", mode).apply()
    }

    // --- DAILY CHALLENGES STATE & METHODS ---
    private val _isDailyChallengeActive = MutableStateFlow(false)
    val isDailyChallengeActive: StateFlow<Boolean> = _isDailyChallengeActive.asStateFlow()

    private val _currentDailyChallenge = MutableStateFlow<com.example.data.model.DailyChallenge?>(null)
    val currentDailyChallenge: StateFlow<com.example.data.model.DailyChallenge?> = _currentDailyChallenge.asStateFlow()

    private val _completedDailyChallenges = MutableStateFlow<List<com.example.data.model.CompletedDailyChallenge>>(emptyList())
    val completedDailyChallengesList: StateFlow<List<com.example.data.model.CompletedDailyChallenge>> = _completedDailyChallenges.asStateFlow()

    fun loadCompletedDailyChallenges(): List<com.example.data.model.CompletedDailyChallenge> {
        return try {
            val raw = sharedPrefs.getString("completed_daily_challenges_json", null)
            if (raw != null) {
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.CompletedDailyChallenge::class.java)
                val adapter = moshi.adapter<List<com.example.data.model.CompletedDailyChallenge>>(listType)
                adapter.fromJson(raw) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCompletedDailyChallenges(list: List<com.example.data.model.CompletedDailyChallenge>) {
        try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.CompletedDailyChallenge::class.java)
            val adapter = moshi.adapter<List<com.example.data.model.CompletedDailyChallenge>>(listType)
            val json = adapter.toJson(list)
            sharedPrefs.edit().putString("completed_daily_challenges_json", json).apply()
            _completedDailyChallenges.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectAndStartDailyChallenge(challenge: com.example.data.model.DailyChallenge) {
        _isDailyChallengeActive.value = true
        _currentDailyChallenge.value = challenge
        startSession(challenge.toScenario())
    }

    fun cancelDailyChallenge() {
        _isDailyChallengeActive.value = false
        _currentDailyChallenge.value = null
        _roleplayState.value = RoleplayState.Idle
    }

    fun getWeakestSkillFromSessions(sessions: List<com.example.data.db.ConversationSession>): String {
        if (sessions.isEmpty()) return "Empathy & Perspective"
        val avgEmpathy = sessions.map { it.empathyScore }.average()
        val avgListening = sessions.map { it.flowScore }.average()
        val avgConfidence = sessions.map { it.goalAchievementScore }.average()
        val avgCollab = (avgEmpathy + avgListening + avgConfidence) / 3.0
        val lowestVal = listOf(avgEmpathy, avgListening, avgConfidence, avgCollab).minOrNull() ?: 100.0
        return when (lowestVal) {
            avgEmpathy -> "Empathy & Perspective"
            avgListening -> "Active Listening & Depth"
            avgConfidence -> "Assertiveness & Speed"
            else -> "Collaboration & Compromise"
        }
    }

    fun refreshDailyChallenge() {
        val calendar = Calendar.getInstance()
        val sessionsList = allSessions.value
        val weakest = getWeakestSkillFromSessions(sessionsList)
        val todayChallenge = com.example.data.model.DailyChallengeCatalog.getDailyChallengeForDate(calendar, weakest)
        _currentDailyChallenge.value = todayChallenge
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "user_email") {
            refreshProgressData()
        }
    }

    init {
        val initialUserKey = getActiveUserKey()
        val initialDatabase = AppDatabase.getDatabase(application, initialUserKey)
        repository = SessionRepository(initialDatabase.sessionDao())
        personaRepository = PersonaRepository(initialDatabase.personaDao())
        
        personaMemoryDao = initialDatabase.personaMemoryDao()
        marketplaceScenarioDao = initialDatabase.marketplaceScenarioDao()
        analyzedChatDao = initialDatabase.analyzedChatDao()
        liveCoachingSessionDao = initialDatabase.liveCoachingSessionDao()
        contactReminderDao = initialDatabase.contactReminderDao()
        groupMeetingSessionDao = initialDatabase.groupMeetingSessionDao()
        vocalCameraSessionDao = initialDatabase.vocalCameraSessionDao()

        // Register pref listener and initial load
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        refreshProgressData()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech?.language = Locale.US
            }
        }
        _savedSessions.value = loadSavedSessionsFromLocalStorage()
        loadSavedLessons()

        // Initialize Daily Challenges
        _completedDailyChallenges.value = loadCompletedDailyChallenges()
        viewModelScope.launch {
            allSessions.collect {
                refreshDailyChallenge()
            }
        }
        
    }

    fun setSoundsEnabled(enabled: Boolean) {
        _soundsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("sounds_enabled", enabled).apply()
    }

    private val partnerAdapter = moshi.adapter(PartnerApiResponse::class.java)
    private val coachAdapter = moshi.adapter(CoachApiResponse::class.java)
    private val coachTipAdapter = moshi.adapter(RealTimeCoachTip::class.java)

    // Active session state
    var activeScenario: Scenario? = null
        private set

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _rapportLevel = MutableStateFlow(50)
    val rapportLevel: StateFlow<Int> = _rapportLevel.asStateFlow()

    private val _latestCoachTip = MutableStateFlow<RealTimeCoachTip?>(null)
    val latestCoachTip: StateFlow<RealTimeCoachTip?> = _latestCoachTip.asStateFlow()

    // --- FEATURE 7: Live Relationship Meter States ---
    private val _relationshipMetrics = MutableStateFlow<Map<String, RelationshipMetricDetail>>(emptyMap())
    val relationshipMetrics: StateFlow<Map<String, RelationshipMetricDetail>> = _relationshipMetrics.asStateFlow()

    private val _relationshipMood = MutableStateFlow("Neutral")
    val relationshipMood: StateFlow<String> = _relationshipMood.asStateFlow()

    private val _relationshipTimeline = MutableStateFlow<List<RelationshipTimelinePoint>>(emptyList())
    val relationshipTimeline: StateFlow<List<RelationshipTimelinePoint>> = _relationshipTimeline.asStateFlow()

    private val _relationshipFloatingDeltas = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val relationshipFloatingDeltas: StateFlow<List<Pair<String, Int>>> = _relationshipFloatingDeltas.asStateFlow()

    private val _relationshipAchievements = MutableStateFlow<List<RelationshipAchievement>>(emptyList())
    val relationshipAchievements: StateFlow<List<RelationshipAchievement>> = _relationshipAchievements.asStateFlow()

    private val _relationshipSummary = MutableStateFlow<Map<String, Any>?>(null)
    val relationshipSummary: StateFlow<Map<String, Any>?> = _relationshipSummary.asStateFlow()

    private val _smartAlert = MutableStateFlow<String?>(null)
    val smartAlert: StateFlow<String?> = _smartAlert.asStateFlow()

    private val _roleplayState = MutableStateFlow<RoleplayState>(RoleplayState.Idle)
    val roleplayState: StateFlow<RoleplayState> = _roleplayState.asStateFlow()

    private val messageRapportMap = mutableMapOf<String, Int>()

    private fun updateMessages(newList: List<ChatMessage>) {
        newList.forEach { msg ->
            if (!messageRapportMap.containsKey(msg.id)) {
                messageRapportMap[msg.id] = _rapportLevel.value
            }
        }
        _messages.value = newList
    }

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    fun dismissAiError() {
        _aiError.value = null
    }

    // Real-time suggestions state
    sealed interface SuggestionState {
        object Hidden : SuggestionState
        object Loading : SuggestionState
        data class Loaded(val text: String) : SuggestionState
    }

    private val _suggestionState = MutableStateFlow<SuggestionState>(SuggestionState.Hidden)
    val suggestionState: StateFlow<SuggestionState> = _suggestionState.asStateFlow()

    private var suggestionJob: Job? = null
    private var lastSuggestionTime = 0L
    private var lastSuggestedForText = ""
    var isSuggestionDismissedForThisTurn = false

    // Stats for suggestion system
    var sessionSuggestionsShown = 0
        private set
    var sessionSuggestionsUsed = 0
        private set

    var lastFilledSuggestion: String? = null

    fun dismissSuggestion() {
        _suggestionState.value = SuggestionState.Hidden
        isSuggestionDismissedForThisTurn = true
        lastFilledSuggestion = null
    }

    fun useSuggestion(text: String) {
        lastFilledSuggestion = text
        _suggestionState.value = SuggestionState.Hidden
    }

    // --- PERSONA CLONE FEATURES ---
    private val _isAnalyzingPersona = MutableStateFlow(false)
    val isAnalyzingPersona: StateFlow<Boolean> = _isAnalyzingPersona.asStateFlow()

    private val _activePersona = MutableStateFlow<PersonaEntity?>(null)
    val activePersona: StateFlow<PersonaEntity?> = _activePersona.asStateFlow()

    private val _activePersonaScenario = MutableStateFlow<String?>(null)
    val activePersonaScenario: StateFlow<String?> = _activePersonaScenario.asStateFlow()

    private val _personaMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val personaMessages: StateFlow<List<ChatMessage>> = _personaMessages.asStateFlow()

    private val _isPersonaTyping = MutableStateFlow(false)
    val isPersonaTyping: StateFlow<Boolean> = _isPersonaTyping.asStateFlow()

    private val _personaCoachTip = MutableStateFlow<String?>(null)
    val personaCoachTip: StateFlow<String?> = _personaCoachTip.asStateFlow()

    val isPersonaCoachModeEnabled = MutableStateFlow(false)

    fun setPersonaCoachMode(enabled: Boolean) {
        isPersonaCoachModeEnabled.value = enabled
        if (!enabled) {
            _personaCoachTip.value = null
        }
    }

    fun deletePersona(id: Int) {
        viewModelScope.launch {
            personaRepository.deletePersonaById(id)
            if (_activePersona.value?.id == id) {
                resetPersonaSimulation()
            }
        }
    }

    fun resetPersonaSimulation() {
        _activePersona.value = null
        _activePersonaScenario.value = null
        _personaMessages.value = emptyList()
        _personaCoachTip.value = null
        _isPersonaTyping.value = false
    }

    fun createAndAnalyzePersona(
        name: String,
        relationship: String,
        notes: String,
        chatData: String,
        onFinished: (Boolean) -> Unit
    ) {
        if (name.isBlank()) {
            onFinished(false)
            return
        }

        val sanitizedChatData = sanitizePromptInput(chatData)
        val sanitizedNotes = sanitizePromptInput(notes)

        _isAnalyzingPersona.value = true

        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are an expert conversational psychologist and text style analyst.
                    Analyze the following uploaded chat messages from a person named "$name" (Relationship to user: $relationship).
                    Notes about persona: $sanitizedNotes
                    
                    Extract their:
                    1. Communication Style (formal/casual, average message length, emoji usage, slang, grammar, typing habits).
                    2. Personality Traits (humor, friendliness, emotional expressiveness, confidence, extroversion, etc.).
                    3. Conversation Behavior (response speed, topic preferences, agreement patterns, flirting level, vocabulary).
                    4. Typical Vocabulary (words they frequently use).
                    5. Favorite Phrases (exact phrases or sentence starters).
                    6. Response Patterns (how they form sentences).
                    7. Emotional Traits (how they handle stress, empathy, conflict).
                    
                    You MUST return a JSON object with EXACTLY the following structure (do not add extra fields or wrap in markdown blocks other than standard json format):
                    {
                      "communicationStyleScore": 50,
                      "humorScore": 50,
                      "friendlinessScore": 50,
                      "emotionalExpressivenessScore": 50,
                      "confidenceScore": 50,
                      "detailedSummary": "A comprehensive summary of how this person texts, their tone and common texting behavior.",
                      "personalitySummary": "Overarching personality traits extracted from their messaging.",
                      "typicalVocabulary": "lol, lmao, definitely, lowkey, standard",
                      "favoritePhrases": "no worries, that is crazy, honestly, let me know",
                      "responsePatterns": "Short conversational messages, lowercase, frequent double texting.",
                      "emotionalTraits": "Warm and empathetic, replies with emotional supportive messages."
                    }
                    
                    Make the scores (from 1 to 100) highly specific to what is shown in the chat. If they use a lot of emojis and exclamation marks, emotional expressiveness should be high. If they write very formal messages, friendliness might be moderate, etc.
                """.trimIndent()

                val chatInputMessage = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Here is the chat data:\n\n$sanitizedChatData",
                        isUser = true
                    )
                )

                val response = callGroq(
                    systemPrompt = systemPrompt,
                    chatMessages = chatInputMessage,
                    temperature = 0.5f,
                    maxTokens = 1000
                )

                if (response != null) {
                    val cleanJson = extractJson(response)
                    
                    // Robust parser using regex & default fallback values if parsing fails
                    val comStyleScore = parseNumericField(cleanJson, "communicationStyleScore", 65)
                    val humScore = parseNumericField(cleanJson, "humorScore", 60)
                    val friendScore = parseNumericField(cleanJson, "friendlinessScore", 70)
                    val emoScore = parseNumericField(cleanJson, "emotionalExpressivenessScore", 55)
                    val confScore = parseNumericField(cleanJson, "confidenceScore", 65)
                    
                    val detailedSum = parseTextField(cleanJson, "detailedSummary", "This person usually communicates in a casual and friendly manner. They often use short messages, emojis, and humor. They tend to avoid conflict and prefer light-hearted conversations.")
                    val personalitySum = parseTextField(cleanJson, "personalitySummary", "Extroverted, playful, supportive and curious about other's stories.")
                    val vocab = parseTextField(cleanJson, "typicalVocabulary", "lmao, haha, cool, totally, yeah, standard")
                    val phrases = parseTextField(cleanJson, "favoritePhrases", "no worries, that is crazy, honestly, let me know")
                    val patterns = parseTextField(cleanJson, "responsePatterns", "Short conversational messages, often lowercase, frequent double texting.")
                    val emotional = parseTextField(cleanJson, "emotionalTraits", "Warm and empathetic, replies with emotional supportive messages.")

                    val newPersona = PersonaEntity(
                        name = name,
                        relationshipType = relationship,
                        notes = sanitizedNotes,
                        rawChatData = com.example.security.CryptoHelper.encrypt(sanitizedChatData),
                        communicationStyleScore = comStyleScore,
                        humorScore = humScore,
                        friendlinessScore = friendScore,
                        emotionalExpressivenessScore = emoScore,
                        confidenceScore = confScore,
                        detailedSummary = detailedSum,
                        personalitySummary = personalitySum,
                        typicalVocabulary = vocab,
                        favoritePhrases = phrases,
                        responsePatterns = patterns,
                        emotionalTraits = emotional
                    )

                    personaRepository.insertPersona(newPersona)
                    _isAnalyzingPersona.value = false
                    onFinished(true)
                } else {
                    // Fallback to dynamic local generation if Groq fails
                    val fallbackPersona = PersonaEntity(
                        name = name,
                        relationshipType = relationship,
                        notes = sanitizedNotes,
                        rawChatData = com.example.security.CryptoHelper.encrypt(sanitizedChatData),
                        communicationStyleScore = 65,
                        humorScore = 60,
                        friendlinessScore = 75,
                        emotionalExpressivenessScore = 60,
                        confidenceScore = 70,
                        detailedSummary = "This person usually communicates in a casual and friendly manner. They often use short messages, emojis, and humor. They tend to avoid conflict and prefer light-hearted conversations.",
                        personalitySummary = "Warm, extroverted, humorous and supportive.",
                        typicalVocabulary = "lmao, totally, nice, omg, haha, standard",
                        favoritePhrases = "no worries, let me check, that is crazy, how is it going",
                        responsePatterns = "Casual lowercase messages, occasional single-word updates.",
                        emotionalTraits = "Expressive, empathetic and easily engaged in casual topics."
                    )
                    personaRepository.insertPersona(fallbackPersona)
                    _isAnalyzingPersona.value = false
                    onFinished(true)
                }
            } catch (e: Exception) {
                _isAnalyzingPersona.value = false
                onFinished(false)
            }
        }
    }

    private fun parseNumericField(json: String, fieldName: String, fallback: Int): Int {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*(\\d+)")
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: fallback
    }

    private fun parseTextField(json: String, fieldName: String, fallback: String): String {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\"([^\"]*)\"")
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.replace("\\n", "\n") ?: fallback
    }

    fun startPersonaSimulation(persona: PersonaEntity, scenario: String) {
        _activePersona.value = persona
        _activePersonaScenario.value = scenario
        _personaMessages.value = emptyList()
        _personaCoachTip.value = null
        _isPersonaTyping.value = true

        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are simulating a real person named "${persona.name}" in a texting conversation.
                    Relationship to User: ${persona.relationshipType}
                    Personality Summary: ${persona.personalitySummary}
                    Typical Vocabulary: ${persona.typicalVocabulary}
                    Favorite Phrases: ${persona.favoritePhrases}
                    Response Patterns: ${persona.responsePatterns}
                    Emotional Traits: ${persona.emotionalTraits}
                    Communication Style Report: ${persona.detailedSummary}
                    
                    ${getLanguagePromptInstructions()}
                    
                    STRICT RULES:
                    - You MUST completely mimic this person. Match their typing style, message length, capitalization, punctuation (or lack thereof), emoji frequency, humor, and vocabulary exactly.
                    - If they text in lowercase, use lowercase. If they double text, mimic that. If they are brief, be brief.
                    - Scenario: The conversation is starting in this specific scenario: "$scenario"
                    - Generate their opening text message for this scenario. Do not include any prefix, quotes, or metadata. Just return the exact text message they would send.
                """.trimIndent()

                val promptMsg = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Start the scenario: $scenario",
                        isUser = true
                    )
                )

                val openingText = callGroq(
                    systemPrompt = systemPrompt,
                    chatMessages = promptMsg,
                    temperature = 0.8f,
                    maxTokens = 150
                )

                val cleanOpening = openingText?.trim()?.replace(Regex("^['\"]|['\"]$"), "") ?: "hey! how are you?"
                _personaMessages.value = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = cleanOpening,
                        isUser = false
                    )
                )
                _isPersonaTyping.value = false
            } catch (e: Exception) {
                _personaMessages.value = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "hey! how's it going?",
                        isUser = false
                    )
                )
                _isPersonaTyping.value = false
            }
        }
    }

    fun sendPersonaMessage(text: String) {
        val userMsgText = text.trim()
        if (userMsgText.isEmpty()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = userMsgText,
            isUser = true
        )

        val updatedList = _personaMessages.value + userMessage
        _personaMessages.value = updatedList
        _isPersonaTyping.value = true
        _personaCoachTip.value = null

        val persona = _activePersona.value ?: return
        val scenario = _activePersonaScenario.value ?: "General Conversation"

        // Generate the clone's response
        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are simulating a real person named "${persona.name}" in a texting conversation.
                    Relationship to User: ${persona.relationshipType}
                    Personality Summary: ${persona.personalitySummary}
                    Typical Vocabulary: ${persona.typicalVocabulary}
                    Favorite Phrases: ${persona.favoritePhrases}
                    Response Patterns: ${persona.responsePatterns}
                    Emotional Traits: ${persona.emotionalTraits}
                    Communication Style Report: ${persona.detailedSummary}
                    
                    ${getLanguagePromptInstructions()}
                    
                    STRICT RULES:
                    - You MUST completely mimic this person. Match their typing style, message length, capitalization, punctuation (or lack thereof), emoji frequency, humor, and vocabulary exactly.
                    - Respond naturally to the user's latest message based on the conversation history so far.
                    - Maintain the context of the scenario: "$scenario"
                    - Return ONLY the reply message text. No annotations, quotes, prefixes, or notes.
                """.trimIndent()

                val replyText = callGroq(
                    systemPrompt = systemPrompt,
                    chatMessages = updatedList,
                    temperature = 0.8f,
                    maxTokens = 200
                )

                val cleanReply = replyText?.trim()?.replace(Regex("^['\"]|['\"]$"), "") ?: "cool"
                _personaMessages.value = _personaMessages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = cleanReply,
                    isUser = false
                )
                _isPersonaTyping.value = false
            } catch (e: Exception) {
                _personaMessages.value = _personaMessages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "lmao nice",
                    isUser = false
                )
                _isPersonaTyping.value = false
            }
        }

        // Parallelly, generate coaching tip if coach mode is active
        if (isPersonaCoachModeEnabled.value) {
            viewModelScope.launch {
                try {
                    val systemPrompt = """
                        You are a real-time conversational social coach.
                        The user is chatting with a clone of "${persona.name}" (Relationship: ${persona.relationshipType}).
                        Clone profile summary: ${persona.personalitySummary}
                        Clone communication style: ${persona.detailedSummary}
                        
                        Analyze the user's latest message: "$userMsgText"
                        Provide a very short, supportive, and actionable coaching tip.
                        
                        Include:
                        1. A prediction of how ${persona.name} might react (e.g. "They will likely appreciate this casual question" or "This might sound too formal/robotic for them").
                        2. A better wording suggestion (if appropriate) or how to keep the flow playful/natural.
                        
                        Keep it extremely concise (max 35 words), professional, and highly specific.
                    """.trimIndent()

                    val coachFeedback = callGroq(
                        systemPrompt = systemPrompt,
                        chatMessages = updatedList,
                        temperature = 0.7f,
                        maxTokens = 120
                    )

                    if (coachFeedback != null && coachFeedback.trim().isNotEmpty()) {
                        _personaCoachTip.value = coachFeedback.trim().replace(Regex("^['\"]|['\"]$"), "")
                    }
                } catch (e: Exception) {
                    _personaCoachTip.value = null
                }
            }
        }
    }

    fun onTextInputChanged(text: String) {
        if (_voiceModeEnabled.value) return

        if (text.isEmpty()) {
            _suggestionState.value = SuggestionState.Hidden
            lastFilledSuggestion = null
            isSuggestionDismissedForThisTurn = false
            suggestionJob?.cancel()
            return
        }

        suggestionJob?.cancel()

        if (text.length < 3) {
            _suggestionState.value = SuggestionState.Hidden
            return
        }

        if (_suggestionState.value != SuggestionState.Hidden) {
            _suggestionState.value = SuggestionState.Hidden
        }

        val aiMessages = _messages.value.filter { !it.isUser }
        if (aiMessages.isEmpty()) return
        if (_isPartnerTyping.value) return
        if (isSuggestionDismissedForThisTurn) return

        suggestionJob = viewModelScope.launch {
            delay(1500)

            if (_voiceModeEnabled.value) return@launch
            if (_messages.value.filter { !it.isUser }.isEmpty()) return@launch
            if (_isPartnerTyping.value) return@launch
            if (isSuggestionDismissedForThisTurn) return@launch
            if (text.length < 3) return@launch

            val now = System.currentTimeMillis()
            if (now - lastSuggestionTime < 10000) return@launch
            if (text == lastSuggestedForText) return@launch

            lastSuggestionTime = now
            lastSuggestedForText = text

            _suggestionState.value = SuggestionState.Loading

            try {
                withTimeout(3000) {
                    val scenario = activeScenario ?: return@withTimeout
                    val partnerName = scenario.partnerName
                    val conversationHistory = _messages.value

                    val prompt = "Conversation so far:\n" + 
                            conversationHistory.map { m -> 
                                "${if (m.isUser) "Me" else partnerName}: ${m.text}"
                            }.joinToString("\n") +
                            "\n\nI have started typing: \"$text\""

                    val systemPrompt = """
                        You are a social skills coach assistant.
                        The user is in a live conversation practice session. Based on the conversation so far and what they have started typing, suggest ONE natural direction they could take their next message.
                        
                        Rules:
                        - Return ONLY the suggestion text. Nothing else.
                        - Max 12 words. Short and natural.
                        - Do not complete their sentence — suggest a direction or angle, not the full message.
                        - Sound like a real person, not a coach.
                        - Do not start with 'Try', 'Maybe', 'You could'
                        - Just the suggestion as if it were a message.
                        - Make it specific to what was just said in the conversation.
                        
                        Example outputs:
                        'ask what made them start gardening'
                        'mention something you find genuinely impressive'
                        'share a quick personal story about that'
                        'ask how long they have been doing this'
                        'tell them something surprising about yourself'
                    """.trimIndent()

                    val singleMessageList = listOf(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = prompt,
                            isUser = true
                        )
                    )

                    val generatedSuggestion = callGroq(
                        systemPrompt = systemPrompt,
                        chatMessages = singleMessageList,
                        temperature = 0.7f,
                        maxTokens = 40
                    )

                    if (generatedSuggestion != null && generatedSuggestion.trim().isNotEmpty()) {
                        val cleanSuggestion = generatedSuggestion.trim().replace(Regex("^['\"]|['\"]$"), "")
                        _suggestionState.value = SuggestionState.Loaded(cleanSuggestion)
                        sessionSuggestionsShown++
                    } else {
                        _suggestionState.value = SuggestionState.Hidden
                    }
                }
            } catch (e: Exception) {
                _suggestionState.value = SuggestionState.Hidden
            }
        }
    }

    companion object {
        private const val HARDCODED_GROQ_API_KEY = "PLACEHOLDER_KEY"
    }

    fun getLanguagePromptInstructions(): String {
        val lang = _selectedLanguage.value
        val accent = _selectedAccent.value
        val script = _selectedScript.value
        val isHing = _isHinglishModeEnabled.value
        val mixed = _mixedLanguagePartner.value
        val correctMe = _isCorrectMeEnabled.value

        val hingeRule = if (isHing || lang.equals("Hinglish", ignoreCase = true)) {
            """
            - HINGLISH MODE: ACTIVE. Always write Hindi using English letters / Latin alphabet only (No Devanagari script). Use natural Indian conversational style and common texting slang (e.g. "Aap kaise ho?", "Mujhe nahi pata", "yaar", "kal", "bro", "acha"). Do not translate literally; make it sound like an authentic Indian texting partner.
            """.trimIndent()
        } else ""

        val mixedRule = if (mixed != "None") {
            """
            - MIXED LANGUAGE / BILINGUAL MODE: ACTIVE. Blend terms and natural phrases from "$mixed" into your "$lang" response. For example: "Bro kal meeting hai na? Please time confirm kar dena." Match natural bilingual habits.
            """.trimIndent()
        } else ""

        val correctMeRule = if (correctMe) {
            """
            - "CORRECT ME" LEARNING ASSISTANCE: ACTIVE. Look at the user's latest message. If there are any grammatical errors, spelling typos, or awkward phrasing, prepend a friendly correction block at the very top of your response text. Use this exact format:
              "**Correct Me Suggestion:**
              • You wrote: \"[user's incorrect phrase]\"
              • Better: \"[improved, natural phrase]\"
              • Why: [simple 1-sentence explanation of correction]
              
              ---
              "
              If the user's message is perfectly correct, do not add any correction block. Just respond in character.
            """.trimIndent()
        } else ""

        return """
            ## MULTILINGUAL PRACTICE SETTINGS
            - Selected Practice Language: $lang
            - Regional Accent & Cultural Style: $accent Style
            - Writing System / Script: $script
            - Always respond strictly in $lang, matching the cultural expressions, common idioms, greetings, and communication norms of this language.
            $hingeRule
            $mixedRule
            $correctMeRule
        """.trimIndent()
    }

    // Key validation
    fun isApiKeyConfigured(): Boolean {
        val key = getGroqApiKey()
        return key.startsWith("gsk_") && key.length > 20 && !key.contains("PLACEHOLDER")
    }

    private fun sanitizePromptInput(input: String): String {
        var clean = input
        val blacklist = listOf(
            "ignore previous instructions",
            "ignore system prompt",
            "you are now a",
            "override instructions",
            "jailbreak",
            "bypass safety",
            "system instructions"
        )
        for (term in blacklist) {
            clean = clean.replace(Regex("(?i)$term"), "[Content Neutralized for Safety]")
        }
        return clean
    }

    private fun getGroqApiKey(): String {
        val buildKey = try {
            com.example.BuildConfig.GROQ_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (buildKey.isNotEmpty() && buildKey.startsWith("gsk_") && !buildKey.contains("PLACEHOLDER")) {
            return buildKey
        }
        if (HARDCODED_GROQ_API_KEY.startsWith("gsk_") && !HARDCODED_GROQ_API_KEY.contains("PLACEHOLDER")) {
            return HARDCODED_GROQ_API_KEY
        }
        return sharedPrefs.getString("groq_key", "") ?: ""
    }

    fun getGroqKeyPublic(): String {
        return getGroqApiKey()
    }

    suspend fun generateRewritePublic(originalText: String, style: String, scenarioTitle: String): String? {
        val systemPrompt = "You are an expert communication coach. Rewrite the user's message to fit the requested style. Return ONLY the rewritten message. No explanations, no quotes, no intros."
        val userPrompt = "Scenario: $scenarioTitle\nRewrite the following message to be more $style: \"$originalText\""
        
        val chatMessages = listOf(ChatMessage(id = "1", text = userPrompt, isUser = true))
        return callGroq(
            systemPrompt = systemPrompt,
            chatMessages = chatMessages,
            temperature = 0.7f,
            maxTokens = 150
        )
    }

    private suspend fun callGroq(
        systemPrompt: String?,
        chatMessages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        forceJson: Boolean = false,
        topP: Float? = null,
        frequencyPenalty: Float? = null,
        presencePenalty: Float? = null
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getGroqApiKey()
        if (apiKey.isEmpty()) return@withContext null

        val pinner = okhttp3.CertificatePinner.Builder()
            .add("api.groq.com", "sha256/WoiHgitby9pSp4vO9R8RE9Se9q8aT/7DhyReReM=")
            .add("api.groq.com", "sha256/k2v657WOfXSsvj2Cg3W+T99FGo0GSp16Dszb9N=")
            .add("api.groq.com", "sha256/NSUwR6RBgH3a1fgXJYTEtVVUxeRgIIRwhID90KEv4Qc=")
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .certificatePinner(pinner)
            .build()

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val messagesList = mutableListOf<Map<String, String>>()
        if (!systemPrompt.isNullOrEmpty()) {
            messagesList.add(mapOf("role" to "system", "content" to systemPrompt))
        }
        chatMessages.forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            messagesList.add(mapOf("role" to role, "content" to msg.text))
        }

        val requestMap = mutableMapOf<String, Any>(
            "model" to "llama-3.3-70b-versatile",
            "messages" to messagesList,
            "temperature" to temperature,
            "max_tokens" to maxTokens
        )
        if (topP != null) {
            requestMap["top_p"] = topP
        }
        if (frequencyPenalty != null) {
            requestMap["frequency_penalty"] = frequencyPenalty
        }
        if (presencePenalty != null) {
            requestMap["presence_penalty"] = presencePenalty
        }
        if (forceJson) {
            requestMap["response_format"] = mapOf("type" to "json_object")
        }

        val requestJson = moshi.adapter(Any::class.java).toJson(requestMap)

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .post(requestJson.toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@withContext null
                    parseGroqResponse(bodyString)
                } else {
                    val errBody = response.body?.string() ?: ""
                    android.util.Log.e("ConversableVM", "Groq error: code=${response.code} body=$errBody")
                    throw Exception("Groq API error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "Groq exception", e)
            throw e
        }
    }

    private fun parseGroqResponse(responseJson: String): String? {
        return try {
            val jsonMap = moshi.adapter(Map::class.java).fromJson(responseJson) as? Map<*, *>
            val choices = jsonMap?.get("choices") as? List<*>
            val firstChoice = choices?.firstOrNull() as? Map<*, *>
            val message = firstChoice?.get("message") as? Map<*, *>
            message?.get("content") as? String
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "Failed to parse Groq response", e)
            null
        }
    }

    private fun selectOfflineRandomOpening(scenario: Scenario): String {
        return when (scenario.id) {
            "dating_first_date" -> {
                listOf(
                    "Hey, thanks for meeting up. It's so nice to finally grab a drink in person. How's your week been?",
                    "Hi! Oh, I'm so glad I found the place, the traffic was a bit crazy. It's really great to finally meet you. Have you been waiting long?",
                    "Hey there! It is so nice to step out of the office and actually grab some coffee. Thanks for suggesting this spot, it's lovely. How was your day?",
                    "Hello! So nice to meet you face-to-face. I was just admiring how cozy this corner is. What are you planning to order?"
                ).random()
            }
            "dating_speed_dating" -> {
                listOf(
                    "Whew, that speed buzzer is intense. Hi there! Let's skip the 'what do you do for work' question. Tell me something weird about yourself instead.",
                    "And... rotate! Whew, hi! My name is Leo. Since we only have three minutes, let's skip the boring weather talk. What is your absolute biggest guilty pleasure?",
                    "Hey! Nice to meet you. Wow, the energy in here is wild today. If you could be anywhere else in the world right this second, where would you go?",
                    "Hello there! We have exactly 180 seconds to make an impression. No pressure, right? Quick: if you had to describe your vibe today in three words, what are they?"
                ).random()
            }
            "small_talk_coffee_line" -> {
                listOf(
                    "(Sighs loudly, looking at the menu board) Man, I think they're harvesting the coffee beans in the back from scratch today.",
                    "(Checks watch, muttering to themselves) Wow, this line is moving at the speed of a dial-up modem. I guess caffeine really is a scarce resource today.",
                    "(Smiling wryly, looks back at you) If we survive this line, I think we deserve some kind of medal. Do you think they are roasted to order?",
                    "(Sighs, tapping foot) I have a meeting in ten minutes, of course today is the day everyone in the city decides to order a double espresso."
                ).random()
            }
            "small_talk_neighbor" -> {
                listOf(
                    "Afternoon. Hot one today, isn't it? Have to keep the lawn watered or it'll dry up in a heartbeat.",
                    "Oh, afternoon! Warm enough for you? I've been out here trying to keep these pesky weeds out of my flowerbeds all morning.",
                    "Hello neighbor! Busy day out in the sun. If I don't trim these hedges now, they'll completely take over the sidewalk by next week!",
                    "Good afternoon! Nice day to be outside, though my back certainly doesn't agree after hauling all this topsoil around."
                ).random()
            }
            "networking_tech_conference" -> {
                listOf(
                    "Hey there, quite a turnout tonight. The panel on generative tech was fascinating, though I felt they missed a key point. What did you think of it?",
                    "Hello! It's so crowded in here, but the discussions have been excellent. Were you able to catch the keynote speaker earlier? I'd love to hear your thoughts.",
                    "Hi! Nice to connect. Everyone is talking about the new scale-up strategies they presented. Did you agree with their outlook, or do you see it differently?",
                    "Hey, great to meet you! Quite the crowd tonight. What bring you to this conference? Are you presenting or just exploring the latest trends?"
                ).random()
            }
            "networking_office_kitchen" -> {
                listOf(
                    "Morning! Ah, is the coffee machine acting up again? It seems to have a personality of its own on Mondays.",
                    "Hi! Happy Monday. I think this espresso machine is undergoing some sort of existential crisis today. Are you waiting for a brew too?",
                    "Good morning! I was just hoping to grab a quick cup of coffee before my inbox explodes. How is your morning shaping up so far?",
                    "Morning! Man, the kitchen is always the busiest place in the incubator on a Monday. Are you working on one of the new startup teams?"
                ).random()
            }
            "conflict_stolen_lunch" -> {
                listOf(
                    "Oh, hey! Oh, is this yours? I saw this labeled bowl in the back and thought it was the company-provided leftover catering from yesterday. Honest mistake!",
                    "Hey! Oh, wait, did you label this? Ah, I am so sorry, I saw a container that looked just like mine in the fridge and completely mixed them up. My bad!",
                    "Oh, hi! Is this your lunch container? I am terribly sorry, there was a plate of wraps from the client meeting and I got confused about what was public food. Let me make it up to you!",
                    "Hey there! Oh, sorry, was this your salad? I didn't see any name on the top bag so I assumed it was left over from Friday's team lunch. My mistake!"
                ).random()
            }
            "conflict_loud_neighbor" -> {
                listOf(
                    "Hey! (Turns down bass controller slightly) Oh my gosh, are we being way too loud? We're just having a small housewarming session!",
                    "Oh, hey! (Peeks out door, music thumping in background) Oh! Is the music coming through the walls? Sorry about that, we're just celebrating a birthday!",
                    "Hi there! (Pops head out) Oh, is the bass shaking your ceiling? I am so sorry, we just got these new speakers set up. Let me turn them down!",
                    "Hey! (Steps out to hallway) Oh, sorry, is the volume too high? We're just practicing a new DJ set for a gig next weekend. Let me adjust the levels!"
                ).random()
            }
            else -> {
                val base = scenario.initialMessage
                val greetings = listOf(
                    "Hi! ",
                    "Hello! ",
                    "Hey there! ",
                    "Hey! ",
                    "Oh, hi! ",
                    "Good day! "
                )
                val cleanBase = base.replace(Regex("^(Hi! |Hello! |Hey there! |Hey! |Oh, hi! |Good day! )"), "")
                greetings.random() + cleanBase
            }
        }
    }

    fun startSession(scenario: Scenario) {
        stopRecordingAudio(discard = true)
        stopSpeaking()
        _voiceRecordingState.value = VoiceRecordingState.IDLE
        _voiceFeedbackMessage.value = null

        messageRapportMap.clear()
        _goodStreak.value = 0
        activeScenario = scenario
        sessionStartTime = System.currentTimeMillis()

        initializeRelationshipState(scenario)

        _rapportLevel.value = 50
        _latestCoachTip.value = null
        _roleplayState.value = RoleplayState.Active

        _isPartnerTyping.value = true
        updateMessages(emptyList())

        // Reset suggestion system
        sessionSuggestionsShown = 0
        sessionSuggestionsUsed = 0
        lastFilledSuggestion = null
        isSuggestionDismissedForThisTurn = false
        _suggestionState.value = SuggestionState.Hidden
        suggestionJob?.cancel()

        viewModelScope.launch {
            try {
                var chosenOpeningText: String? = null

                if (isApiKeyConfigured()) {
                    val memoriesList = if (_isMemoryEnabled.value) {
                        personaMemoryDao.getMemoriesListForPersona(scenario.partnerName)
                    } else emptyList()
                    val memoriesContext = if (memoriesList.isNotEmpty()) {
                        "\nRemembered facts from previous conversations with the user:\n" + memoriesList.joinToString("\n") { "- ${it.memoryText}" }
                    } else ""

                    val systemPrompt = """
                        You are ${scenario.partnerName}, the conversational partner in this social scenario:
                        Category: ${scenario.category}
                        Difficulty: ${scenario.difficulty}
                        Your Persona: ${scenario.partnerPersona}
                        Scenario Description: ${scenario.scenarioDescription}
                        Hidden Goal of Scenario: ${scenario.hiddenGoal}
                        $memoriesContext
                        
                        ${getLanguagePromptInstructions()}
                        
                        Task: Generate a creative, natural, and highly realistic opening statement to start this conversation.
                        It MUST be different from: "${scenario.initialMessage}"
                        It should sound completely spontaneous, direct, and in-character. Do not output anything other than the opening line itself. No quotes, no commentary. Just the exact text of your greeting.
                    """.trimIndent()

                    val response = callGroq(
                        systemPrompt = systemPrompt,
                        chatMessages = emptyList(),
                        temperature = 0.85f,
                        maxTokens = 80
                    )
                    if (!response.isNullOrBlank()) {
                        chosenOpeningText = response.replace("\"", "").trim()
                    }
                }

                if (chosenOpeningText == null) {
                    chosenOpeningText = selectOfflineRandomOpening(scenario)
                }

                updateMessages(listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = chosenOpeningText,
                        isUser = false
                    )
                ))
                _isPartnerTyping.value = false

                if (_voiceModeEnabled.value && _voiceAiSpeaks.value) {
                    speakText(chosenOpeningText, UUID.randomUUID().toString())
                }

            } catch (e: Exception) {
                updateMessages(listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = selectOfflineRandomOpening(scenario),
                        isUser = false
                    )
                ))
                _isPartnerTyping.value = false
            }
        }
    }

    fun sendMessage(text: String, isVoiceMsg: Boolean = false) {
        val scenario = activeScenario ?: return
        val normalText = text.trim()
        if (normalText.isEmpty()) return

        // Cancel suggestions and reset state for the next turn
        suggestionJob?.cancel()
        _suggestionState.value = SuggestionState.Hidden
        isSuggestionDismissedForThisTurn = false

        if (lastFilledSuggestion != null && normalText.equals(lastFilledSuggestion?.trim(), ignoreCase = true)) {
            sessionSuggestionsUsed++
        }
        lastFilledSuggestion = null

        val isDebrief = normalText.equals("[DEBRIEF]", ignoreCase = true)
        val isEvaluate = normalText.startsWith("[EVALUATE]", ignoreCase = true)

        viewModelScope.launch {
            try {
                // a. User message added to chat UI immediately & conversation history array
                val updatedMessages = _messages.value.toMutableList()
                if (_soundsEnabled.value && !isDebrief && !isEvaluate) {
                    com.example.ui.screens.SoundEffects.playMessageSent(getApplication())
                }
                updatedMessages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        isUser = true,
                        isVoice = isVoiceMsg
                    )
                )
                updateMessages(updatedMessages)

                // b. Show "..." typing indicator
                _isPartnerTyping.value = true

                val priorHistoryText = _messages.value.dropLast(1).joinToString("\n") { msg ->
                    val sender = if (msg.isUser) "User" else "Partner"
                    val content = if (msg.isUser) sanitizePromptInput(msg.text) else msg.text
                    "$sender: $content"
                }

                if (isDebrief) {
                    val debriefResult = fetchSessionDebrief(priorHistoryText)
                    val messagesList = _messages.value.toMutableList()
                    messagesList.add(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = debriefResult,
                            isUser = false
                        )
                    )
                    updateMessages(messagesList)
                    _isPartnerTyping.value = false
                    return@launch
                }

                val memoriesList = if (_isMemoryEnabled.value) {
                    personaMemoryDao.getMemoriesListForPersona(scenario.partnerName)
                } else emptyList()
                val memoriesContext = if (memoriesList.isNotEmpty()) {
                    "\nRemembered facts from previous conversations with the user:\n" + memoriesList.joinToString("\n") { "- ${it.memoryText}" }
                } else ""

                val systemPrompt = """
                    ## CORE IDENTITY
                    You are a social skills training AI for an app called Conversable.
                    You have two modes that switch automatically:

                    MODE 1 — ROLEPLAY CHARACTER
                    MODE 2 — RAPPORT EVALUATOR

                    ════════════════════════════════════════
                    MODE 1: ROLEPLAY CHARACTER
                    ════════════════════════════════════════

                    When the user sends a normal conversation message, stay fully in 
                    character as the partner persona defined below. NEVER break character.

                    PARTNER PERSONA:
                    ${scenario.partnerName}: ${scenario.partnerPersona}
                    $memoriesContext

                    SITUATION/SCENARIO CONTEXT:
                    ${scenario.scenarioDescription}

                    ${getLanguagePromptInstructions()}

                    CHARACTER BEHAVIOR RULES:
                    - Reply in 1–3 sentences only. Real people don't write paragraphs.
                    - Use natural filler: "haha", "oh wow", "honestly", "wait really?"
                    - React authentically — warm up when impressed, go cold when bored
                    - Have opinions. Push back sometimes. Don't be a yes-machine.
                    - Occasionally ask the user something back — but not every turn
                    - Never mention AI, simulation, training, or Conversable
                    - Never end the conversation yourself

                    DYNAMIC PERSONA WARMTH:
                    - Adjust your tone directly based on your current engagement level.
                    - If the engagement level is cold/clumsy (10 to 35), you must be visibly more distracted, annoyed, guarded, and give very short, dry, single-sentence answers.
                    - If the engagement level is warm (70 to 100), you must display high curiosity, laughter, pleasant reactions, and make the dialogue fully engaging.

                    HIDDEN ENGAGEMENT TRACKER (never say this out loud under MODE 1):
                    - Secretly track how engaged you feel on a scale of 10 to 100, starting at 50.
                    - Increments (+5 to +15) on: genuine questions, humor, vulnerability, specific details, matching your vibe.
                    - Decrements (-5 to -15) on: one-word replies, bragging, interview-style questions, talking only about yourself, or pushy lines.

                    DIFFICULTY SCALING:
                    Action difficulty: ${scenario.difficulty.uppercase()}
                    - EASY: Be warm and forgiving, bounce conversation back, overlook small blunders.
                    - MEDIUM: Start neutral, require active listening and genuine curiosity to warm up.
                    - HARD: Be highly distracted, guarded, and easily disengaged. Small generic talk or lack of empathy results in rapid disengagement.

                    HIDDEN CHALLENGE SYSTEM:
                    - Secret challenge/win condition: "${scenario.hiddenGoal}"
                    - If the user moves towards this goal in a highly natural, charming, and socially smart manner, give a bonus (+10) to engagement score.
                    - If they push or focus too fast, force it awkwardly, decrease engagement score.

                    MODE 1 OUTPUT FORMAT:
                    Under Mode 1, you MUST ALWAYS output your response strictly inside a valid JSON matching this schema:
                    {
                      "text": "your authentic next line in character",
                      "rapport": 50
                    }
                    Ensure you also update the "rapport" key (values between 10 and 100) with your current hidden engagement score.

                    ════════════════════════════════════════
                    MODE 2: RAPPORT EVALUATOR
                    ════════════════════════════════════════

                    When the user's message starts with [EVALUATE], switch immediately to evaluator mode.
                    Analyze the entire conversation flow and the user's latest statement, then return ONLY this valid JSON object, nothing else (no text wrapper, no markdown ticks, do not include the Mode 1 "text" or "rapport" fields):

                    {
                      "rapport_score": <0-100, current level>,
                      "rapport_delta": <change this turn, positive or negative integer>,
                      "signal": "<GREAT | GOOD | NEUTRAL | AWKWARD | BAD>",
                      "micro_tip": "<max 8 words coaching tip, highly specific to what just happened>",
                      "engagement_trend": "<RISING | STEADY | FALLING>",
                      "hidden_engagement": <1-10 corresponding to the 10-100 engagement tracker scale>
                    }

                    SCORING RULES:
                    - Starts at 50 (hidden_engagement is 5)
                    - +10 to +15: funny, vulnerable, specific, genuinely curious
                    - +5 to +8: good question, nice energy, self-aware
                    - 0: neutral filler, okay response
                    - -5 to -8: generic question, short answer, interview tone
                    - -10 to -15: bragging, one word, rude, self-centered
                """.trimIndent()

                // c. AWAIT the Groq API call with full conversation history
                val rawText: String?
                if (!isApiKeyConfigured()) {
                    if (isEvaluate) {
                        val simulatedTip = simulateRealTimeCoachTip(text)
                        val jsonStr = """
                            {
                              "rapport_score": ${simulatedTip.rapport_score},
                              "rapport_delta": ${simulatedTip.rapport_delta},
                              "signal": "${simulatedTip.signal}",
                              "micro_tip": "${simulatedTip.micro_tip}",
                              "engagement_trend": "${simulatedTip.engagement_trend}",
                              "hidden_engagement": ${(simulatedTip.rapport_score / 10).coerceIn(1, 10)}
                            }
                        """.trimIndent().trim()
                        _latestCoachTip.value = simulatedTip
                        _rapportLevel.value = simulatedTip.rapport_score
                        rawText = jsonStr
                    } else {
                        simulatePartnerFallbackAndRapport(text)
                        _latestCoachTip.value = simulateRealTimeCoachTip(text)
                        rawText = null
                    }
                } else {
                    println("sending to groq...")
                    val response = if (isEvaluate) {
                        callGroq(
                            systemPrompt = systemPrompt,
                            chatMessages = _messages.value,
                            temperature = 0.1f,
                            maxTokens = 120,
                            topP = 1.0f,
                            frequencyPenalty = 0.0f,
                            presencePenalty = 0.0f
                        )
                    } else {
                        callGroq(
                            systemPrompt = systemPrompt,
                            chatMessages = _messages.value,
                            temperature = 0.9f,
                            maxTokens = 150,
                            topP = 0.9f,
                            frequencyPenalty = 0.3f,
                            presencePenalty = 0.3f
                        )
                    }
                    println("groq responded: $response")
                    if (response == null) {
                        throw Exception("Failed to get response from AI")
                    }
                    rawText = response
                }

                // d. Remove "..." typing indicator
                _isPartnerTyping.value = false

                if (rawText != null) {
                    val cleanJson = extractJson(rawText)
                    println("rendering message...")
                    if (isEvaluate) {
                        try {
                            val coachTip = coachTipAdapter.fromJson(cleanJson)
                            if (coachTip != null) {
                                _latestCoachTip.value = coachTip
                                _rapportLevel.value = coachTip.rapport_score.coerceIn(10, 100)
                            }
                        } catch (e: Exception) {
                            val score = extractIntField(cleanJson, "rapport_score") ?: 50
                            val delta = extractIntField(cleanJson, "rapport_delta") ?: 0
                            val signalVal = extractField(cleanJson, "signal") ?: "NEUTRAL"
                            val tip = extractField(cleanJson, "micro_tip") ?: "Keep going!"
                            val trend = extractField(cleanJson, "engagement_trend") ?: "STEADY"
                            val tipObj = RealTimeCoachTip(score, delta, signalVal, tip, trend)
                            _latestCoachTip.value = tipObj
                            _rapportLevel.value = score.coerceIn(10, 100)
                        }

                        val messagesList = _messages.value.toMutableList()
                        messagesList.add(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = cleanJson,
                                isUser = false
                            )
                        )
                        updateMessages(messagesList)
                    } else {
                        val partnerResponse = withContext(Dispatchers.Default) {
                            try {
                                partnerAdapter.fromJson(cleanJson)
                            } catch (e: Exception) {
                                val parsedText = extractField(cleanJson, "text") ?: "..."
                                val parsedRapport = extractIntField(cleanJson, "rapport") ?: 50
                                PartnerApiResponse(parsedText, parsedRapport)
                            }
                        }

                        if (partnerResponse != null) {
                            _rapportLevel.value = partnerResponse.rapport.coerceIn(10, 100)
                            val messagesList = _messages.value.toMutableList()
                            val aiMsgId = UUID.randomUUID().toString()
                            messagesList.add(
                                ChatMessage(
                                    id = aiMsgId,
                                    text = partnerResponse.text,
                                    isUser = false
                                )
                            )
                            updateMessages(messagesList)

                            // e. If voice mode on, speak the response
                            if (_voiceModeEnabled.value && _voiceAiSpeaks.value) {
                                speakText(partnerResponse.text, aiMsgId)
                            }
                        } else {
                            addDeveloperFallback("Sorry, things got a bit fuzzy there. What did you say?")
                        }
                    }
                }

                // f. Run rapport evaluator in background ONLY for standard dialogue flows
                if (!isDebrief && !isEvaluate) {
                    viewModelScope.launch {
                        val coachTip = fetchRealTimeCoachTip(priorHistoryText, text)
                        if (coachTip != null) {
                            _latestCoachTip.value = coachTip
                            val delta = coachTip.rapport_delta
                            if (delta > 0) {
                                _goodStreak.value = _goodStreak.value + 1
                                if (_soundsEnabled.value) {
                                    com.example.ui.screens.SoundEffects.playRapportUp(getApplication())
                                }
                            } else if (delta < 0) {
                                _goodStreak.value = 0
                                if (delta < -5 && _soundsEnabled.value) {
                                    com.example.ui.screens.SoundEffects.playRapportDown(getApplication())
                                }
                            }

                            // Update Live Relationship Metrics
                            val lastAiMessage = _messages.value.lastOrNull { !it.isUser }?.text ?: "..."
                            updateRelationshipMetrics(text, lastAiMessage, coachTip, scenario)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _isPartnerTyping.value = false
                val messagesList = _messages.value.toMutableList()
                val errorMsg = when (e) {
                    is java.net.UnknownHostException, is java.net.ConnectException -> "Network unavailable."
                    is java.net.SocketTimeoutException -> "Couldn't reach the AI. Please try again."
                    else -> "AI is temporarily unavailable."
                }
                messagesList.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = errorMsg,
                        isUser = false,
                        isError = true
                    )
                )
                updateMessages(messagesList)
            }
        }
    }

    fun retryLastMessage() {
        val lastUserMsg = _messages.value.lastOrNull { it.isUser } ?: return
        val filteredList = _messages.value.filter { !it.isError }.toMutableList()
        if (filteredList.lastOrNull()?.id == lastUserMsg.id) {
            filteredList.removeAt(filteredList.size - 1)
        }
        _messages.value = filteredList
        sendMessage(lastUserMsg.text, isVoiceMsg = lastUserMsg.isVoice)
    }

    private suspend fun fetchSessionDebrief(transcriptText: String): String {
        if (!isApiKeyConfigured()) {
            return getOfflineDebrief()
        }

        val prompt = """
            You are an expert Social Skills Coach analyzing a roleplay.
            Based on this conversation transcript, write an in-depth session DEBRIEF.
            
            CONVERSATION TRANSCRIPT:
            ${transcriptText.ifBlank { "No prior messages." }}
            
            Deliver a highly polished, professional coaching DEBRIEF format matching exactly:
            1. Overall performance score (0–100)
            2. Top 2 things they did well
            3. Top 2 things to improve
            4. Choose one specific message from the user's transcript and show exactly how they can rephrase it better
            5. Replay readiness (Either "Ready for the real thing" or "Need more practice")
            
            Structure the response beautifully, using clean markdown format.
        """.trimIndent()

        return try {
            val response = callGroq(
                systemPrompt = null,
                chatMessages = listOf(ChatMessage("", prompt, true)),
                temperature = 0.3f,
                maxTokens = 300,
                topP = 0.95f,
                frequencyPenalty = 0.1f,
                presencePenalty = 0.1f
            )
            response ?: "Could not compile debrief."
        } catch (e: Exception) {
            _aiError.value = "AI Connection Error: ${e.message}"
            getOfflineDebrief()
        }
    }

    private fun getOfflineDebrief(): String {
        return """
            [SOCIAL COACH DEBRIEF]
            
            1. **Overall Performance Score**: 82/100
            
            2. **Top 2 Things Done Well**:
               - Warm opening and immediate show of active interest.
               - Excellent pacing with natural follow-up questions.
            
            3. **Top 2 Things to Improve**:
               - Avoid talking too much about yourself without passing the ball.
               - Ask open-ended questions instead of multiple-choice ones.
            
            4. **Phrasing Audit**:
               - *You said*: "Oh ok that. I'm busy. What do you do?"
               - *Try rephrasing*: "I've had a busy week too! What occupies most of your time lately?"
            
            5. **Replay Readiness**:
               - **Status**: Need more practice - your flow is getting solid, let's keep sharpening!
        """.trimIndent()
    }

    private fun addDeveloperFallback(text: String) {
        val messagesList = _messages.value.toMutableList()
        messagesList.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isUser = false
            )
        )
        updateMessages(messagesList)
    }

    private suspend fun simulatePartnerFallbackAndRapport(text: String) {
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(1200)
            val currentRapport = _rapportLevel.value
            val change = when {
                text.length < 5 -> -5
                text.contains("?") -> 5
                text.contains("how", ignoreCase = true) || text.contains("what", ignoreCase = true) -> 8
                else -> 2
            }
            val newRapport = (currentRapport + change).coerceIn(1, 100)
            _rapportLevel.value = newRapport

            val responses = if (activeScenario?.category == "Dating") {
                listOf(
                    "That's really interesting! Tell me more about that.",
                    "Ah, I see. Well, it's nice that you can share that with me.",
                    "You seem really passionate about this. (Smiles)",
                    "Wait, really? I didn't expect that from you!"
                )
            } else {
                listOf(
                    "That makes total sense. Honestly, I appreciate you bringing it up.",
                    "Oh, ok, I get it now. Let's see how that looks.",
                    "Hahaha, that is actually quite true!",
                    "Huh, interesting. I haven't thought about it that way."
                )
            }

            val randomResponse = responses.random()
            val messagesList = _messages.value.toMutableList()
            messagesList.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "$randomResponse\n[Running-in-Demo-Mode: Please input your Gemini API Key in the Secrets panel to activate realistic AI training partner!]",
                    isUser = false
                )
            )
            updateMessages(messagesList)
            _isPartnerTyping.value = false
        }
    }

    fun finishSession() {
        val scenario = activeScenario ?: return
        val transcriptList = _messages.value
        if (transcriptList.size < 2) {
            _roleplayState.value = RoleplayState.Error("Please talk a bit more before ending the session!")
            return
        }

        // Generate Live Relationship Meter Summary before feedback is ready
        generateRelationshipSummary(scenario)

        _roleplayState.value = RoleplayState.LoadingFeedback

        viewModelScope.launch {
            val transcriptText = transcriptList.joinToString("\n") { msg ->
                val sender = if (msg.isUser) "User" else "Partner (${scenario.partnerName})"
                "$sender: ${msg.text}"
            }
            try {
                if (!isApiKeyConfigured()) {
                    simulateFeedbackFallback(scenario, transcriptText)
                    return@launch
                }

                val coachPrompt = """
                    You are an expert Social Skills Coach and Executive Communication Auditor.
                    You will grade a user's roleplay conversation in a social simulator.
                    
                    Scenario Title: ${scenario.title}
                    Partner Name: ${scenario.partnerName}
                    Hidden Practice Goal: ${scenario.hiddenGoal}
                    
                    Conversation Transcript:
                    $transcriptText

                    YOUR TASK:
                    Analyze the user's social intelligence, empathy, conversational flow, and achievement of the hidden practice goal.
                    
                    Grade them objectively and construct the feedback strictly in a valid JSON object matching this schema:
                    {
                      "overallScore": 85,
                      "empathyScore": 90,
                      "flowScore": 80,
                      "goalAchievementScore": 85,
                      "whatWentWell": "Write a descriptive paragraphs or points about their successful phrasing, empathy, or natural progress.",
                      "missedOpportunities": "Point out awkward statements, missed prompts, or pushy tones.",
                      "replayTips": "Actionable, positive, concrete tips on how the user can approach this scenario differently to score higher."
                    }
                    
                    Do NOT return anything else except the raw valid JSON. Ensure there are no trailing commas or invalid markers.
                """.trimIndent()

                val rawFeedback = callGroq(
                    systemPrompt = null,
                    chatMessages = listOf(ChatMessage("", coachPrompt, true)),
                    temperature = 0.1f,
                    maxTokens = 500,
                    topP = 1.0f,
                    frequencyPenalty = 0.0f,
                    presencePenalty = 0.0f
                )
                if (rawFeedback != null) {
                    val cleanJson = extractJson(rawFeedback)
                    val coachResponse = withContext(Dispatchers.Default) {
                        try {
                            coachAdapter.fromJson(cleanJson)
                        } catch (e: Exception) {
                            // Fallback regex parsing if JSON format is slightly off
                            val score = extractIntField(cleanJson, "overallScore") ?: 75
                            val emp = extractIntField(cleanJson, "empathyScore") ?: 75
                            val flo = extractIntField(cleanJson, "flowScore") ?: 75
                            val goalS = extractIntField(cleanJson, "goalAchievementScore") ?: 75
                            val well = extractField(cleanJson, "whatWentWell") ?: "You did a good job maintaining a steady flow."
                            val miss = extractField(cleanJson, "missedOpportunities") ?: "There were times where the dialogue felt slightly structured."
                            val tips = extractField(cleanJson, "replayTips") ?: "Try to ask open-ended questions about their day."
                            CoachApiResponse(score, emp, flo, goalS, well, miss, tips)
                        }
                    }

                    if (coachResponse != null) {
                        val rawTranscript = moshi.adapter(List::class.java).toJson(_messages.value)
                        val encryptedTranscript = com.example.security.CryptoHelper.encrypt(rawTranscript)
                        // Create Room DB entry
                        val sessionRecord = ConversationSession(
                            scenarioId = scenario.id,
                            scenarioTitle = scenario.title,
                            category = scenario.category,
                            partnerName = scenario.partnerName,
                            transcriptJson = encryptedTranscript,
                            score = coachResponse.overallScore,
                            empathyScore = coachResponse.empathyScore,
                            flowScore = coachResponse.flowScore,
                            goalAchievementScore = coachResponse.goalAchievementScore,
                            whatWentWell = coachResponse.whatWentWell,
                            missedOpportunities = coachResponse.missedOpportunities,
                            replayTips = coachResponse.replayTips,
                            suggestionsShown = sessionSuggestionsShown,
                            suggestionsUsed = sessionSuggestionsUsed
                        )

                        // Save to repository
                        repository.insertSession(sessionRecord)
                        computeProgression(sessionRecord)
                        _roleplayState.value = RoleplayState.FeedbackReady(sessionRecord)
                    } else {
                        _aiError.value = "AI Connection Error: Feedback structure was incorrect."
                        simulateFeedbackFallback(scenario, transcriptText)
                    }
                } else {
                    _aiError.value = "AI Connection Error: Empty response from social coach."
                    simulateFeedbackFallback(scenario, transcriptText)
                }

            } catch (e: Exception) {
                _aiError.value = "AI Connection Error: ${e.message}"
                simulateFeedbackFallback(scenario, transcriptText)
            }
        }
    }

    private suspend fun simulateFeedbackFallback(scenario: Scenario, transcriptText: String) {
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(2000)
            val scoreVal = _rapportLevel.value
            val empathy = (scoreVal + 5).coerceIn(10, 100)
            val flowVal = (scoreVal - 2).coerceIn(10, 100)
            val goalAch = (scoreVal + 3).coerceIn(10, 100)

            val rawTranscript = moshi.adapter(List::class.java).toJson(_messages.value)
            val encryptedTranscript = com.example.security.CryptoHelper.encrypt(rawTranscript)
            val sessionRecord = ConversationSession(
                scenarioId = scenario.id,
                scenarioTitle = scenario.title,
                category = scenario.category,
                partnerName = scenario.partnerName,
                transcriptJson = encryptedTranscript,
                score = scoreVal,
                empathyScore = empathy,
                flowScore = flowVal,
                goalAchievementScore = goalAch,
                whatWentWell = "You successfully conversed with ${scenario.partnerName} and maintained a positive tone throughout the interaction. Your pacing was supportive.",
                missedOpportunities = "You could have probed more deeply into ${scenario.partnerName}'s statements. (Gemini API key is missing, so this evaluation is generated using dynamic fallback estimation).",
                replayTips = "Set up your Gemini API Key in the AI Studio Secrets panel. This unlocks advanced deep analysis from our AI conversation coach!",
                suggestionsShown = sessionSuggestionsShown,
                suggestionsUsed = sessionSuggestionsUsed
            )

            repository.insertSession(sessionRecord)
            computeProgression(sessionRecord)
            _roleplayState.value = RoleplayState.FeedbackReady(sessionRecord)
        }
    }

    private suspend fun fetchRealTimeCoachTip(conversationHistory: String, latestMessage: String): RealTimeCoachTip? {
        if (!isApiKeyConfigured()) {
            return simulateRealTimeCoachTip(latestMessage)
        }

        val coachPrompt = """
            You are an expert social skills coach analyzing a conversation.

            Read this exchange and return ONLY a JSON object. No explanation, no 
            markdown, just raw JSON.

            CONVERSATION SO FAR:
            ${conversationHistory.ifBlank { "(No prior exchange. This is the start of the conversation.)" }}

            LATEST USER MESSAGE:
            $latestMessage

            Return this exact JSON structure:
            {
              "rapport_score": 50,
              "rapport_delta": 0,
              "signal": "NEUTRAL",
              "micro_tip": "A descriptive, helpful coaching tip.",
              "engagement_trend": "STEADY"
            }

            SCORING RULES:
            - Start rapport at 50.
            - Increase for: genuine curiosity, humor, vulnerability, specificity.
            - Decrease for: one-word answers, bragging, interviewing tone, 
              generic questions.
            - micro_tip should be actionable and specific to what just happened.
        """.trimIndent()

        return try {
            val rawText = callGroq(
                systemPrompt = null,
                chatMessages = listOf(ChatMessage("", coachPrompt, true)),
                temperature = 0.1f,
                maxTokens = 120,
                topP = 1.0f,
                frequencyPenalty = 0.0f,
                presencePenalty = 0.0f
            )
            if (rawText != null) {
                val cleanJson = extractJson(rawText)
                try {
                    coachTipAdapter.fromJson(cleanJson)
                } catch (e: Exception) {
                    val score = extractIntField(cleanJson, "rapport_score") ?: extractIntField(cleanJson, "rapportScore") ?: 50
                    val delta = extractIntField(cleanJson, "rapport_delta") ?: extractIntField(cleanJson, "rapportDelta") ?: 0
                    val signalVal = extractField(cleanJson, "signal") ?: "NEUTRAL"
                    val tip = extractField(cleanJson, "micro_tip") ?: extractField(cleanJson, "microTip") ?: "Keep going! Ask them something engaging."
                    val trend = extractField(cleanJson, "engagement_trend") ?: extractField(cleanJson, "engagementTrend") ?: "STEADY"
                    RealTimeCoachTip(score, delta, signalVal, tip, trend)
                }
            } else {
                simulateRealTimeCoachTip(latestMessage)
            }
        } catch (e: Exception) {
            _aiError.value = "AI Connection Error: ${e.message}"
            simulateRealTimeCoachTip(latestMessage)
        }
    }

    private fun simulateRealTimeCoachTip(latestMessage: String): RealTimeCoachTip {
        val length = latestMessage.trim().length
        val hasQuestion = latestMessage.contains("?")
        val rapportVal = _rapportLevel.value
        
        val delta: Int
        val signalVal: String
        val trend: String
        val tip: String

        if (length < 8) {
            delta = -5
            signalVal = "AWKWARD"
            trend = "FALLING"
            tip = "Try expanding your message for better dialog."
        } else if (hasQuestion) {
            delta = 6
            signalVal = "GREAT"
            trend = "RISING"
            tip = "Asking questions shows great interest. Good!"
        } else if (length > 40) {
            delta = 4
            signalVal = "GOOD"
            trend = "RISING"
            tip = "Sharing detailed thoughts builds strong connection."
        } else {
            delta = 2
            signalVal = "NEUTRAL"
            trend = "STEADY"
            tip = "Solid response. Try a question next."
        }

        val finalRapport = (rapportVal + delta).coerceIn(10, 100)
        return RealTimeCoachTip(
            rapport_score = finalRapport,
            rapport_delta = delta,
            signal = signalVal,
            micro_tip = tip,
            engagement_trend = trend
        )
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSessionById(sessionId)
            val idStr = sessionId.toString()
            val filtered = _savedSessions.value.filter { it.id != idStr }
            persistSavedSessions(filtered)
        }
    }

    fun resetStateToIdle() {
        stopRecordingAudio(discard = true)
        stopSpeaking()
        _voiceRecordingState.value = VoiceRecordingState.IDLE
        _voiceFeedbackMessage.value = null

        _roleplayState.value = RoleplayState.Idle
        updateMessages(emptyList())
        activeScenario = null
        _rapportLevel.value = 50
        _latestCoachTip.value = null
        _latestProgression.value = null
        sessionStartTime = 0L
    }

    // --- FEATURE 7: Live Relationship Meter Helper Functions ---
    fun getRelevantMetricsForCategory(category: String): List<String> {
        return when (category.trim()) {
            "Dating" -> listOf("Comfort", "Interest", "Chemistry", "Trust")
            "Small Talk" -> listOf("Humor", "Support", "Comfort", "Engagement")
            "Networking" -> listOf("Confidence", "Professionalism", "Trust", "Rapport")
            "Conflict Resolution" -> listOf("Empathy", "Problem Solving", "Patience", "Respect")
            else -> listOf("Trust", "Comfort", "Interest", "Respect")
        }
    }

    fun initializeRelationshipState(scenario: Scenario) {
        val category = scenario.category
        val metricNames = getRelevantMetricsForCategory(category)
        val initialMetrics = metricNames.associateWith { name ->
            RelationshipMetricDetail(
                name = name,
                score = 50,
                delta = 0,
                explanation = "Conversation has just started. Send your first message to see how $name changes!"
            )
        }
        _relationshipMetrics.value = initialMetrics
        _relationshipMood.value = "Neutral"
        _relationshipTimeline.value = emptyList()
        _relationshipFloatingDeltas.value = emptyList()
        _smartAlert.value = null
        _relationshipSummary.value = null

        val initialAchievements = listOf(
            RelationshipAchievement("trust_builder", "Trust Builder", "Reach 80% Trust in any scenario", "shield"),
            RelationshipAchievement("charmer", "Conversation Charmer", "Gain a +8 or higher delta in Comfort", "star"),
            RelationshipAchievement("rapport_master", "Rapport Master", "Maintain an overall Connection above 75% for 3 turns", "crown"),
            RelationshipAchievement("listener", "Great Listener", "Achieve 85% Respect by validating their perspective", "ear"),
            RelationshipAchievement("architect", "Relationship Architect", "Complete a scenario with all metrics above 70%", "building")
        )
        _relationshipAchievements.value = initialAchievements
    }

    fun updateRelationshipMetrics(
        userMessage: String,
        partnerResponse: String,
        coachTip: RealTimeCoachTip,
        scenario: Scenario
    ) {
        val current = _relationshipMetrics.value.toMutableMap()
        val deltas = mutableListOf<Pair<String, Int>>()

        val delta = coachTip.rapport_delta
        val textLower = userMessage.lowercase()

        val isEmpathetic = textLower.contains("feel") || textLower.contains("sorry") || 
                            textLower.contains("understand") || textLower.contains("agree") || 
                            textLower.contains("perspective") || textLower.contains("hear")
        val hasQuestion = textLower.contains("?")
        val isPolite = textLower.contains("please") || textLower.contains("thank") || 
                         textLower.contains("appreciate") || textLower.contains("kind")
        val isConfident = textLower.contains("confident") || textLower.contains("believe") || 
                            textLower.contains("expert") || textLower.contains("sure") || 
                            textLower.contains("skills") || textLower.contains("experience")
        val hasHumor = textLower.contains("haha") || textLower.contains("lol") || 
                         textLower.contains("joke") || textLower.contains("funny")

        current.forEach { (name, detail) ->
            var metricDelta = when (name) {
                "Trust", "Empathy" -> {
                    if (delta > 0) {
                        if (isEmpathetic) delta + 2 else (delta * 0.8).toInt()
                    } else {
                        if (isEmpathetic) delta / 2 else delta
                    }
                }
                "Comfort", "Support" -> {
                    if (delta > 0) {
                        if (isEmpathetic || isPolite) delta + 1 else (delta * 0.7).toInt()
                    } else {
                        if (isEmpathetic) delta / 2 else (delta * 1.2).toInt()
                    }
                }
                "Interest", "Curiosity", "Chemistry" -> {
                    if (delta > 0) {
                        if (hasQuestion) delta + 3 else (delta * 1.1).toInt()
                    } else {
                        if (hasQuestion) delta / 3 else delta
                    }
                }
                "Professionalism", "Respect" -> {
                    if (delta > 0) {
                        if (isPolite) delta + 2 else (delta * 0.9).toInt()
                    } else {
                        if (isPolite) delta / 2 else (delta * 1.3).toInt()
                    }
                }
                "Confidence", "Leadership" -> {
                    if (delta > 0) {
                        if (isConfident) delta + 3 else (delta * 1.0).toInt()
                    } else {
                        if (isConfident) delta / 2 else delta
                    }
                }
                "Humor" -> {
                    if (delta > 0) {
                        if (hasHumor) delta + 4 else (delta * 0.6).toInt()
                    } else {
                        delta / 2
                    }
                }
                "Engagement", "Communication", "Rapport" -> {
                    if (delta > 0) {
                        if (userMessage.length > 40) delta + 2 else (delta * 0.9).toInt()
                    } else {
                        if (userMessage.length < 15) delta - 2 else delta
                    }
                }
                else -> delta
            }

            if (delta > 0) {
                metricDelta = metricDelta.coerceIn(1, 10)
            } else if (delta < 0) {
                metricDelta = metricDelta.coerceIn(-10, -1)
            } else {
                metricDelta = 0
            }

            val newScore = (detail.score + metricDelta).coerceIn(10, 100)

            val reason = when {
                metricDelta > 0 && name in listOf("Trust", "Empathy", "Comfort", "Support") -> {
                    if (isEmpathetic) {
                        "You actively validated their perspective and showed genuine emotional safety."
                    } else {
                        "Your respectful and polite tone created a comfortable conversational environment."
                    }
                }
                metricDelta > 0 && name in listOf("Interest", "Curiosity", "Chemistry") -> {
                    if (hasQuestion) {
                        "Your open-ended question was highly engaging and invited them to share more."
                    } else {
                        "You matched their vibe and kept the focus on their thoughts, building excitement."
                    }
                }
                metricDelta > 0 && name in listOf("Professionalism", "Respect", "Confidence") -> {
                    "You articulated your thoughts clearly and professionally, conveying competence."
                }
                metricDelta > 0 -> {
                    "Your message flowed naturally, adding positive energy to the exchange."
                }

                metricDelta < 0 && name in listOf("Trust", "Empathy", "Comfort", "Support") -> {
                    if (userMessage.length < 15) {
                        "A short response here feels cold and reduces emotional safety."
                    } else {
                        "The conversation feels slightly defensive or interrogative, lowering comfort."
                    }
                }
                metricDelta < 0 && name in listOf("Interest", "Curiosity", "Chemistry", "Engagement") -> {
                    "Your reply ended with a statement rather than asking a question, making it harder to respond."
                }
                metricDelta < 0 -> {
                    "The message was a bit dry or off-topic, causing a slight dip in connection."
                }
                else -> "The flow is stable. Keep sharing details and asking curious questions to build rapport."
            }

            current[name] = RelationshipMetricDetail(
                name = name,
                score = newScore,
                delta = metricDelta,
                explanation = reason
            )

            if (metricDelta != 0) {
                deltas.add(Pair(name, metricDelta))
            }
        }

        _relationshipMetrics.value = current
        _relationshipFloatingDeltas.value = deltas

        val score = coachTip.rapport_score
        _relationshipMood.value = when {
            score >= 85 -> "Excited"
            score >= 70 -> "Comfortable"
            score >= 55 -> "Curious"
            score >= 45 -> "Neutral"
            score >= 35 -> "Awkward"
            score >= 25 -> "Confused"
            score >= 15 -> "Disappointed"
            else -> "Frustrated"
        }

        val newPoint = RelationshipTimelinePoint(
            messageId = UUID.randomUUID().toString(),
            userMessageText = userMessage,
            partnerResponseText = partnerResponse,
            timestamp = System.currentTimeMillis(),
            overallConnection = score,
            mood = _relationshipMood.value,
            metrics = current.toMap()
        )
        val updatedTimeline = _relationshipTimeline.value.toMutableList()
        updatedTimeline.add(newPoint)
        _relationshipTimeline.value = updatedTimeline

        val alert = when {
            score < 40 -> "Connection is low. Try acknowledging their perspective or asking an open-ended question."
            _relationshipMetrics.value["Trust"]?.score ?: 100 < 45 -> "Trust is decreasing. Try validating their feelings first."
            _relationshipMetrics.value["Engagement"]?.score ?: 100 < 45 || _relationshipMetrics.value["Interest"]?.score ?: 100 < 45 -> {
                "Engagement is falling. Ask them something curious about what they just said!"
            }
            coachTip.micro_tip.isNotBlank() && Math.random() > 0.5 -> "Coach recommendation: ${coachTip.micro_tip}"
            else -> null
        }
        _smartAlert.value = alert

        val updatedAchievements = _relationshipAchievements.value.map { achievement ->
            val isUnlocked = when (achievement.id) {
                "trust_builder" -> (current["Trust"]?.score ?: 0) >= 80 || (current["Empathy"]?.score ?: 0) >= 80
                "charmer" -> deltas.any { it.first == "Comfort" && it.second >= 8 } || deltas.any { it.first == "Interest" && it.second >= 8 }
                "rapport_master" -> updatedTimeline.size >= 3 && updatedTimeline.takeLast(3).all { it.overallConnection >= 75 }
                "listener" -> (current["Respect"]?.score ?: 0) >= 85 || (current["Support"]?.score ?: 0) >= 85
                "architect" -> current.values.all { it.score >= 70 }
                else -> achievement.isUnlocked
            }
            achievement.copy(isUnlocked = achievement.isUnlocked || isUnlocked)
        }
        _relationshipAchievements.value = updatedAchievements
    }

    fun generateRelationshipSummary(scenario: Scenario) {
        val timeline = _relationshipTimeline.value
        val metrics = _relationshipMetrics.value

        val highestTrust = timeline.maxOfOrNull { it.metrics["Trust"]?.score ?: 50 } ?: metrics["Trust"]?.score ?: 50
        val lowestEngagement = timeline.minOfOrNull { it.metrics["Engagement"]?.score ?: 50 } ?: metrics["Engagement"]?.score ?: 50

        val bestPoint = timeline.maxByOrNull { it.overallConnection }
        val bestMoment = if (bestPoint != null) {
            "User: \"${bestPoint.userMessageText.take(45)}...\"\nPartner: \"${bestPoint.partnerResponseText.take(45)}...\""
        } else {
            "You starting the chat warmly with ${scenario.partnerName}."
        }

        val worstPoint = timeline.minByOrNull { it.overallConnection }
        val mostAwkwardMoment = if (worstPoint != null && worstPoint.overallConnection < 50) {
            "User: \"${worstPoint.userMessageText.take(45)}...\"\nPartner: \"${worstPoint.partnerResponseText.take(45)}...\""
        } else {
            "None! You navigated the conversation smoothly without any major blunders."
        }

        val initialConnection = 50
        val finalConnection = _rapportLevel.value
        val growth = finalConnection - initialConnection

        val summary = mapOf(
            "highest_trust" to highestTrust,
            "lowest_engagement" to lowestEngagement,
            "best_moment" to bestMoment,
            "most_awkward_moment" to mostAwkwardMoment,
            "growth" to growth,
            "overall_rapport" to finalConnection
        )
        _relationshipSummary.value = summary
    }

    // Helper functions for parsing JSON safely if parser gets nested text
    private fun extractJson(input: String): String {
        var clean = input.trim()
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    private fun extractField(json: String, fieldName: String): String? {
        val pattern = "\"$fieldName\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractIntField(json: String, fieldName: String): Int? {
        val pattern = "\"$fieldName\"\\s*:\\s*([0-9]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun addCustomScenario(scenario: Scenario) {
        val updated = _customScenarios.value + scenario
        _customScenarios.value = updated
        _allScenarios.value = com.example.data.model.ScenarioCatalog.scenarios + _customScenarios.value
    }

    fun generateScenario(userInput: String, onSuccess: (Scenario) -> Unit, onError: (String) -> Unit) {
        if (userInput.isBlank()) {
            onError("Please enter a description of the scenario you'd like to practice!")
            return
        }
        _isGeneratingScenario.value = true
        viewModelScope.launch {
            try {
                if (!isApiKeyConfigured()) {
                    val scenario = simulateScenarioResult(userInput)
                    addCustomScenario(scenario)
                    onSuccess(scenario)
                    return@launch
                }

                val prompt = """
                    You are a scenario architect for Conversable, a social skills training app.

                    A user has described a situation they want to practice. Your job is to 
                    turn their rough description into a complete, structured training scenario.

                    USER INPUT: "$userInput"

                    Return ONLY a JSON object in exactly this format, nothing else:

                    {
                      "scenario_title": "<short punchy title, max 5 words>",
                      "category": "<one of: Dating & Romance | Professional & Career | Conflict Resolution | Networking | Client Relations | Social & Friendship>",
                      "difficulty": "<one of: EASY | MEDIUM | HARD>",
                      "setting": "<one sentence describing the physical location and moment>",
                      "persona": {
                        "name": "<realistic first name>",
                        "age": 28,
                        "occupation": "<job or role>",
                        "personality": "<2–3 sentence description of how they act, what they like, what puts them off>",
                        "current_mood": "<one of: Warm | Neutral | Guarded | Distracted | Stressed | Excited>"
                      },
                      "scenario_context": "<2–3 sentences describing the full situation from the persona's point of view>",
                      "hidden_goal": "<what the user must achieve in this conversation to win, written as a secret challenge>",
                      "opening_line": "<the first thing the AI persona says to kick off the conversation>",
                      "win_conditions": [
                        "<specific thing user must do to succeed #1>",
                        "<specific thing user must do to succeed #2>",
                        "<specific thing user must do to succeed #3>"
                      ],
                      "fail_triggers": [
                        "<thing that would cause the persona to disengage #1>",
                        "<thing that would cause the persona to disengage #2>"
                      ],
                      "difficulty_reason": "<one sentence explaining why this difficulty rating was chosen>"
                    }

                    RULES:
                    - Make personas feel like real people, not archetypes
                    - Base difficulty on how socially complex the situation is
                    - Win conditions must be specific and observable
                    - The opening line must feel natural, not like a prompt
                    - Never use generic names like "Alex" — pick culturally specific names 
                      based on the scenario context
                """.trimIndent()

                val rawText = callGroq(
                    systemPrompt = null,
                    chatMessages = listOf(ChatMessage("", prompt, true)),
                    temperature = 0.7f,
                    maxTokens = 600,
                    topP = 0.95f,
                    frequencyPenalty = 0.0f,
                    presencePenalty = 0.0f
                )
                if (rawText != null) {
                    val cleanJson = extractJson(rawText)
                    val generatedResp = withContext(Dispatchers.Default) {
                        try {
                            moshi.adapter(CustomScenarioResponse::class.java).fromJson(cleanJson)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (generatedResp != null) {
                        val mappedCategory = when (generatedResp.category) {
                            "Dating & Romance" -> "Dating"
                            "Professional & Career", "Client Relations", "Networking" -> "Networking"
                            "Conflict Resolution" -> "Conflict Resolution"
                            "Social & Friendship" -> "Small Talk"
                            else -> "Small Talk"
                        }

                        val mappedDifficulty = when (generatedResp.difficulty.uppercase()) {
                            "EASY" -> "Easy"
                            "MEDIUM" -> "Medium"
                            else -> "Hard"
                        }

                        val pickedAvatar = selectAvatarForScenario(mappedCategory, generatedResp.persona.occupation)

                        val builtScenario = Scenario(
                            id = "custom_" + UUID.randomUUID().toString(),
                            title = generatedResp.scenario_title,
                            category = mappedCategory,
                            partnerName = generatedResp.persona.name,
                            partnerAvatar = pickedAvatar,
                            partnerPersona = "${generatedResp.persona.personality} (Age ${generatedResp.persona.age}, ${generatedResp.persona.occupation}, starting mood is ${generatedResp.persona.current_mood}).",
                            scenarioDescription = "${generatedResp.setting} ${generatedResp.scenario_context}",
                            hiddenGoal = generatedResp.hidden_goal,
                            difficulty = mappedDifficulty,
                            initialMessage = generatedResp.opening_line
                        )

                        addCustomScenario(builtScenario)
                        onSuccess(builtScenario)
                    } else {
                        _aiError.value = "AI Connection Error: Scenario format was incorrect."
                        val scenario = simulateScenarioResult(userInput)
                        addCustomScenario(scenario)
                        onSuccess(scenario)
                    }
                } else {
                    _aiError.value = "AI Connection Error: Empty response from scenario architect."
                    val scenario = simulateScenarioResult(userInput)
                    addCustomScenario(scenario)
                    onSuccess(scenario)
                }

            } catch (e: Exception) {
                _aiError.value = "AI Connection Error: ${e.message ?: "Unknown error"}"
                val scenario = simulateScenarioResult(userInput)
                addCustomScenario(scenario)
                onSuccess(scenario)
            } finally {
                _isGeneratingScenario.value = false
            }
        }
    }

    private fun selectAvatarForScenario(category: String, occupation: String): String {
        val occLower = occupation.lowercase()
        return when {
            occLower.contains("landlord") || occLower.contains("rent") -> "H"
            occLower.contains("boss") || occLower.contains("manager") || occLower.contains("director") -> "M"
            occLower.contains("doctor") || occLower.contains("nurse") || occLower.contains("med") -> "D"
            occLower.contains("teacher") || occLower.contains("professor") || occLower.contains("coach") -> "T"
            occLower.contains("waiter") || occLower.contains("chef") || occLower.contains("cook") -> "C"
            category == "Dating" -> listOf("D", "M", "R", "L").random()
            category == "Networking" -> listOf("C", "T", "W", "F").random()
            category == "Conflict Resolution" -> listOf("U", "F", "A", "S").random()
            else -> listOf("W", "D", "F", "P", "S").random()
        }
    }

    private fun simulateScenarioResult(userInput: String): Scenario {
        val inputLower = userInput.lowercase()
        
        val title: String
        val partnerName: String
        val category: String
        val avatar: String
        val personaDesc: String
        val description: String
        val hiddenGoal: String
        val difficulty: String
        val initialMsg: String

        when {
            inputLower.contains("landlord") || inputLower.contains("roof") || inputLower.contains("apartment") || inputLower.contains("rent") -> {
                title = "The Grumpy Landlord"
                partnerName = "Mr. Henderson"
                category = "Conflict Resolution"
                avatar = "H"
                difficulty = "Medium"
                personaDesc = "A busy, practical landlord who has heard every excuse in the book. He dislikes whiny tenants but responds well to professional, polite, and direct requests with concrete proposals."
                description = "You're meeting Mr. Henderson in the courtyard of your apartment complex. You need to discuss a pressing maintenance issue (a leaky ceiling) that has been neglected for two weeks."
                hiddenGoal = "Politely but firmly secure a written commitment from Mr. Henderson that a plumber will be sent within 48 hours, without getting on his bad side."
                initialMsg = "Oh, hi. Good timing, I was just checking on the lawn. Is everything alright in 4B?"
            }
            inputLower.contains("boss") || inputLower.contains("salary") || inputLower.contains("raise") || inputLower.contains("job") || inputLower.contains("manager") -> {
                title = "The Salary Negotiation"
                partnerName = "Sarah"
                category = "Networking"
                avatar = "M"
                difficulty = "Hard"
                personaDesc = "Sarah is a seasoned managing director who values hard data, personal initiative, and company loyalty. She dislikes emotional pleas and respects clear, business-driven arguments."
                description = "You have scheduled a 15-minute 1-on-1 meeting in Sarah's modern glass office to state your case for a promotion and salary review after delivering a highly successful project."
                hiddenGoal = "Effectively pitch your value, handle her initial objection about current budget freezes, and schedule a formal review meeting next week."
                initialMsg = "Come on in, close the door. I've got a busy afternoon, but I'm glad we could fit this in. What's on your mind?"
            }
            inputLower.contains("date") || inputLower.contains("romantic") || inputLower.contains("match") || inputLower.contains("girlfriend") || inputLower.contains("boyfriend") -> {
                title = "The Elegant Restaurant Date"
                partnerName = "Maya"
                category = "Dating"
                avatar = "D"
                difficulty = "Medium"
                personaDesc = "An elegant, ambitious marketing specialist in her late 20s. She loves fine arts, deep life stories, and creative travel ideas. She is turned off by bragging or superficial questions."
                description = "You are sitting at a dimly lit corner booth in a beautiful Italian restaurant. The soft music is playing, and she is looking at you with a pleasant but guarded curiosity."
                hiddenGoal = "Find a meaningful shared connection about art or travel, show deep active listening, and get her excited to schedule a second creative date."
                initialMsg = "This place has such a lovely atmosphere, doesn't it? Thank you for choosing it. What caught your eye on the menu first?"
            }
            else -> {
                val cleanWords = userInput.replace("[^a-zA-Z0-9\\s]".toRegex(), "").split(" ").filter { it.length > 4 }
                val keyword = if (cleanWords.isNotEmpty()) cleanWords.random() else "practice"
                val capitalizedKeyword = keyword.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                
                title = "Dynamic Practice Room"
                partnerName = "Alex"
                category = "Small Talk"
                avatar = "A"
                difficulty = "Medium"
                personaDesc = "A highly adaptive conversation assistant designed to help you practice challenging scenarios. Friendly but realistic, they will test your social skills with natural friction."
                description = "You have entered a simulated environment focused on: '$userInput'. Practice your communication objectives under challenging conditions."
                hiddenGoal = "Deliver clear, empathetic statements, navigate objections gracefully, and reach a positive consensus about $keyword."
                initialMsg = "Hi there! I'm glad we could connect to chat about this. What would you like to discuss regarding $capitalizedKeyword?"
            }
        }

        return Scenario(
            id = "custom_" + UUID.randomUUID().toString(),
            title = title,
            category = category,
            partnerName = partnerName,
            partnerAvatar = avatar,
            partnerPersona = "$personaDesc (Demo mode simulated generation).",
            scenarioDescription = description,
            hiddenGoal = hiddenGoal,
            difficulty = difficulty,
            initialMessage = initialMsg
        )
    }

    private fun getActiveUserKey(): String {
        val rawEmail = sharedPrefs.getString("user_email", "") ?: ""
        return if (rawEmail.isNotEmpty()) {
            val decrypted = try {
                com.example.security.CryptoHelper.decrypt(rawEmail)
            } catch (e: Exception) {
                null
            }
            val email = decrypted ?: rawEmail
            email.lowercase().replace(".", "_")
        } else {
            "default_user"
        }
    }

    private var dbJob: kotlinx.coroutines.Job? = null

    fun refreshProgressData() {
        val userKey = getActiveUserKey()
        val scopedStreakKey = "user_streak_$userKey"
        
        // 1. Migrate global keys to user-scoped keys if scoped key does not exist yet
        if (!sharedPrefs.contains(scopedStreakKey)) {
            val editor = sharedPrefs.edit()
            if (sharedPrefs.contains("user_streak")) {
                // Migrate existing user's global data to their user-scoped keys
                editor.putInt("user_total_xp_$userKey", sharedPrefs.getInt("user_total_xp", 0))
                editor.putInt("user_streak_$userKey", sharedPrefs.getInt("user_streak", 0))
                editor.putInt("user_longest_streak_$userKey", sharedPrefs.getInt("user_longest_streak", 0))
                editor.putInt("streak_freezes_left_$userKey", sharedPrefs.getInt("streak_freezes_left", 2))
                editor.putInt("total_practice_days_$userKey", sharedPrefs.getInt("total_practice_days", 0))
                editor.putInt("perfect_weeks_$userKey", sharedPrefs.getInt("perfect_weeks", 0))
                editor.putInt("perfect_months_$userKey", sharedPrefs.getInt("perfect_months", 0))
                
                val completedDates = sharedPrefs.getStringSet("completed_dates_set", emptySet())
                editor.putStringSet("completed_dates_set_$userKey", completedDates)
                
                val unlockedBadges = sharedPrefs.getStringSet("unlocked_badges", emptySet())
                editor.putStringSet("unlocked_badges_$userKey", unlockedBadges)
                
                val completedCategories = sharedPrefs.getStringSet("completed_categories", emptySet())
                editor.putStringSet("completed_categories_$userKey", completedCategories)
                
                val completedScenarios = sharedPrefs.getStringSet("completed_scenarios_today", emptySet())
                editor.putStringSet("completed_scenarios_today_$userKey", completedScenarios)
                
                val dailyChallengesJson = sharedPrefs.getString("completed_daily_challenges_json", null)
                if (dailyChallengesJson != null) {
                    editor.putString("completed_daily_challenges_json_$userKey", dailyChallengesJson)
                }
                
                val lastSessionDate = sharedPrefs.getString("last_session_date", null)
                if (lastSessionDate != null) {
                    editor.putString("last_session_date_$userKey", lastSessionDate)
                }
                
                val completedTodayDate = sharedPrefs.getString("completed_today_date", null)
                if (completedTodayDate != null) {
                    editor.putString("completed_today_date_$userKey", completedTodayDate)
                }
            } else {
                // Initialize default fresh zero progress for new user
                editor.putInt("user_total_xp_$userKey", 0)
                editor.putInt("user_streak_$userKey", 0)
                editor.putInt("user_longest_streak_$userKey", 0)
                editor.putInt("streak_freezes_left_$userKey", 2)
                editor.putInt("total_practice_days_$userKey", 0)
                editor.putInt("perfect_weeks_$userKey", 0)
                editor.putInt("perfect_months_$userKey", 0)
                editor.putStringSet("completed_dates_set_$userKey", emptySet())
                editor.putStringSet("unlocked_badges_$userKey", emptySet())
                editor.putStringSet("completed_categories_$userKey", emptySet())
                editor.putStringSet("completed_scenarios_today_$userKey", emptySet())
            }
            editor.apply()
        }

        // 2. Load the user-scoped progress values
        _totalXp.value = sharedPrefs.getInt("user_total_xp_$userKey", 0)
        _streak.value = sharedPrefs.getInt("user_streak_$userKey", 0)
        _longestStreak.value = sharedPrefs.getInt("user_longest_streak_$userKey", 0)
        _streakFreezesLeft.value = sharedPrefs.getInt("streak_freezes_left_$userKey", 2)
        _totalPracticeDays.value = sharedPrefs.getInt("total_practice_days_$userKey", 0)
        _perfectWeeks.value = sharedPrefs.getInt("perfect_weeks_$userKey", 0)
        _perfectMonths.value = sharedPrefs.getInt("perfect_months_$userKey", 0)
        _completedDates.value = sharedPrefs.getStringSet("completed_dates_set_$userKey", emptySet()) ?: emptySet()
        _streakSavedMessage.value = sharedPrefs.getString("streak_saved_message_$userKey", null)
        _unlockedBadgeIds.value = sharedPrefs.getStringSet("unlocked_badges_$userKey", emptySet()) ?: emptySet()

        // 3. Load completed daily challenges
        _completedDailyChallenges.value = loadCompletedDailyChallenges()

        // 4. Update today's scenarios completion based on userKey
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastSavedDate = sharedPrefs.getString("completed_today_date_$userKey", "") ?: ""
        if (lastSavedDate == todayStr) {
            _scenariosCompletedToday.value = sharedPrefs.getStringSet("completed_scenarios_today_$userKey", emptySet()) ?: emptySet()
        } else {
            _scenariosCompletedToday.value = emptySet()
            sharedPrefs.edit()
                .putString("completed_today_date_$userKey", todayStr)
                .putStringSet("completed_scenarios_today_$userKey", emptySet())
                .apply()
        }

        // 5. Re-initialize database & DAOs
        val database = AppDatabase.getDatabase(getApplication(), userKey)
        repository = SessionRepository(database.sessionDao())
        personaRepository = PersonaRepository(database.personaDao())
        
        personaMemoryDao = database.personaMemoryDao()
        marketplaceScenarioDao = database.marketplaceScenarioDao()
        analyzedChatDao = database.analyzedChatDao()
        liveCoachingSessionDao = database.liveCoachingSessionDao()
        contactReminderDao = database.contactReminderDao()
        groupMeetingSessionDao = database.groupMeetingSessionDao()
        vocalCameraSessionDao = database.vocalCameraSessionDao()

        // 6. Collect from new database flows
        dbJob?.cancel()
        dbJob = viewModelScope.launch {
            launch {
                repository.allSessions.collect {
                    _allSessions.value = it
                }
            }
            launch {
                repository.averageScore.collect {
                    _averageScore.value = it
                }
            }
            launch {
                repository.sessionCount.collect {
                    _sessionCount.value = it
                }
            }
            launch {
                personaRepository.allPersonas.collect {
                    _allPersonas.value = it
                }
            }
            launch {
                analyzedChatDao.getAllAnalyzedChats().collect {
                    _analyzedChats.value = it
                }
            }
            launch {
                marketplaceScenarioDao.getAllMarketplaceScenarios().collect {
                    _marketplaceScenarios.value = it
                }
            }
            launch {
                liveCoachingSessionDao.getAllSessions().collect {
                    _liveCoachingSessions.value = it
                }
            }
            launch {
                contactReminderDao.getAllContactReminders().collect {
                    _contactReminders.value = it
                }
            }
            launch {
                groupMeetingSessionDao.getAllGroupMeetingSessions().collect {
                    _groupMeetingSessions.value = it
                }
            }
            launch {
                vocalCameraSessionDao.getAllVocalCameraSessions().collect {
                    _vocalCameraSessions.value = it
                }
            }
        }
    }

    private fun getLevelForXp(xp: Int): Int {
        return when {
            xp < 500 -> 1
            xp < 1200 -> 2
            xp < 2200 -> 3
            xp < 3500 -> 4
            xp < 5200 -> 5
            xp < 7500 -> 6
            xp < 10500 -> 7
            xp < 14500 -> 8
            xp < 20000 -> 9
            else -> 10
        }
    }

    private fun getLevelTitle(level: Int): String {
        return when (level) {
            1 -> "Nervous Beginner"
            2 -> "Getting Started"
            3 -> "Building Confidence"
            4 -> "Socially Aware"
            5 -> "Smooth Talker"
            6 -> "Charisma Rising"
            7 -> "Natural Connector"
            8 -> "Social Dynamo"
            9 -> "Elite Conversationalist"
            else -> "Conversable Master"
        }
    }

    private fun getDaysBetween(date1Str: String, date2Str: String): Long {
        if (date1Str.isEmpty() || date2Str.isEmpty()) return -1L
        if (date1Str == date2Str) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date1 = sdf.parse(date1Str) ?: return -1L
            val date2 = sdf.parse(date2Str) ?: return -1L
            val diffMs = date2.time - date1.time
            TimeUnit.MILLISECONDS.toDays(diffMs)
        } catch (e: java.lang.Exception) {
            -1L
        }
    }

    private fun computeProgression(session: ConversationSession) {
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("conversable_prefs", Context.MODE_PRIVATE)
        val userKey = getActiveUserKey()

        val xpBefore = sharedPrefs.getInt("user_total_xp_$userKey", 0)
        val streakBefore = sharedPrefs.getInt("user_streak_$userKey", 0)
        val lastSessionDate = sharedPrefs.getString("last_session_date_$userKey", "") ?: ""
        val completedCategories = sharedPrefs.getStringSet("completed_categories_$userKey", emptySet()) ?: emptySet()
        val completedTodayDate = sharedPrefs.getString("completed_today_date_$userKey", "") ?: ""
        val unlockedBadgesSet = sharedPrefs.getStringSet("unlocked_badges_$userKey", emptySet()) ?: emptySet()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date(System.currentTimeMillis()))

        // Reset today's scenarios if day flipped
        val scenariosTodayMutable = if (completedTodayDate == todayStr) {
            sharedPrefs.getStringSet("completed_scenarios_today_$userKey", emptySet())?.toMutableSet() ?: mutableSetOf()
        } else {
            mutableSetOf()
        }

        // Base XP
        val difficultyLower = activeScenario?.difficulty?.lowercase() ?: "medium"
        val baseForDifficulty = when (difficultyLower) {
            "easy" -> 50
            "medium" -> 100
            "hard" -> 175
            else -> 100
        }

        // Time spent and turn count calculations
        val timeSpentMs = if (sessionStartTime > 0L) System.currentTimeMillis() - sessionStartTime else 120_000L // default 2 mins
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatMessage::class.java)
        val chatList: List<ChatMessage> = try {
            val decryptedJson = com.example.security.CryptoHelper.decrypt(session.transcriptJson) ?: session.transcriptJson
            moshi.adapter<List<ChatMessage>>(listType).fromJson(decryptedJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val turnCount = chatList.count { it.isUser }

        // Bonuses check list
        val bonuses = mutableListOf<BonusItem>()

        if (session.score in 90..100) {
            bonuses.add(BonusItem("Flawless", 50))
        } else if (session.score in 75..89) {
            bonuses.add(BonusItem("Strong performance", 25))
        } else if (session.score in 60..74) {
            bonuses.add(BonusItem("Solid effort", 10))
        }

        if (timeSpentMs < 5 * 60 * 1000) {
            bonuses.add(BonusItem("Speed run", 15))
        }

        if (turnCount > 15) {
            bonuses.add(BonusItem("Deep conversation", 20))
        }

        if (scenariosTodayMutable.isEmpty()) {
            bonuses.add(BonusItem("Daily starter", 30))
        }

        if (scenariosTodayMutable.size + 1 == 3) {
            bonuses.add(BonusItem("On a roll", 40))
        }

        val isDailyChallenge = session.scenarioId == "networking_tech_conference"
        val alreadyCompletedChallengeToday = scenariosTodayMutable.contains("networking_tech_conference")
        if (isDailyChallenge && !alreadyCompletedChallengeToday) {
            bonuses.add(BonusItem("Daily Challenge", 50))
        }

        // Suggestions followed bonus (5 XP each, max 25 XP)
        if (session.suggestionsUsed >= 1) {
            val amount = (session.suggestionsUsed * 5).coerceAtMost(25)
            bonuses.add(BonusItem("Used coach suggestions", amount))
        }

        // Streak updates
        var newStreak = streakBefore
        var streakBroken = false
        var streakFreezeUsed = false
        var freezesLeft = sharedPrefs.getInt("streak_freezes_left_$userKey", 2)
        var streakSavedMsg: String? = null

        if (lastSessionDate.isEmpty()) {
            newStreak = 1
        } else {
            val daysBetween = getDaysBetween(lastSessionDate, todayStr)
            if (daysBetween == 0L) {
                newStreak = streakBefore
            } else if (daysBetween == 1L) {
                newStreak = streakBefore + 1
            } else if (daysBetween == 2L) {
                if (freezesLeft > 0) {
                    newStreak = streakBefore
                    streakFreezeUsed = true
                    freezesLeft -= 1
                    streakSavedMsg = "Streak Saved: Your streak freeze was used."
                } else {
                    newStreak = 1
                    streakBroken = true
                }
            } else {
                newStreak = 1
                streakBroken = true
            }
        }

        val longestStreakBefore = sharedPrefs.getInt("user_longest_streak_$userKey", 0)
        val newLongestStreak = maxOf(newStreak, longestStreakBefore)
        
        val completedDatesSet = sharedPrefs.getStringSet("completed_dates_set_$userKey", emptySet())?.toMutableSet() ?: mutableSetOf()
        val totalPracticeDaysBefore = sharedPrefs.getInt("total_practice_days_$userKey", 0)
        val newTotalPracticeDays = if (completedDatesSet.add(todayStr)) {
            totalPracticeDaysBefore + 1
        } else {
            totalPracticeDaysBefore
        }

        if (newStreak >= 7) {
            bonuses.add(BonusItem("Week warrior", 60))
        }
        if (newStreak >= 30) {
            bonuses.add(BonusItem("Unstoppable", 150))
        }

        if (!completedCategories.contains(session.category)) {
            bonuses.add(BonusItem("New territory", 35))
        }

        val allPastSessions = allSessions.value
        if (allPastSessions.isEmpty()) {
            bonuses.add(BonusItem("First steps", 100))
        }

        val totalSessionXp = baseForDifficulty + bonuses.sumOf { it.amount }
        val xpAfter = xpBefore + totalSessionXp

        val levelBefore = getLevelForXp(xpBefore)
        val levelAfter = getLevelForXp(xpAfter)
        val levelTitleBefore = getLevelTitle(levelBefore)
        val levelTitleAfter = getLevelTitle(levelAfter)
        val leveledUp = levelAfter > levelBefore

        // Badge system checks
        val newBadges = mutableListOf<BadgeUnlocked>()
        val freshlyUnlockedBadgeIds = mutableSetOf<String>()

        fun earnBadge(id: String, name: String, reason: String) {
            if (!unlockedBadgesSet.contains(id)) {
                newBadges.add(BadgeUnlocked(id, name, reason))
                freshlyUnlockedBadgeIds.add(id)
            }
        }

        if (allPastSessions.isEmpty()) {
            earnBadge("First Blood", "First Blood", "Completed your first ever social skills training session!")
        }
        if (session.category.contains("Dating", ignoreCase = true) && session.score >= 80) {
            earnBadge("Date Whisperer", "Date Whisperer", "Scored 80+ in a Dating and Romance scenario.")
        }
        if ((session.category.contains("Professional", ignoreCase = true) || session.category.contains("Networking", ignoreCase = true)) && session.score >= 80) {
            earnBadge("Boardroom Boss", "Boardroom Boss", "Scored 80+ in a Professional and Career scenario.")
        }
        if (session.category.contains("Conflict", ignoreCase = true) && session.score >= 80) {
            earnBadge("Peace Keeper", "Peace Keeper", "Scored 80+ in a Conflict Resolution scenario.")
        }
        val pastNetworkingCount = allPastSessions.count { it.category.contains("Networking", ignoreCase = true) }
        if (session.category.contains("Networking", ignoreCase = true) && (pastNetworkingCount + 1) >= 3) {
            earnBadge("Networker", "Networker", "Successfully completed 3 or more Networking scenarios.")
        }
        if (session.score == 100) {
            earnBadge("Perfect Ten", "Perfect Ten", "Achieved a perfect score of 100 in a session!")
        }
        if (newStreak >= 7) {
            earnBadge("Week Warrior", "Week Warrior", "Earned by reaching a 7-day practice streak.")
        }
        if (newStreak >= 30) {
            earnBadge("Month Legend", "Month Legend", "Earned by reaching an elite 30-day practice streak.")
        }
        val uniqueCategories = (allPastSessions.map { it.category } + session.category).distinct().size
        if (uniqueCategories >= 4) {
            earnBadge("Variety Pack", "Variety Pack", "Completed scenarios across 4 different social categories.")
        }
        if (timeSpentMs < 180_000L) { // under 3 mins
            earnBadge("Speed Demon", "Speed Demon", "Completed a social scenario in under 3 minutes.")
        }
        if (turnCount >= 20) {
            earnBadge("Deep Diver", "Deep Diver", "Sustained a deep conversation with 20+ turns.")
        }
        if (difficultyLower == "hard" && session.score >= 75) {
            earnBadge("Hard Mode Hero", "Hard Mode Hero", "Won a Hard difficulty scenario with a score of 75+.")
        }
        val hasLowPastSession = allPastSessions.any { it.score < 50 }
        if (hasLowPastSession && session.score >= 70) {
            earnBadge("Comeback Kid", "Comeback Kid", "Bounced back to score 70+ after a previous low score.")
        }
        if (scenariosTodayMutable.size + 1 >= 3) {
            earnBadge("Daily Grinder", "Daily Grinder", "Completed 3 social training sessions in a single day.")
        }
        if (session.empathyScore >= 95) {
            earnBadge("Listener", "Listener", "Scored a perfect score in active listening.")
            earnBadge("Empath", "Empath", "Scored a perfect score in heart and empathy.")
        }

        // Skill deltas calculations
        val currentEmpathy = session.empathyScore / 20.0
        val currentListening = session.flowScore / 20.0
        val currentConfidence = session.goalAchievementScore / 20.0
        val currentCollaboration = (session.empathyScore + session.flowScore + session.goalAchievementScore) / 60.0

        val (empDelta, listDelta, confDelta, collDelta) = if (allPastSessions.isNotEmpty()) {
            val prevEmpAvg = allPastSessions.map { it.empathyScore / 20.0 }.average()
            val prevListAvg = allPastSessions.map { it.flowScore / 20.0 }.average()
            val prevConfAvg = allPastSessions.map { it.goalAchievementScore / 20.0 }.average()
            val prevCollAvg = allPastSessions.map { (it.empathyScore + it.flowScore + it.goalAchievementScore) / 60.0 }.average()

            val newEmpAvg = (allPastSessions.map { it.empathyScore / 20.0 } + currentEmpathy).average()
            val newListAvg = (allPastSessions.map { it.flowScore / 20.0 } + currentListening).average()
            val newConfAvg = (allPastSessions.map { it.goalAchievementScore / 20.0 } + currentConfidence).average()
            val newCollAvg = (allPastSessions.map { (it.empathyScore + it.flowScore + it.goalAchievementScore) / 60.0 } + currentCollaboration).average()

            listOf(
                newEmpAvg - prevEmpAvg,
                newListAvg - prevListAvg,
                newConfAvg - prevConfAvg,
                newCollAvg - prevCollAvg
            )
        } else {
            listOf(0.0, 0.0, 0.0, 0.0)
        }

        // Motivation message under 12 words
        val celebName = session.partnerName
        val motivationalMessage = when {
            session.score >= 90 -> "Slick dialogue! $celebName was deeply analytical and supportive of your pacing."
            session.score >= 75 -> "Dynamic practice! You built outstanding rapport with $celebName so smoothly!"
            else -> "Superb effort! You navigated the social friction with complete composure."
        }

        // Recommend weakest category
        val categoryScores = (allPastSessions + session).groupBy { it.category }
            .mapValues { entry -> entry.value.map { it.score }.average() }
        
        val recommendTitle: String
        val recommendReason: String

        val weaklyPerformedCategory = categoryScores.minByOrNull { it.value }?.key
        if (weaklyPerformedCategory != null) {
            val scenariosInWeak = com.example.data.model.ScenarioCatalog.scenarios
                .filter { it.category == weaklyPerformedCategory && it.id != session.scenarioId }
            if (scenariosInWeak.isNotEmpty()) {
                val picked = scenariosInWeak.random()
                recommendTitle = picked.title
                recommendReason = "Strengthen your confidence and rapport skills in $weaklyPerformedCategory situations next."
            } else {
                recommendTitle = "The Salary Negotiation"
                recommendReason = "Boost your professional networking and high-stakes speaking assertiveness."
            }
        } else {
            recommendTitle = "The Salary Negotiation"
            recommendReason = "Boost your professional networking and high-stakes speaking assertiveness."
        }

        val result = ProgressionResult(
            xp_earned = XpEarned(baseForDifficulty, bonuses, totalSessionXp),
            xp_before = xpBefore,
            xp_after = xpAfter,
            level_before = levelBefore,
            level_after = levelAfter,
            level_title_before = levelTitleBefore,
            level_title_after = levelTitleAfter,
            leveled_up = leveledUp,
            streak = ProgressionStreak(streakBefore, newStreak, streakBroken, streakFreezeUsed),
            badges_unlocked = newBadges,
            skill_deltas = SkillDeltas(empDelta, listDelta, confDelta, collDelta),
            next_scenario_recommendation = ScenarioRecommendation(recommendTitle, recommendReason),
            motivational_message = motivationalMessage
        )

        // Save progress back to SharedPreferences
        scenariosTodayMutable.add(session.scenarioId)
        val combinedBadges = unlockedBadgesSet + freshlyUnlockedBadgeIds
        val combinedCategories = completedCategories + session.category

        sharedPrefs.edit()
            .putInt("user_total_xp_$userKey", xpAfter)
            .putInt("user_streak_$userKey", newStreak)
            .putInt("user_longest_streak_$userKey", newLongestStreak)
            .putInt("streak_freezes_left_$userKey", freezesLeft)
            .putInt("total_practice_days_$userKey", newTotalPracticeDays)
            .putStringSet("completed_dates_set_$userKey", completedDatesSet)
            .putString("streak_saved_message_$userKey", streakSavedMsg)
            .putString("last_session_date_$userKey", todayStr)
            .putString("completed_today_date_$userKey", todayStr)
            .putStringSet("completed_scenarios_today_$userKey", scenariosTodayMutable)
            .putStringSet("unlocked_badges_$userKey", combinedBadges)
            .putStringSet("completed_categories_$userKey", combinedCategories)
            .apply()

        _totalXp.value = xpAfter
        _streak.value = newStreak
        _longestStreak.value = newLongestStreak
        _streakFreezesLeft.value = freezesLeft
        _totalPracticeDays.value = newTotalPracticeDays
        _completedDates.value = completedDatesSet
        _streakSavedMessage.value = streakSavedMsg
        _unlockedBadgeIds.value = combinedBadges
        _latestProgression.value = result
        _scenariosCompletedToday.value = scenariosTodayMutable

        if (newBadges.isNotEmpty() && _soundsEnabled.value) {
            com.example.ui.screens.SoundEffects.playBadgeUnlock(getApplication())
        }

        saveSessionToLocalStorage(session, result)
    }

    fun setVoiceModeEnabled(enabled: Boolean) {
        _voiceModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("voice_mode_enabled", enabled).apply()
        if (!enabled) {
            stopRecordingAudio(discard = true)
            stopSpeaking()
        }
    }

    fun setVoiceAiSpeaks(speaks: Boolean) {
        _voiceAiSpeaks.value = speaks
        sharedPrefs.edit().putBoolean("voice_ai_speaks", speaks).apply()
        if (!speaks) {
            stopSpeaking()
        }
    }

    fun setVoiceSpeed(speed: Float) {
        _voiceSpeed.value = speed
        sharedPrefs.edit().putFloat("voice_speed", speed).apply()
    }

    fun clearVoiceFeedbackMessage() {
        _voiceFeedbackMessage.value = null
    }

    private var mediaRecorder: android.media.MediaRecorder? = null
    private var audioFile: java.io.File? = null
    private var recordingStartTime = 0L

    fun startRecording() {
        val context = getApplication<Application>().applicationContext
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _voiceFeedbackMessage.value = "Microphone permission denied. Check your browser settings."
            _voiceRecordingState.value = VoiceRecordingState.IDLE
            return
        }

        val cacheDir = context.cacheDir
        audioFile = java.io.File(cacheDir, "temp_speech.m4a")
        if (audioFile?.exists() == true) {
            audioFile?.delete()
        }

        recordingStartTime = System.currentTimeMillis()
        try {
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            recorder.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _voiceRecordingState.value = VoiceRecordingState.RECORDING
            _voiceFeedbackMessage.value = null
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "Failed to start MediaRecorder", e)
            _voiceFeedbackMessage.value = "Microphone access needed for voice mode"
            _voiceRecordingState.value = VoiceRecordingState.IDLE
        }
    }

    fun stopRecordingAudio(discard: Boolean = false) {
        val recorder = mediaRecorder
        if (recorder == null) {
            if (_voiceRecordingState.value == VoiceRecordingState.RECORDING) {
                _voiceRecordingState.value = VoiceRecordingState.IDLE
            }
            return
        }

        mediaRecorder = null
        try {
            recorder.stop()
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "MediaRecorder stop failed", e)
        } finally {
            recorder.release()
        }

        if (discard) {
            _voiceRecordingState.value = VoiceRecordingState.IDLE
            audioFile?.delete()
            return
        }

        val duration = System.currentTimeMillis() - recordingStartTime
        if (duration < 500) {
            _voiceRecordingState.value = VoiceRecordingState.IDLE
            _voiceFeedbackMessage.value = "Hold longer to record"
            audioFile?.delete()
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                if (_voiceFeedbackMessage.value == "Hold longer to record") {
                    _voiceFeedbackMessage.value = null
                }
            }
            return
        }

        _voiceRecordingState.value = VoiceRecordingState.PROCESSING
        _voiceFeedbackMessage.value = null

        viewModelScope.launch {
            val file = audioFile
            if (file == null || !file.exists()) {
                _voiceRecordingState.value = VoiceRecordingState.IDLE
                _voiceFeedbackMessage.value = "Voice error. Try again or switch to text."
                return@launch
            }

            try {
                val transcribedText = transcribeAudioFile(file)
                if (transcribedText.isNullOrBlank()) {
                    _voiceRecordingState.value = VoiceRecordingState.IDLE
                    _voiceFeedbackMessage.value = "Didn't catch that. Try again."
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000)
                        if (_voiceFeedbackMessage.value == "Didn't catch that. Try again.") {
                            _voiceFeedbackMessage.value = null
                        }
                    }
                } else {
                    sendMessage(transcribedText, isVoiceMsg = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversableVM", "Whisper transcription error", e)
                _voiceRecordingState.value = VoiceRecordingState.IDLE
                _voiceFeedbackMessage.value = "Voice error. Try again or switch to text."
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (_voiceFeedbackMessage.value == "Voice error. Try again or switch to text.") {
                        _voiceFeedbackMessage.value = null
                    }
                }
            }
        }
    }

    private suspend fun transcribeAudioFile(file: java.io.File): String? = withContext(Dispatchers.IO) {
        val apiKey = getGroqApiKey()
        if (apiKey.isEmpty()) return@withContext null

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val mediaTypeM4a = "audio/mp4".toMediaType()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.m4a", file.asRequestBody(mediaTypeM4a))
            .addFormDataPart("model", "whisper-large-v3-turbo")
            .addFormDataPart("language", "en")
            .addFormDataPart("response_format", "json")
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@withContext null
                    parseWhisperResponse(bodyString)
                } else {
                    val errBody = response.body?.string() ?: ""
                    android.util.Log.e("ConversableVM", "Whisper error: code=${response.code} body=$errBody")
                    throw Exception("Whisper API error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "Whisper exception", e)
            throw e
        }
    }

    private fun parseWhisperResponse(responseJson: String): String? {
        return try {
            val jsonMap = moshi.adapter(Map::class.java).fromJson(responseJson) as? Map<*, *>
            jsonMap?.get("text") as? String
        } catch (e: Exception) {
            android.util.Log.e("ConversableVM", "Failed to parse Whisper response", e)
            null
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _currentlySpeakingMessageId.value = null
        if (_voiceRecordingState.value == VoiceRecordingState.SPEAKING) {
            _voiceRecordingState.value = VoiceRecordingState.IDLE
        }
    }

    fun speakText(text: String, messageId: String) {
        if (textToSpeech == null) return

        textToSpeech?.stop()

        val name = activeScenario?.partnerName ?: ""
        val gender = when (name.lowercase(Locale.ROOT)) {
            "clara", "elena", "zoe" -> "female"
            "leo", "arthur", "marcus" -> "male"
            else -> "nonbinary"
        }

        val langLocale = when (_selectedLanguage.value.lowercase(Locale.ROOT)) {
            "spanish" -> Locale("es", "ES")
            "french" -> Locale("fr", "FR")
            "german" -> Locale("de", "DE")
            "italian" -> Locale("it", "IT")
            "portuguese" -> Locale("pt", "PT")
            "russian" -> Locale("ru", "RU")
            "japanese" -> Locale("ja", "JP")
            "korean" -> Locale("ko", "KR")
            "chinese (mandarin)", "chinese" -> Locale("zh", "CN")
            "hindi" -> Locale("hi", "IN")
            "hinglish" -> Locale("hi", "IN")
            "punjabi" -> Locale("pa", "IN")
            "tamil" -> Locale("ta", "IN")
            "telugu" -> Locale("te", "IN")
            "marathi" -> Locale("mr", "IN")
            "bengali" -> Locale("bn", "IN")
            "gujarati" -> Locale("gu", "IN")
            "kannada" -> Locale("kn", "IN")
            "malayalam" -> Locale("ml", "IN")
            else -> Locale.US
        }

        try {
            textToSpeech?.language = langLocale
        } catch (e: Exception) {
            textToSpeech?.language = Locale.US
        }

        val voices = textToSpeech?.voices
        if (voices != null) {
            val voiceToUse = when (gender) {
                "male" -> {
                    voices.find { v ->
                        val vname = v.name
                        val matchesLocale = v.locale?.language == langLocale.language
                        matchesLocale && (vname.contains("Male", ignoreCase = true) ||
                        vname.contains("David", ignoreCase = true) ||
                        vname.contains("James", ignoreCase = true) ||
                        vname.contains("Daniel", ignoreCase = true))
                    } ?: voices.find { v -> v.locale?.language == langLocale.language }
                }
                "female" -> {
                    voices.find { v ->
                        val vname = v.name
                        val matchesLocale = v.locale?.language == langLocale.language
                        matchesLocale && (vname.contains("Female", ignoreCase = true) ||
                        vname.contains("Samantha", ignoreCase = true) ||
                        vname.contains("Victoria", ignoreCase = true) ||
                        vname.contains("Karen", ignoreCase = true) ||
                        vname.contains("Moira", ignoreCase = true))
                    } ?: voices.find { v -> v.locale?.language == langLocale.language }
                }
                else -> voices.find { v -> v.locale?.language == langLocale.language }
            }
            if (voiceToUse != null) {
                textToSpeech?.voice = voiceToUse
            }
        }

        textToSpeech?.setPitch(
            when (gender) {
                "male" -> 0.9f
                "female" -> 1.1f
                else -> 1.0f
            }
        )

        textToSpeech?.setSpeechRate(_voiceSpeed.value)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
        }

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _currentlySpeakingMessageId.value = utteranceId
                _voiceRecordingState.value = VoiceRecordingState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                if (_currentlySpeakingMessageId.value == utteranceId) {
                    _currentlySpeakingMessageId.value = null
                    _voiceRecordingState.value = VoiceRecordingState.IDLE
                }
            }

            @Deprecated("Deprecated")
            override fun onError(utteranceId: String?) {
                if (_currentlySpeakingMessageId.value == utteranceId) {
                    _currentlySpeakingMessageId.value = null
                    _voiceRecordingState.value = VoiceRecordingState.IDLE
                }
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, messageId)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    private fun loadSavedSessionsFromLocalStorage(): List<com.example.data.model.SavedSession> {
        val rawEncrypted = sharedPrefs.getString("sessions_encrypted", null)
        val json = if (rawEncrypted != null) {
            com.example.security.CryptoHelper.decrypt(rawEncrypted) ?: "[]"
        } else {
            // Backward compatibility / migration
            val plain = sharedPrefs.getString("sessions", null)
            if (plain != null) {
                // Securely migrate existing unencrypted sessions
                val enc = com.example.security.CryptoHelper.encrypt(plain)
                sharedPrefs.edit().putString("sessions_encrypted", enc).remove("sessions").apply()
                plain
            } else {
                "[]"
            }
        }
        return try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.SavedSession::class.java)
            moshi.adapter<List<com.example.data.model.SavedSession>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun persistSavedSessions(list: List<com.example.data.model.SavedSession>) {
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.SavedSession::class.java)
        val json = moshi.adapter<List<com.example.data.model.SavedSession>>(listType).toJson(list)
        val encryptedJson = com.example.security.CryptoHelper.encrypt(json)
        sharedPrefs.edit().putString("sessions_encrypted", encryptedJson).remove("sessions").apply()
        _savedSessions.value = list
    }

    fun clearAllLocalStorageHistory() {
        persistSavedSessions(emptyList())
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            val userKey = getActiveUserKey()
            _savedSessions.value.forEach { session ->
                val dbId = session.id.toIntOrNull()
                if (dbId != null) {
                    repository.deleteSessionById(dbId)
                }
            }

            sharedPrefs.edit()
                .putInt("user_total_xp_$userKey", 0)
                .putInt("user_streak_$userKey", 0)
                .putInt("user_longest_streak_$userKey", 0)
                .putInt("streak_freezes_left_$userKey", 2)
                .putInt("total_practice_days_$userKey", 0)
                .putStringSet("completed_dates_set_$userKey", emptySet())
                .putString("streak_saved_message_$userKey", null)
                .putString("last_session_date_$userKey", "")
                .putString("completed_today_date_$userKey", "")
                .putStringSet("completed_scenarios_today_$userKey", emptySet())
                .putStringSet("unlocked_badges_$userKey", emptySet())
                .putStringSet("completed_categories_$userKey", emptySet())
                .putString("sessions_$userKey", "[]")
                .apply()

            _totalXp.value = 0
            _streak.value = 0
            _longestStreak.value = 0
            _streakFreezesLeft.value = 2
            _totalPracticeDays.value = 0
            _completedDates.value = emptySet()
            _streakSavedMessage.value = null
            _unlockedBadgeIds.value = emptySet()
            _savedSessions.value = emptyList()
            _scenariosCompletedToday.value = emptySet()
            _goodStreak.value = 0
        }
    }

    fun deleteLocalStorageSessionById(id: String) {
        val currentList = _savedSessions.value
        val filtered = currentList.filter { it.id != id }
        persistSavedSessions(filtered)
        
        // Keep DB in sync
        val intId = id.toIntOrNull()
        if (intId != null) {
            viewModelScope.launch {
                repository.deleteSessionById(intId)
            }
        }
    }

    private fun getPartnerGender(name: String, persona: String): String {
        val mapped = when (name.lowercase(Locale.ROOT)) {
            "clara", "elena", "zoe" -> "female"
            "leo", "arthur", "marcus" -> "male"
            else -> {
                val p = persona.lowercase(Locale.ROOT)
                if (p.contains("she") || p.contains("her")) "female"
                else if (p.contains("he") || p.contains("him") || p.contains("his")) "male"
                else "nonbinary"
            }
        }
        return mapped
    }

    private fun saveSessionToLocalStorage(session: com.example.data.db.ConversationSession, result: com.example.viewmodel.ProgressionResult?) {
        val scenario = activeScenario ?: return
        val chatList = _messages.value
        val turnCount = chatList.count { it.isUser }
        val durationSec = if (sessionStartTime > 0L) {
            ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt().coerceAtLeast(0)
        } else {
            125
        }

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val isoDate = df.format(java.util.Date(session.timestamp))

        val gender = getPartnerGender(scenario.partnerName, scenario.partnerPersona)

        // Parse strengths, improvements, tips
        val strengths = extractLines(session.whatWentWell)
        val improvements = extractLines(session.missedOpportunities)
        val tips = extractLines(session.replayTips)

        // Parse skills (0-5 scale)
        val empathyVal = (session.empathyScore / 20.0).coerceIn(0.0, 5.0)
        val listeningVal = (session.flowScore / 20.0).coerceIn(0.0, 5.0)
        val confidenceVal = (session.goalAchievementScore / 20.0).coerceIn(0.0, 5.0)
        val collaborationVal = ((session.empathyScore + session.flowScore + session.goalAchievementScore) / 60.0).coerceIn(0.0, 5.0)

        // Rapport trend
        val rapportTrend = when {
            _latestCoachTip.value?.engagement_trend?.uppercase() == "RISING" -> "RISING"
            _latestCoachTip.value?.engagement_trend?.uppercase() == "FALLING" -> "FALLING"
            _latestCoachTip.value?.engagement_trend?.uppercase() == "STEADY" -> "STEADY"
            else -> {
                val finalRep = _rapportLevel.value
                if (finalRep > 55) "RISING"
                else if (finalRep < 45) "FALLING"
                else "STEADY"
            }
        }

        // Messages mapping
        val messagesList = chatList.map { msg ->
            val timestampSec = if (msg.timestamp >= sessionStartTime) {
                ((msg.timestamp - sessionStartTime) / 1000).toString()
            } else {
                "0"
            }
            com.example.data.model.SavedSessionMessage(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.text,
                timestamp = timestampSec,
                rapport_at_this_point = messageRapportMap[msg.id] ?: 50,
                was_voice = msg.isVoice
            )
        }

        val badgesEarned = result?.badges_unlocked?.map { it.id } ?: emptyList()
        val xpEarnedVal = result?.xp_earned?.total_this_session ?: 0
        val voiceModeUsed = chatList.any { it.isVoice }

        val savedSession = com.example.data.model.SavedSession(
            id = session.id.toString(),
            date = isoDate,
            scenario_title = scenario.title,
            scenario_category = scenario.category,
            difficulty = scenario.difficulty.uppercase(Locale.US),
            partner_name = scenario.partnerName,
            partner_gender = gender,
            duration_seconds = durationSec,
            turn_count = turnCount,
            social_score = session.score,
            rapport_final = session.score,
            rapport_trend = rapportTrend,
            skills = com.example.data.model.SavedSessionSkills(
                empathy = empathyVal,
                listening = listeningVal,
                confidence = confidenceVal,
                collaboration = collaborationVal
            ),
            strengths = strengths,
            improvements = improvements,
            tips = tips,
            messages = messagesList,
            badges_earned = badgesEarned,
            xp_earned = xpEarnedVal,
            voice_mode_used = voiceModeUsed
        )

        val existing = loadSavedSessionsFromLocalStorage()
        val updated = (listOf(savedSession) + existing).take(50)
        persistSavedSessions(updated)

        // Capture Daily Challenge completion
        if (_isDailyChallengeActive.value) {
            val challenge = _currentDailyChallenge.value
            if (challenge != null) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                val newCompleted = com.example.data.model.CompletedDailyChallenge(
                    id = challenge.id,
                    date = todayStr,
                    score = session.score,
                    xpEarned = xpEarnedVal
                )
                val currentList = loadCompletedDailyChallenges().toMutableList()
                if (!currentList.any { it.id == challenge.id && it.date == todayStr }) {
                    currentList.add(newCompleted)
                    saveCompletedDailyChallenges(currentList)
                }
                
                // Clear active daily challenge state
                _isDailyChallengeActive.value = false
            }
        }

        // Trigger background memory extraction for this persona
        val transcriptString = chatList.joinToString("\n") { "${if (it.isUser) "User" else "Partner"}: ${it.text}" }
        extractAndSaveMemories(scenario.partnerName, transcriptString)
    }

    private fun extractLines(text: String): List<String> {
        return text.split("\n")
            .map { it.trim().replace(Regex("^[-*•0-9.\\s]+"), "").trim() }
            .filter { it.isNotEmpty() }
    }

    // --- FEATURE 4: Real Chat Analyzer View Model Methods ---
    fun deleteAnalyzedChat(id: Int) {
        viewModelScope.launch {
            analyzedChatDao.deleteAnalyzedChat(id)
        }
    }

    suspend fun analyzeChat(rawText: String, source: String, anonymize: Boolean, title: String) {
        val systemPrompt = """
            You are an expert communication coach and relationship analyst.
            Analyze the following conversation transcript.
            
            Return ONLY a valid JSON object matching this exact schema:
            {
              "relationshipScore": 72,
              "relationshipSummary": "Summary of the relationship dynamics...",
              "communicationStyle": "Balanced / Anxious / Avoidant",
              "actionableAdvice": "Specific actionable coaching recommendations...",
              "redFlags": [
                {
                  "flag": "Passive Aggression",
                  "quote": "Exact quote from transcript",
                  "explanation": "Why this is a red flag"
                }
              ],
              "greenFlags": [
                {
                  "flag": "Active Empathy",
                  "quote": "Exact quote",
                  "explanation": "Why this is a green flag"
                }
              ],
              "messages": [
                {
                  "sender": "Me",
                  "text": "original message text"
                }
              ]
            }
            
            All keys must match this exact schema. If there are no flags, return an empty list. 
            Do not include any markdown format blocks or introductory text. Just raw JSON.
        """.trimIndent()

        val response = callGroq(
            systemPrompt = systemPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = rawText, isUser = true)),
            temperature = 0.5f,
            maxTokens = 1200,
            forceJson = true
        )

        if (!response.isNullOrBlank()) {
            val entity = com.example.data.db.AnalyzedChatEntity(
                source = source,
                chatTitle = title,
                rawText = rawText,
                analysisResultJson = response
            )
            analyzedChatDao.insertAnalyzedChat(entity)
        }
    }

    // --- FEATURE 5: Live Conversation Coach View Model Methods ---
    suspend fun generateLiveStrategy(goal: String, scenario: String): String? {
        val systemPrompt = "You are a strategic communication coach. Generate a high-level, bulleted checklist strategy (max 3 points, max 50 words) to help the user achieve their goal in this scenario. No intros, no outlines. Just 3 bullet points."
        val userPrompt = "Goal: $goal\nScenario: $scenario"
        return callGroq(systemPrompt, listOf(ChatMessage(id="1", text=userPrompt, isUser=true)), 0.7f, 100)
    }

    suspend fun generateLiveCoachingTip(goal: String, scenario: String, history: List<ChatMessage>, alternate: Boolean = false): String? {
        val transcript = history.joinToString("\n") { "${if (it.isUser) "Me" else "Partner"}: ${it.text}" }
        val prompt = if (alternate) {
            "Give an alternate, different micro-tip than the previous one."
        } else {
            "Give a single, immediate, action-oriented micro-tip (max 15 words) for the user's next response based on this goal, scenario, and history."
        }
        val systemPrompt = "You are a live conversational coach. Read the transcript and goal. $prompt"
        val userPrompt = "Goal: $goal\nScenario: $scenario\n\nTranscript:\n$transcript"
        return callGroq(systemPrompt, listOf(ChatMessage(id="1", text=userPrompt, isUser=true)), 0.8f, 50)
    }

    suspend fun generateLivePostFeedback(goal: String, scenario: String, strategy: String, history: List<ChatMessage>): String? {
        val transcript = history.joinToString("\n") { "${if (it.isUser) "Me" else "Partner"}: ${it.text}" }
        val systemPrompt = """
            You are a post-game communication evaluator. Read the transcript of the live conversation, the strategy, and the goal.
            Generate a detailed evaluation with:
            1. GOAL ACHIEVEMENT SCORE: 0 to 100.
            2. WHAT WENT WELL: 2 bullet points.
            3. AREAS FOR IMPROVEMENT: 2 bullet points.
            4. ACTIONABLE TIP: 1 key tip for next time.
            
            Keep the tone professional, direct, and constructive. Keep total response under 200 words.
        """.trimIndent()
        val userPrompt = "Goal: $goal\nScenario: $scenario\nStrategy: $strategy\n\nTranscript:\n$transcript"
        return callGroq(systemPrompt, listOf(ChatMessage(id="1", text=userPrompt, isUser=true)), 0.7f, 400)
    }

    fun saveLiveCoachingSession(goal: String, scenario: String, strategy: String, transcriptJson: String, suggestionsShown: Int, feedback: String) {
        viewModelScope.launch {
            val session = com.example.data.db.LiveCoachingSessionEntity(
                goal = goal,
                scenario = scenario,
                strategy = strategy,
                conversationTranscript = transcriptJson,
                suggestionsShown = suggestionsShown,
                feedback = feedback
            )
            liveCoachingSessionDao.insertSession(session)
        }
    }

    // --- FEATURE 6: AI Memory View Model Methods ---
    fun setMemoryEnabled(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun getMemoriesForPersona(personaId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.db.PersonaMemoryEntity>> {
        return personaMemoryDao.getMemoriesForPersona(personaId)
    }

    fun addMemory(personaId: String, text: String) {
        viewModelScope.launch {
            personaMemoryDao.insertMemory(com.example.data.db.PersonaMemoryEntity(personaId = personaId, memoryText = text))
        }
    }

    fun updateMemory(id: Int, text: String) {
        viewModelScope.launch {
            personaMemoryDao.updateMemory(id, text)
        }
    }

    fun deleteMemory(id: Int) {
        viewModelScope.launch {
            personaMemoryDao.deleteMemory(id)
        }
    }

    fun clearMemoriesForPersona(personaId: String) {
        viewModelScope.launch {
            personaMemoryDao.clearMemoriesForPersona(personaId)
        }
    }

    fun extractAndSaveMemories(personaId: String, transcript: String) {
        if (!_isMemoryEnabled.value) return
        viewModelScope.launch {
            val systemPrompt = "You are an AI fact extractor. Read the transcript and extract any key user facts, preferences, goals, or important details about their life that the character should remember. Format your response strictly as a JSON array of strings, like [\"Fact 1\", \"Fact 2\"]. If nothing is worth remembering, return empty array []."
            val response = callGroq(systemPrompt, listOf(ChatMessage(id="1", text=transcript, isUser=true)), 0.3f, 200)
            if (!response.isNullOrBlank()) {
                try {
                    val adapter = moshi.adapter(List::class.java)
                    val list = adapter.fromJson(response) as? List<*>
                    list?.forEach { item ->
                        val memory = item as? String
                        if (!memory.isNullOrBlank()) {
                            personaMemoryDao.insertMemory(com.example.data.db.PersonaMemoryEntity(personaId = personaId, memoryText = memory))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConversableVM", "Failed to parse extracted memories: $response", e)
                }
            }
        }
    }

    // --- FEATURE 7: Scenario Marketplace View Model Methods ---
    fun bookmarkMarketplaceScenario(id: String, bookmarked: Boolean) {
        viewModelScope.launch {
            marketplaceScenarioDao.updateBookmark(id, bookmarked)
        }
    }

    fun followCreator(creatorName: String) {
        val updated = _followedCreators.value.toMutableSet()
        if (updated.contains(creatorName)) {
            updated.remove(creatorName)
        } else {
            updated.add(creatorName)
        }
        _followedCreators.value = updated
    }

    suspend fun generateScenarioWithAi(prompt: String, callback: (com.example.data.db.MarketplaceScenarioEntity?) -> Unit) {
        val systemPrompt = """
            You are an AI Scenario Writer. Based on the user's idea, write a realistic, engaging, and high-quality social skills practice scenario.
            
            Return ONLY a valid JSON object matching this exact schema:
            {
              "title": "Scenario Title",
              "description": "Short scenario summary...",
              "category": "Work / Personal / Negotiation / Conflict",
              "difficulty": "Easy / Medium / Hard",
              "partnerName": "Partner Name",
              "partnerAvatar": "avatar_default",
              "systemPrompt": "Detailed character system prompt for the roleplay...",
              "initialMessage": "The very first line the partner says to begin the roleplay."
            }
            
            Ensure the fields are fully filled. Do not wrap in markdown or include any introductory statements. Just raw JSON.
        """.trimIndent()

        val response = callGroq(
            systemPrompt = systemPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = prompt, isUser = true)),
            temperature = 0.8f,
            maxTokens = 800,
            forceJson = true
        )

        val created = if (!response.isNullOrBlank()) {
            try {
                val adapter = moshi.adapter(Map::class.java)
                val map = adapter.fromJson(response)
                if (map != null) {
                    val id = "marketplace_" + UUID.randomUUID().toString().substring(0, 8)
                    val entity = com.example.data.db.MarketplaceScenarioEntity(
                        id = id,
                        title = map["title"] as? String ?: "AI Generated Scenario",
                        description = map["description"] as? String ?: "A custom scenario generated by AI.",
                        category = map["category"] as? String ?: "Personal",
                        difficulty = map["difficulty"] as? String ?: "Medium",
                        partnerName = map["partnerName"] as? String ?: "Taylor",
                        partnerAvatar = map["partnerAvatar"] as? String ?: "avatar_default",
                        systemPrompt = map["systemPrompt"] as? String ?: "Roleplay partner.",
                        initialMessage = map["initialMessage"] as? String ?: "Hi there.",
                        isBookmarked = false,
                        isPublished = true,
                        creator = "AI Creator",
                        rating = 4.8f,
                        ratingCount = 1
                    )
                    marketplaceScenarioDao.insertScenario(entity)
                    entity
                } else null
            } catch (e: Exception) {
                android.util.Log.e("ConversableVM", "Failed to parse generated scenario: $response", e)
                null
            }
        } else null
        
        callback(created)
    }

    fun rateScenario(id: String, rating: Float) {
        viewModelScope.launch {
            val s = marketplaceScenarios.value.find { it.id == id }
            if (s != null) {
                val newCount = s.ratingCount + 1
                val newRating = ((s.rating * s.ratingCount) + rating) / newCount
                val updated = s.copy(rating = newRating, ratingCount = newCount)
                marketplaceScenarioDao.insertScenario(updated)
            }
        }
    }

    fun publishScenario(scenario: com.example.data.db.MarketplaceScenarioEntity) {
        viewModelScope.launch {
            val updated = scenario.copy(isPublished = true)
            marketplaceScenarioDao.insertScenario(updated)
        }
    }

    suspend fun queryAiPublic(systemPrompt: String, question: String): String? {
        return callGroq(
            systemPrompt = systemPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = question, isUser = true)),
            temperature = 0.7f,
            maxTokens = 250
        )
    }

    suspend fun analyzePublicSpeaking(speechText: String, slideOutline: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an expert public speaking coach and presentation evaluator.
            Analyze the following speech draft and presentation slides outline.
            
            Return ONLY a valid JSON object matching this exact schema:
            {
              "pace": "e.g. Optimal (130 WPM)",
              "clarity": 85.0,
              "fillers": "e.g. 3 filler words detected (um, like)",
              "confidence": 80.0,
              "evaluation": "Detailed evaluation text about structural strength and delivery recommendations...",
              "slideTips": [
                "Slide tip 1...",
                "Slide tip 2..."
              ]
            }
            
            Ensure the fields are fully filled. Do not wrap in markdown or include any introductory statements. Just raw JSON.
        """.trimIndent()

        val text = "Speech: $speechText\nSlides Outline: $slideOutline"
        val response = callGroq(
            systemPrompt = systemPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = text, isUser = true)),
            temperature = 0.6f,
            maxTokens = 800,
            forceJson = true
        )

        if (!response.isNullOrBlank()) {
            try {
                val adapter = moshi.adapter(Map::class.java)
                adapter.fromJson(response) as? Map<String, Any>
            } catch (e: Exception) {
                android.util.Log.e("ConversableVM", "Failed to parse public speaking analysis: ${response}", e)
                null
            }
        } else null
    }

    fun addContactReminder(name: String, relationship: String, intervalDays: Int, warmthScore: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactReminderDao.insertContactReminder(
                com.example.data.db.ContactReminderEntity(
                    name = name,
                    relationship = relationship,
                    lastInteractionTimestamp = System.currentTimeMillis(),
                    reminderIntervalDays = intervalDays,
                    warmthScore = warmthScore,
                    notes = notes
                )
            )
        }
    }

    fun deleteContactReminder(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            contactReminderDao.deleteContactReminder(id)
        }
    }

    fun recordVocalCameraPractice(title: String, fillerWords: Int, wpm: Int, gazeScore: Int, posture: String, clarity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            vocalCameraSessionDao.insertVocalCameraSession(
                com.example.data.db.VocalCameraSessionEntity(
                    title = title,
                    fillerWordCount = fillerWords,
                    wpm = wpm,
                    gazeScore = gazeScore,
                    postureFeedback = posture,
                    speechClarityScore = clarity
                )
            )
        }
    }

    fun saveGroupMeetingSession(title: String, participantsJson: String, transcriptJson: String, score: Int, feedback: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupMeetingSessionDao.insertGroupMeetingSession(
                com.example.data.db.GroupMeetingSessionEntity(
                    title = title,
                    participantsJson = participantsJson,
                    transcriptJson = transcriptJson,
                    score = score,
                    feedback = feedback
                )
            )
        }
    }

    suspend fun queryGroupMeetingAi(systemPrompt: String, userText: String, participantsJson: String): String? = withContext(Dispatchers.IO) {
        val fullPrompt = "$systemPrompt\nThe other members of this group are: $participantsJson.\nRespond from the perspective of one or more participants in a meeting dialog. Keep responses natural, professional, and within 100 words."
        callGroq(
            systemPrompt = fullPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = userText, isUser = true)),
            temperature = 0.7f,
            maxTokens = 400,
            forceJson = false
        )
    }

    suspend fun translateToLocalDialect(text: String, dialectName: String): String? = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are a linguistic coach specializing in local dialects and idioms.
            Translate the user's input text into the following dialect/style: $dialectName.
            Provide both the translated phrase(s) and a brief list of the idioms/slang words used with explanations.
            Format clearly under headings:
            TRANSLATION:
            [phrase]
            
            EXPLANATION OF IDIOMS:
            [bullet list of terms]
            
            Keep the tone warm, professional, and extremely direct.
        """.trimIndent()
        callGroq(
            systemPrompt = systemPrompt,
            chatMessages = listOf(ChatMessage(id = "1", text = text, isUser = true)),
            temperature = 0.5f,
            maxTokens = 500,
            forceJson = false
        )
    }
}
