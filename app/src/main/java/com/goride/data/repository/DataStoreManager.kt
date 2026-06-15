package com.goride.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.goride.data.models.LocationModel
import com.goride.data.models.UserProfile
import com.goride.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

class DataStoreManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val RECENT_LOCATIONS = stringPreferencesKey("recent_locations")
        val SAVED_LOCATIONS = stringPreferencesKey("saved_locations")
        val USER_PROFILE = stringPreferencesKey("user_profile")
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

    suspend fun saveSavedLocations(locations: List<LocationModel>) {
        val jsonString = gson.toJson(locations)
        context.dataStore.edit { preferences ->
            preferences[SAVED_LOCATIONS] = jsonString
        }
    }

    val savedLocations: Flow<List<LocationModel>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[SAVED_LOCATIONS]
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

    suspend fun saveUserProfile(profile: UserProfile) {
        val jsonString = gson.toJson(profile)
        context.dataStore.edit { preferences ->
            preferences[USER_PROFILE] = jsonString
        }
    }

    val userProfile: Flow<UserProfile?> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[USER_PROFILE]
        if (jsonString.isNullOrEmpty()) {
            null
        } else {
            try {
                gson.fromJson(jsonString, UserProfile::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Saves a profile image from a Uri to internal storage and returns the local file path.
     * This ensures the image persists even if the original Uri becomes inaccessible.
     */
    suspend fun saveProfileImageToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            
            // Cleanup: Delete old profile images to save space
            context.filesDir.listFiles { _, name -> name.startsWith("profile_image_") }?.forEach { it.delete() }
            
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(USER_EMAIL)
            // USER_PROFILE is kept for local persistence as per requirements
        }
    }

    suspend fun clearData() {
        context.dataStore.edit { it.clear() }
    }
}
