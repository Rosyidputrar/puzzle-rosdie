package id.rosdie.puzzle.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import id.rosdie.puzzle.data.datastore.AppSettings
import kotlinx.coroutines.flow.StateFlow

/** Getar halus per gerakan, getar pendek saat tidak valid, pola khusus saat menang. */
class Haptics(context: Context, private val settings: StateFlow<AppSettings>) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun move() = oneShot(12, 0.55f)
    fun aiStep() = oneShot(8, 0.35f)
    fun invalid() = oneShot(28, 1f)

    fun win() = waveform(longArrayOf(0, 40, 60, 40, 60, 120), floatArrayOf(0f, 0.6f, 0f, 0.8f, 0f, 1f))
    fun lose() = waveform(longArrayOf(0, 120, 80, 200), floatArrayOf(0f, 0.7f, 0f, 0.5f))
    fun preview() = oneShot(30, 0.8f, force = true)

    private fun oneShot(durationMs: Long, relative: Float, force: Boolean = false) {
        val v = vibrator ?: return
        val s = settings.value
        if ((!s.hapticsEnabled && !force) || !v.hasVibrator()) return
        val strength = s.hapticStrength.coerceIn(0.05f, 1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amp = if (v.hasAmplitudeControl()) (relative * strength * 255).toInt().coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
            val duration = if (v.hasAmplitudeControl()) durationMs else (durationMs * strength).toLong().coerceAtLeast(5)
            v.vibrate(VibrationEffect.createOneShot(duration, amp))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate((durationMs * strength).toLong().coerceAtLeast(5))
        }
    }

    private fun waveform(timings: LongArray, relative: FloatArray) {
        val v = vibrator ?: return
        val s = settings.value
        if (!s.hapticsEnabled || !v.hasVibrator()) return
        val strength = s.hapticStrength.coerceIn(0.05f, 1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (v.hasAmplitudeControl()) {
                val amps = IntArray(relative.size) { (relative[it] * strength * 255).toInt().coerceIn(0, 255) }
                v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            } else {
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, -1)
        }
    }
}

/** Umpan balik permainan (bunyi + getar). Antarmuka agar ViewModel bisa diuji tanpa perangkat. */
interface GameFeedback {
    fun move()
    fun invalid()
    fun aiStep()
    fun win()
    fun lose()

    object None : GameFeedback {
        override fun move() = Unit
        override fun invalid() = Unit
        override fun aiStep() = Unit
        override fun win() = Unit
        override fun lose() = Unit
    }
}

class DeviceFeedback(
    private val sound: SoundPlayer,
    private val haptics: Haptics,
    private val settings: StateFlow<AppSettings>,
) : GameFeedback {
    override fun move() {
        sound.click(); haptics.move()
    }

    override fun invalid() {
        sound.invalid(); haptics.invalid()
    }

    override fun aiStep() {
        sound.aiStep(); haptics.aiStep()
    }

    override fun win() {
        if (!settings.value.winCelebration) return
        sound.win(); haptics.win()
    }

    override fun lose() {
        sound.lose(); haptics.lose()
    }
}
