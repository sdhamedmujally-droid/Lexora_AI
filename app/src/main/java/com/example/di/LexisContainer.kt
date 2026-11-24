package com.example.di

import android.content.Context
import com.example.R
import com.example.audio.DualAccentAudioEngine
import com.example.data.local.AssetSeeder
import com.example.data.local.LexisDao
import com.example.data.local.LexisDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.AiContentRepository
import com.example.data.remote.UserProgressRepository
import com.google.firebase.firestore.FirebaseFirestore

sealed interface LexisUiState<out T> {
    data object Loading : LexisUiState<Nothing>
    data class Success<T>(val data: T) : LexisUiState<T>
    data class Error(val message: String) : LexisUiState<Nothing>
}

class LexisContainer private constructor(appContext: Context) {

    val database: LexisDatabase = LexisDatabase.getInstance(appContext)
    val dao: LexisDao = database.lexisDao()
    val assetSeeder: AssetSeeder = AssetSeeder(appContext, database)
    val prefsRepository: UserPreferencesRepository = UserPreferencesRepository(appContext)

    private val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    val userProgressRepository: UserProgressRepository = UserProgressRepository(firestore)
    val aiContentRepository: AiContentRepository = AiContentRepository(dao)
    val audioEngine: DualAccentAudioEngine = DualAccentAudioEngine(appContext)

    companion object {
        @Volatile
        private var INSTANCE: LexisContainer? = null

        fun get(context: Context): LexisContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LexisContainer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
