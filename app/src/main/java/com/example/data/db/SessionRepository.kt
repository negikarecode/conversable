package com.example.data.db

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<ConversationSession>> = sessionDao.getAllSessions()
    val averageScore: Flow<Float?> = sessionDao.getAverageScore()
    val sessionCount: Flow<Int> = sessionDao.getSessionCount()

    suspend fun insertSession(session: ConversationSession) {
        sessionDao.insertSession(session)
    }

    suspend fun deleteSessionById(id: Int) {
        sessionDao.deleteSessionById(id)
    }
}
