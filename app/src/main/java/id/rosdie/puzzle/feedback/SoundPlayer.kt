package id.rosdie.puzzle.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.datastore.ClickSound
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Memutar bunyi buatan sendiri lewat SoundPool. File WAV disintesis sekali ke cacheDir. */
class SoundPlayer(
    context: Context,
    private val settings: StateFlow<AppSettings>,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<SoundSynth.Sound, Int>()
    private val loaded = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(loaded) { loaded += sampleId }
        }
        val dir = File(context.cacheDir, "sounds_v1")
        scope.launch(io) {
            dir.mkdirs()
            for (sound in SoundSynth.Sound.entries) {
                val file = File(dir, sound.fileName)
                if (!file.exists()) runCatching { file.writeBytes(SoundSynth.toWav(SoundSynth.render(sound))) }
                if (file.exists()) {
                    val id = pool.load(file.absolutePath, 1)
                    synchronized(ids) { ids[sound] = id }
                }
            }
        }
    }

    fun click() = play(
        when (settings.value.clickSound) {
            ClickSound.SOFT -> SoundSynth.Sound.CLICK_SOFT
            ClickSound.WOOD -> SoundSynth.Sound.CLICK_WOOD
            ClickSound.POP -> SoundSynth.Sound.CLICK_POP
        },
    )

    fun preview(variant: ClickSound) = play(
        when (variant) {
            ClickSound.SOFT -> SoundSynth.Sound.CLICK_SOFT
            ClickSound.WOOD -> SoundSynth.Sound.CLICK_WOOD
            ClickSound.POP -> SoundSynth.Sound.CLICK_POP
        },
        force = true,
    )

    fun invalid() = play(SoundSynth.Sound.INVALID)
    fun aiStep() = play(SoundSynth.Sound.AI_STEP)
    fun win() = play(SoundSynth.Sound.WIN)
    fun lose() = play(SoundSynth.Sound.LOSE)

    private fun play(sound: SoundSynth.Sound, force: Boolean = false) {
        val s = settings.value
        if (!s.soundEnabled && !force) return
        val id = synchronized(ids) { ids[sound] } ?: return
        if (synchronized(loaded) { id !in loaded }) return
        val v = s.soundVolume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 1f)
    }
}
