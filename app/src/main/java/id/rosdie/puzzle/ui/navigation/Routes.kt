package id.rosdie.puzzle.ui.navigation

import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object HomeRoute

/** Permainan: lanjutkan simpanan, Puzzle Harian, atau puzzle baru dengan konfigurasi. */
@Serializable
data class GameRoute(
    val resume: Boolean = false,
    val daily: Boolean = false,
    val size: Int = 4,
    val mode: String = GameMode.NUMBERS.name,
    val imageId: String? = null,
    val challenge: String = ChallengeType.NORMAL.name,
) {
    fun toConfig(): GameConfig = GameConfig(
        size = size,
        mode = GameMode.entries.firstOrNull { it.name == mode } ?: GameMode.NUMBERS,
        imageId = imageId,
        challenge = ChallengeType.entries.firstOrNull { it.name == challenge } ?: ChallengeType.NORMAL,
        dailyDate = null,
    )

    companion object {
        fun newGame(config: GameConfig) = GameRoute(
            size = config.size,
            mode = config.mode.name,
            imageId = config.imageId,
            challenge = config.challenge.name,
        )
    }
}

@Serializable
data object GalleryRoute

@Serializable
data object StatsRoute

@Serializable
data object HowToRoute

@Serializable
data object SettingsRoute
