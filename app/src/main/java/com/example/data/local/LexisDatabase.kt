package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntryEntity::class,
        VerbEntity::class,
        GrammarLessonEntity::class,
        StoryEntity::class,
        ConversationEntity::class,
        VoicePersonaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LexisDatabase : RoomDatabase() {
    abstract fun lexisDao(): LexisDao

    companion object {
        @Volatile
        private var INSTANCE: LexisDatabase? = null

        fun getInstance(context: Context): LexisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LexisDatabase::class.java,
                    "lexis_oxford_v1.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
