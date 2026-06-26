package com.example.data.db

import kotlinx.coroutines.flow.Flow

class PersonaRepository(private val personaDao: PersonaDao) {
    val allPersonas: Flow<List<PersonaEntity>> = personaDao.getAllPersonas()

    suspend fun insertPersona(persona: PersonaEntity): Long {
        return personaDao.insertPersona(persona)
    }

    suspend fun getPersonaById(id: Int): PersonaEntity? {
        return personaDao.getPersonaById(id)
    }

    suspend fun deletePersonaById(id: Int) {
        personaDao.deletePersonaById(id)
    }
}
