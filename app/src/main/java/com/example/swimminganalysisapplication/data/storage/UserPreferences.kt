package com.example.swimminganalysisapplication.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey // Int型用に変更
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_prefs")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = intPreferencesKey("user_id") // USER_ID_KEY を追加 (Int型)
    }

    val authToken: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }

    // --- userId 関連 ---
    val userId: Flow<Int?> // userId を Int? として公開
        get() = context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }

    suspend fun saveUserId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = id
        }
    }

    suspend fun clearUserId() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
        }
    }
}
