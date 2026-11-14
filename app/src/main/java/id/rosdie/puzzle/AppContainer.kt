package id.rosdie.puzzle

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.datastore.SettingsRepository
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.local.PuzzleDatabase
import id.rosdie.puzzle.data.repository.AchievementRepository
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.data.repository.SystemTimeSource
import id.rosdie.puzzle.data.repository.TimeSource
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.feedback.DeviceFeedback
import id.rosdie.puzzle.feedback.GameFeedback
import id.rosdie.puzzle.feedback.Haptics
import id.rosdie.puzzle.feedback.SoundPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dependency injection manual. Satu instance per proses (dibuat di [PuzzleApplication]). */
class AppContainer(
    context: Context,
    val db: PuzzleDatabase = PuzzleDatabase.create(context),
    dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("settings")
    },
    val time: TimeSource = SystemTimeSource,
    val io: CoroutineDispatcher = Dispatchers.IO,
    enableDeviceFeedback: Boolean = true,
) {
    /** Scope aplikasi untuk penyimpanan yang tidak boleh terputus saat layar ditutup. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsRepository = SettingsRepository(dataStore)

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .onEach { _settingsLoaded.value = true }
        .stateIn(appScope, SharingStarted.Eagerly, AppSettings())

    val games = GameRepository(db)
    val achievements = AchievementRepository(db.achievements(), time)
    val images = ImageRepository(context, db.customImages(), time, io)
    val solver = PuzzleSolver(Dispatchers.Default)

    val sound: SoundPlayer? = if (enableDeviceFeedback) SoundPlayer(context, settings, appScope, io) else null
    val haptics: Haptics? = if (enableDeviceFeedback) Haptics(context, settings) else null
    val feedback: GameFeedback =
        if (sound != null && haptics != null) DeviceFeedback(sound, haptics, settings) else GameFeedback.None

    suspend fun awaitSettings(): AppSettings {
        settingsLoaded.first { it }
        return settings.value
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        appScope.launch { settingsRepository.update(transform) }
    }

    /** Hapus semua data lokal: riwayat, rekor, pencapaian, simpanan, gambar, pengaturan. */
    suspend fun clearAllData() {
        withContext(io) { db.clearAllTables() }
        images.deleteAllFiles()
        settingsRepository.clear()
    }
}
