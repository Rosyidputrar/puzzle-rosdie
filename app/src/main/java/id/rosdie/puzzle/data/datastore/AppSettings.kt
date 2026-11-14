package id.rosdie.puzzle.data.datastore

import id.rosdie.puzzle.domain.model.Difficulty

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Tiga varian bunyi klik yang disintesis sendiri. */
enum class ClickSound { SOFT, WOOD, POP }

enum class AnimationSpeed(val stiffnessFactor: Float) {
    SLOW(0.5f),
    NORMAL(1f),
    FAST(2f),
}

data class AppSettings(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val clickSound: ClickSound = ClickSound.SOFT,
    val hapticsEnabled: Boolean = true,
    val hapticStrength: Float = 0.6f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val showReference: Boolean = true,
    val aiSpeedMs: Int = DEFAULT_AI_SPEED_MS,
    /** Fanfare + pola getar menang. */
    val winCelebration: Boolean = true,
) {
    companion object {
        const val MIN_AI_SPEED_MS = 50
        const val MAX_AI_SPEED_MS = 1000
        const val DEFAULT_AI_SPEED_MS = 300
    }
}
