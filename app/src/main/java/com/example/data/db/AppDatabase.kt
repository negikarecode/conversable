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
        private var INSTANCES = mutableMapOf<String, AppDatabase>()

        fun getDatabase(context: Context, userKey: String = "default_user"): AppDatabase {
            val dbKey = if (userKey.isEmpty()) "default_user" else userKey
            return INSTANCES[dbKey] ?: synchronized(this) {
                val dbName = if (dbKey == "default_user") {
                    "conversable_database"
                } else {
                    "conversable_database_$dbKey"
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCES[dbKey] = instance
                instance
            }
        }
    }
}
