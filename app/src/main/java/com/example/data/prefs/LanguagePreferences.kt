package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shasthoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shastho_sathi_prefs"
)

class AppPreferences(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("app_language")
    private val keyOnboardingSeen = booleanPreferencesKey("onboarding_seen")

    val languageFlow: Flow<AppLanguage> = context.shasthoDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[keyLanguage])
    }

    val onboardingSeenFlow: Flow<Boolean> = context.shasthoDataStore.data.map { prefs ->
        prefs[keyOnboardingSeen] ?: false
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.shasthoDataStore.edit { prefs ->
            prefs[keyLanguage] = language.code
        }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.shasthoDataStore.edit { prefs ->
            prefs[keyOnboardingSeen] = seen
        }
    }
}
