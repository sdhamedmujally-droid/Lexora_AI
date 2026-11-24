package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lexisPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lexis_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val arabicUi: Boolean = true,
    val dailyGoalMinutes: Int = 20,
    val defaultAccent: String = "US"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ARABIC_UI = booleanPreferencesKey("arabic_ui")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val DEFAULT_ACCENT = stringPreferencesKey("default_accent")
    }

    val settingsFlow: Flow<UserSettings> = context.lexisPrefsDataStore.data.map { prefs ->
        val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = enumValues<ThemeMode>().firstOrNull { it.name == modeName } ?: ThemeMode.SYSTEM
        UserSettings(
            themeMode = themeMode,
            arabicUi = prefs[Keys.ARABIC_UI] ?: true,
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINUTES] ?: 20,
            defaultAccent = prefs[Keys.DEFAULT_ACCENT] ?: "US"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.lexisPrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setArabicUi(arabic: Boolean) {
        context.lexisPrefsDataStore.edit { it[Keys.ARABIC_UI] = arabic }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.lexisPrefsDataStore.edit { it[Keys.DAILY_GOAL_MINUTES] = minutes.coerceIn(5, 180) }
    }

    suspend fun setDefaultAccent(accent: String) {
        context.lexisPrefsDataStore.edit { it[Keys.DEFAULT_ACCENT] = if (accent == "UK") "UK" else "US" }
    }
}
