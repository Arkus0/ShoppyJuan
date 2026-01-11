package com.arkus.shoppyjuan.domain.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Supported languages in the app
 */
enum class AppLanguage(val code: String, val displayName: String) {
    SPANISH("es", "Español"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SPANISH
        }
    }
}

/**
 * App theme options
 */
enum class AppTheme(val value: Int) {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}

/**
 * User preferences data class
 */
data class UserPreferences(
    val language: AppLanguage = AppLanguage.SPANISH,
    val theme: AppTheme = AppTheme.SYSTEM,
    val searchRadiusKm: Int = 10,
    val locationEnabled: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val notificationsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val keepScreenOnInSupermarketMode: Boolean = true
)

/**
 * Manager for user preferences including language, theme, and location settings
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_THEME = intPreferencesKey("theme")
        private val KEY_SEARCH_RADIUS_KM = intPreferencesKey("search_radius_km")
        private val KEY_LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        private val KEY_LAST_LATITUDE = doublePreferencesKey("last_latitude")
        private val KEY_LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        // Default search radius options in km
        val RADIUS_OPTIONS = listOf(1, 2, 5, 10, 15, 20, 30, 50)
        const val DEFAULT_RADIUS_KM = 10
    }

    private val dataStore = context.userPreferencesDataStore

    /**
     * Flow of user preferences
     */
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            language = AppLanguage.fromCode(prefs[KEY_LANGUAGE] ?: AppLanguage.SPANISH.code),
            theme = AppTheme.entries.find { it.value == prefs[KEY_THEME] } ?: AppTheme.SYSTEM,
            searchRadiusKm = prefs[KEY_SEARCH_RADIUS_KM] ?: DEFAULT_RADIUS_KM,
            locationEnabled = prefs[KEY_LOCATION_ENABLED] ?: false,
            lastLatitude = prefs[KEY_LAST_LATITUDE],
            lastLongitude = prefs[KEY_LAST_LONGITUDE],
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
            keepScreenOnInSupermarketMode = prefs[KEY_KEEP_SCREEN_ON] ?: true
        )
    }

    /**
     * Get current language
     */
    val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[KEY_LANGUAGE] ?: AppLanguage.SPANISH.code)
    }

    /**
     * Get current search radius in km
     */
    val searchRadiusKm: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_SEARCH_RADIUS_KM] ?: DEFAULT_RADIUS_KM
    }

    /**
     * Get last known location
     */
    val lastLocation: Flow<Pair<Double, Double>?> = dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAST_LATITUDE]
        val lon = prefs[KEY_LAST_LONGITUDE]
        if (lat != null && lon != null) Pair(lat, lon) else null
    }

    // ==================== LANGUAGE ====================

    /**
     * Set the app language
     */
    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.code
        }
        applyLanguage(language)
    }

    /**
     * Apply language to the app using AppCompat's per-app language feature
     */
    fun applyLanguage(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Initialize language on app start
     */
    suspend fun initializeLanguage() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty) {
            // No per-app locale set, use system default or saved preference
            dataStore.data.collect { prefs ->
                val savedLanguage = AppLanguage.fromCode(prefs[KEY_LANGUAGE] ?: getSystemLanguage().code)
                applyLanguage(savedLanguage)
            }
        }
    }

    /**
     * Get system language
     */
    private fun getSystemLanguage(): AppLanguage {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.SPANISH
        }
    }

    // ==================== THEME ====================

    /**
     * Set the app theme
     */
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.value
        }
        AppCompatDelegate.setDefaultNightMode(theme.value)
    }

    /**
     * Apply saved theme on app start
     */
    suspend fun initializeTheme() {
        dataStore.data.collect { prefs ->
            val themeValue = prefs[KEY_THEME] ?: AppTheme.SYSTEM.value
            AppCompatDelegate.setDefaultNightMode(themeValue)
        }
    }

    // ==================== LOCATION & RADIUS ====================

    /**
     * Set search radius in km
     */
    suspend fun setSearchRadius(radiusKm: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SEARCH_RADIUS_KM] = radiusKm.coerceIn(1, 100)
        }
    }

    /**
     * Set location enabled state
     */
    suspend fun setLocationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_LOCATION_ENABLED] = enabled
        }
    }

    /**
     * Update last known location
     */
    suspend fun updateLocation(latitude: Double, longitude: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_LATITUDE] = latitude
            prefs[KEY_LAST_LONGITUDE] = longitude
            prefs[KEY_LOCATION_ENABLED] = true
        }
    }

    /**
     * Clear saved location
     */
    suspend fun clearLocation() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_LATITUDE)
            prefs.remove(KEY_LAST_LONGITUDE)
            prefs[KEY_LOCATION_ENABLED] = false
        }
    }

    // ==================== OTHER SETTINGS ====================

    /**
     * Set notifications enabled
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Set haptic feedback enabled
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }

    /**
     * Set keep screen on in supermarket mode
     */
    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_KEEP_SCREEN_ON] = enabled
        }
    }
}

/**
 * Calculate distance between two coordinates using Haversine formula
 * Returns distance in kilometers
 */
fun calculateDistanceKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val earthRadiusKm = 6371.0

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

    return earthRadiusKm * c
}

/**
 * Format distance for display
 */
fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1 -> "${(distanceKm * 1000).toInt()} m"
        distanceKm < 10 -> String.format("%.1f km", distanceKm)
        else -> "${distanceKm.toInt()} km"
    }
}
