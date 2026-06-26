package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationSession::class,
        PersonaEntity::class,
        PersonaMemoryEntity::class,
        MarketplaceScenarioEntity::class,
        AnalyzedChatEntity::class,
        LiveCoachingSessionEntity::class,
        ContactReminderEntity::class,
        GroupMeetingSessionEntity::class,
        VocalCameraSessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun personaDao(): PersonaDao
    abstract fun personaMemoryDao(): PersonaMemoryDao
    abstract fun marketplaceScenarioDao(): MarketplaceScenarioDao
    abstract fun analyzedChatDao(): AnalyzedChatDao
    abstract fun liveCoachingSessionDao(): LiveCoachingSessionDao
    abstract fun contactReminderDao(): ContactReminderDao
    abstract fun groupMeetingSessionDao(): GroupMeetingSessionDao
    abstract fun vocalCameraSessionDao(): VocalCameraSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "conversable_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
