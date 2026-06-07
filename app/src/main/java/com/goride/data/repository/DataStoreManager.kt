package com.goride.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.goride.data.models.LocationModel
import com.goride.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

class DataStoreManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val RECENT_LOCATIONS = stringPreferencesKey("recent_locations")
    }

    suspend fun saveSession(token: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
            preferences[USER_EMAIL] = email
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    suspend fun saveRecentLocations(locations: List<LocationModel>) {
        val jsonString = gson.toJson(locations)
        context.dataStore.edit { preferences ->
            preferences[RECENT_LOCATIONS] = jsonString
        }
    }

    val recentLocations: Flow<List<LocationModel>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[RECENT_LOCATIONS]
        if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<LocationModel>>() {}.type
            try {
                gson.fromJson(jsonString, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun clearData() {
        context.dataStore.edit { it.clear() }
    }
}