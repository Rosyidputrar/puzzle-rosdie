package id.rosdie.puzzle.feedback

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Semua bunyi aplikasi disintesis lewat kode (bebas hak cipta), lalu ditulis sebagai WAV
 * PCM 16-bit mono 44,1 kHz untuk SoundPool.
 */
object SoundSynth {
    const val SAMPLE_RATE = 44_100

    enum class Sound(val fileName: String) {
        CLICK_SOFT("click_soft.wav"),
        CLICK_WOOD("click_wood.wav"),
        CLICK_POP("click_pop.wav"),
        INVALID("invalid.wav"),
        AI_STEP("ai_step.wav"),
        WIN("win.wav"),
        LOSE("lose.wav"),
    }

    fun render(sound: Sound): ShortArray = when (sound) {
        Sound.CLICK_SOFT -> tone(0.045, amp = 0.55) { t -> sin(2 * PI * 1250 * t) * exp(-t * 95) }
        Sound.CLICK_WOOD -> tone(0.07, amp = 0.7) { t ->
            (0.6 * sin(2 * PI * 820 * t) + 0.4 * sin(2 * PI * 1730 * t)) * exp(-t * 70) + noise() * exp(-t * 900) * 0.4
        }
        Sound.CLICK_POP -> tone(0.06, amp = 0.6) { t ->
            val f = 520 + 900 * t / 0.06
            sin(2 * PI * f * t) * exp(-t * 60)
        }
        Sound.INVALID -> tone(0.11, amp = 0.45) { t ->
            (sin(2 * PI * 170 * t) + 0.35 * sin(2 * PI * 340 * t)) * exp(-t * 28)
        }
        Sound.AI_STEP -> tone(0.03, amp = 0.35) { t -> sin(2 * PI * 1900 * t) * exp(-t * 150) }
        Sound.WIN -> sequence(
            listOf(523.25 to 0.12, 659.25 to 0.12, 783.99 to 0.12, 1046.5 to 0.5),
            chordLast = doubleArrayOf(523.25, 659.25, 783.99, 1046.5),
        )
        Sound.LOSE -> sequence(listOf(392.0 to 0.16, 329.63 to 0.16, 261.63 to 0.42), chordLast = null)
    }

    fun toWav(samples: ShortArray): ByteArray {
        val dataBytes = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(SAMPLE_RATE); putInt(SAMPLE_RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes)
        }
        val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { body.putShort(it) }
        return ByteArrayOutputStream().apply { write(header.array()); write(body.array()) }.toByteArray()
    }

    private var noiseState = 0x2545F491
    private fun noise(): Double {
        noiseState = noiseState xor (noiseState shl 13)
        noiseState = noiseState xor (noiseState ushr 17)
        noiseState = noiseState xor (noiseState shl 5)
        return (noiseState and 0xFFFF) / 32768.0 - 1.0
    }

    private inline fun tone(seconds: Double, amp: Double, wave: (Double) -> Double): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val attack = (0.002 * SAMPLE_RATE).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = if (i < attack) i.toDouble() / attack else 1.0
            val tail = if (i > n - 64) (n - i) / 64.0 else 1.0
            (wave(t) * amp * env * tail * Short.MAX_VALUE).coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
    }

    /** Arpeggio lembut (nada segitiga + harmonik) dengan akor akhir opsional. */
    private fun sequence(notes: List<Pair<Double, Double>>, chordLast: DoubleArray?): ShortArray {
        val parts = notes.mapIndexed { index, (freq, dur) ->
            val freqs = if (index == notes.lastIndex && chordLast != null) chordLast else doubleArrayOf(freq)
            tone(dur, amp = 0.5 / freqs.size.coerceAtLeast(1) * if (freqs.size > 1) 1.6 else 1.0) { t ->
                var v = 0.0
                for (f in freqs) v += sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t) + 0.12 * sin(6 * PI * f * t)
                v * exp(-t * (if (index == notes.lastIndex) 4.5 else 14.0))
            }
        }
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (p in parts) {
            p.copyInto(out, offset)
            offset += p.size
        }
        return out
    }
}
