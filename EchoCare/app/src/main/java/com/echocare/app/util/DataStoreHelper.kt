package com.echocare.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "echocare_prefs")

@Singleton
class DataStoreHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val USER_NAME               = stringPreferencesKey("user_name")
        val IS_ONBOARDED            = booleanPreferencesKey("is_onboarded")
        val TTS_LANGUAGE            = stringPreferencesKey("tts_language")    // "en" | "hi" | ...
        val TTS_SPEED               = floatPreferencesKey("tts_speed")        // 0.5..2.0
        val BIOMETRIC_ENABLED       = booleanPreferencesKey("biometric_enabled")
        val DEFAULT_REPEAT_INTERVAL = intPreferencesKey("default_repeat_interval_min") // minutes
        val DEFAULT_MAX_REPETITIONS = intPreferencesKey("default_max_repetitions")
        val IS_PREMIUM              = booleanPreferencesKey("is_premium")
        val VOICE_PITCH             = floatPreferencesKey("voice_pitch")
        val ALEXA_LINKED            = booleanPreferencesKey("alexa_linked")
        val GOOGLE_HOME_LINKED      = booleanPreferencesKey("google_home_linked")
    }

    val userName: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_NAME] ?: "" }

    val isOnboarded: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_ONBOARDED] ?: false }

    val ttsLanguage: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[TTS_LANGUAGE] ?: "en" }

    val ttsSpeed: Flow<Float> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[TTS_SPEED] ?: 1.0f }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[BIOMETRIC_ENABLED] ?: false }

    val defaultRepeatInterval: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DEFAULT_REPEAT_INTERVAL] ?: 5 }

    val defaultMaxRepetitions: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DEFAULT_MAX_REPETITIONS] ?: 3 }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_PREMIUM] ?: false }

    val voicePitch: Flow<Float> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[VOICE_PITCH] ?: 1.0f }

    val alexaLinked: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[ALEXA_LINKED] ?: false }

    val googleHomeLinked: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[GOOGLE_HOME_LINKED] ?: false }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDED] = value }
    }

    suspend fun setTtsLanguage(lang: String) {
        context.dataStore.edit { it[TTS_LANGUAGE] = lang }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.dataStore.edit { it[TTS_SPEED] = speed }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDefaultRepeatInterval(minutes: Int) {
        context.dataStore.edit { it[DEFAULT_REPEAT_INTERVAL] = minutes }
    }

    suspend fun setDefaultMaxRepetitions(count: Int) {
        context.dataStore.edit { it[DEFAULT_MAX_REPETITIONS] = count }
    }

    suspend fun setPremium(premium: Boolean) {
        context.dataStore.edit { it[IS_PREMIUM] = premium }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.dataStore.edit { it[VOICE_PITCH] = pitch }
    }

    suspend fun setAlexaLinked(linked: Boolean) {
        context.dataStore.edit { it[ALEXA_LINKED] = linked }
    }

    suspend fun setGoogleHomeLinked(linked: Boolean) {
        context.dataStore.edit { it[GOOGLE_HOME_LINKED] = linked }
    }
}

