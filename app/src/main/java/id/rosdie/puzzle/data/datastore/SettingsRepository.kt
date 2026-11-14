package id.rosdie.puzzle.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Pengaturan disimpan di DataStore Preferences. Bahasa diatur lewat per-app locale (AppCompat). */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[DIFFICULTY] = next.difficulty.name
            prefs[THEME] = next.themeMode.name
            prefs[DYNAMIC] = next.dynamicColor
            prefs[SOUND] = next.soundEnabled
            prefs[VOLUME] = next.soundVolume.coerceIn(0f, 1f)
            prefs[CLICK] = next.clickSound.name
            prefs[HAPTICS] = next.hapticsEnabled
            prefs[HAPTIC_STRENGTH] = next.hapticStrength.coerceIn(0f, 1f)
            prefs[ANIMATION] = next.animationSpeed.name
            prefs[REFERENCE] = next.showReference
            prefs[AI_SPEED] = next.aiSpeedMs.coerceIn(AppSettings.MIN_AI_SPEED_MS, AppSettings.MAX_AI_SPEED_MS)
            prefs[CELEBRATION] = next.winCelebration
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            difficulty = enumOr(this[DIFFICULTY], d.difficulty),
            themeMode = enumOr(this[THEME], d.themeMode),
            dynamicColor = this[DYNAMIC] ?: d.dynamicColor,
            soundEnabled = this[SOUND] ?: d.soundEnabled,
            soundVolume = this[VOLUME] ?: d.soundVolume,
            clickSound = enumOr(this[CLICK], d.clickSound),
            hapticsEnabled = this[HAPTICS] ?: d.hapticsEnabled,
            hapticStrength = this[HAPTIC_STRENGTH] ?: d.hapticStrength,
            animationSpeed = enumOr(this[ANIMATION], d.animationSpeed),
            showReference = this[REFERENCE] ?: d.showReference,
            aiSpeedMs = this[AI_SPEED] ?: d.aiSpeedMs,
            winCelebration = this[CELEBRATION] ?: d.winCelebration,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val SOUND = booleanPreferencesKey("sound")
        val VOLUME = floatPreferencesKey("volume")
        val CLICK = stringPreferencesKey("click_sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val HAPTIC_STRENGTH = floatPreferencesKey("haptic_strength")
        val ANIMATION = stringPreferencesKey("animation_speed")
        val REFERENCE = booleanPreferencesKey("show_reference")
        val AI_SPEED = intPreferencesKey("ai_speed_ms")
        val CELEBRATION = booleanPreferencesKey("win_celebration")
    }
}

