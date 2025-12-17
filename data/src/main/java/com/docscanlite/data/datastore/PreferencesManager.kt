package com.docscanlite.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for app preferences using DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    // Preference Keys
    private object PreferenceKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_EDGE_DETECTION = booleanPreferencesKey("auto_edge_detection")
        val AUTO_OCR = booleanPreferencesKey("auto_ocr")
        val SAVE_ORIGINAL_IMAGE = booleanPreferencesKey("save_original_image")
        val IMAGE_QUALITY = intPreferencesKey("image_quality")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // Theme Mode
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode
        }
    }

    // Auto Edge Detection
    val autoEdgeDetection: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_EDGE_DETECTION] ?: true
    }

    suspend fun setAutoEdgeDetection(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_EDGE_DETECTION] = enabled
        }
    }

    // Auto OCR
    val autoOcr: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_OCR] ?: false
    }

    suspend fun setAutoOcr(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_OCR] = enabled
        }
    }

    // Save Original Image
    val saveOriginalImage: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SAVE_ORIGINAL_IMAGE] ?: true
    }

    suspend fun setSaveOriginalImage(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SAVE_ORIGINAL_IMAGE] = enabled
        }
    }

    // Image Quality (0-100)
    val imageQuality: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IMAGE_QUALITY] ?: 90
    }

    suspend fun setImageQuality(quality: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_QUALITY] = quality.coerceIn(0, 100)
        }
    }

    // Export Format
    val exportFormat: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.EXPORT_FORMAT] ?: "pdf"
    }

    suspend fun setExportFormat(format: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.EXPORT_FORMAT] = format
        }
    }

    // First Launch
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FIRST_LAUNCH] = false
        }
    }

    // Onboarding
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    // Clear all preferences
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
