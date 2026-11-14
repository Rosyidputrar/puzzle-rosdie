package id.rosdie.puzzle

import android.content.Context
import androidx.room.Room
import id.rosdie.puzzle.data.local.PuzzleDatabase
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.TimeSource
import kotlin.coroutines.CoroutineContext

/** Jam palsu yang dikendalikan test. */
class FakeTimeSource(var elapsed: Long = 1_000_000L, var now: Long = 1_789_000_000_000L) : TimeSource {
    override fun elapsedMs(): Long = elapsed
    override fun nowMs(): Long = now
    fun advance(ms: Long) {
        elapsed += ms
        now += ms
    }
    fun setDay(key: Int) {
        now = DateKeys.epochDayFromKey(key) * 86_400_000L + 12 * 3_600_000L -
            java.util.TimeZone.getDefault().getOffset(now)
    }
}

fun inMemoryDb(context: Context, queryContext: CoroutineContext? = null): PuzzleDatabase {
    val b = Room.inMemoryDatabaseBuilder(context, PuzzleDatabase::class.java).allowMainThreadQueries()
    if (queryContext != null) b.setQueryCoroutineContext(queryContext)
    return b.build()
}
