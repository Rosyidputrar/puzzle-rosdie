package id.rosdie.puzzle.domain.solver

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

internal class BudgetExceededSignal : RuntimeException() {
    override fun fillInStackTrace(): Throwable = this
}

internal class MemoryLimitSignal : RuntimeException() {
    override fun fillInStackTrace(): Throwable = this
}

/**
 * Pengawas pencarian: pembatalan coroutine (ensureActive), anggaran waktu, dan progres
 * yang dikirim sekitar setiap [intervalMs] (tidak per iterasi).
 */
internal class SearchMonitor(
    private val job: Job?,
    private val nanoClock: () -> Long,
    private val budgetMs: Long?,
    intervalMs: Long,
    var strategy: SolverStrategy,
    private val baseIterations: Long,
    private val baseElapsedMs: Long,
    private val onProgress: (SolverProgress) -> Unit,
) {
    private val start = nanoClock()
    private val intervalNanos = intervalMs * 1_000_000
    private var lastEmit = start
    var iterations = 0L
    var etaProvider: (rate: Double) -> Long? = { null }

    val totalIterations: Long get() = baseIterations + iterations
    val runElapsedMs: Long get() = (nanoClock() - start) / 1_000_000
    val totalElapsedMs: Long get() = baseElapsedMs + runElapsedMs

    fun check(activeStates: Long, bound: Int?) {
        job?.ensureActive()
        val now = nanoClock()
        if (budgetMs != null && (now - start) / 1_000_000 >= budgetMs) throw BudgetExceededSignal()
        if (now - lastEmit >= intervalNanos) {
            lastEmit = now
            onProgress(snapshot(activeStates, bound))
        }
    }

    fun snapshot(activeStates: Long, bound: Int?): SolverProgress {
        val elapsed = totalElapsedMs
        val total = totalIterations
        val rate = if (elapsed > 0) total * 1000.0 / elapsed else 0.0
        return SolverProgress(
            strategy = strategy,
            iterations = total,
            elapsedMs = elapsed,
            activeStates = activeStates,
            iterationsPerSecond = rate.toLong(),
            etaMs = if (rate > 0) etaProvider(rate) else null,
            phase = ThinkingPhase.of(elapsed, total),
            bound = bound,
        )
    }
}

/** Hash map Long -> Int open addressing (key 0 = kosong; state valid tidak pernah 0). */
internal class LongIntMap(initialCapacity: Int = 1 shl 12) {
    private var keys = LongArray(tableSizeFor(initialCapacity))
    private var values = IntArray(keys.size)
    var size = 0
        private set

    fun get(key: Long): Int {
        val mask = keys.size - 1
        var i = mix(key) and mask
        while (true) {
            val k = keys[i]
            if (k == 0L) return -1
            if (k == key) return values[i]
            i = (i + 1) and mask
        }
    }

    fun put(key: Long, value: Int) {
        if ((size + 1) * 2 > keys.size) grow()
        val mask = keys.size - 1
        var i = mix(key) and mask
        while (true) {
            val k = keys[i]
            if (k == 0L) {
                keys[i] = key
                values[i] = value
                size++
                return
            }
            if (k == key) {
                values[i] = value
                return
            }
            i = (i + 1) and mask
        }
    }

    private fun grow() {
        val oldKeys = keys
        val oldValues = values
        keys = LongArray(oldKeys.size * 2)
        values = IntArray(keys.size)
        size = 0
        for (i in oldKeys.indices) if (oldKeys[i] != 0L) put(oldKeys[i], oldValues[i])
    }

    private companion object {
        fun tableSizeFor(n: Int): Int {
            var c = 16
            while (c < n * 2) c = c shl 1
            return c
        }

        fun mix(key: Long): Int {
            var h = key
            h = (h xor (h ushr 33)) * -0xae502812aa7333L
            h = (h xor (h ushr 33)) * -0x3b314601e57a13adL
            h = h xor (h ushr 33)
            return h.toInt()
        }
    }
}

/** Binary min-heap untuk key Long yang sudah memuat prioritas + tie-breaking. */
internal class LongMinHeap(initialCapacity: Int = 1 shl 12) {
    private var heap = LongArray(initialCapacity)
    var size = 0
        private set

    fun isEmpty() = size == 0

    fun push(key: Long) {
        if (size == heap.size) heap = heap.copyOf(heap.size * 2)
        var i = size++
        while (i > 0) {
            val parent = (i - 1) ushr 1
            val p = heap[parent]
            if (p <= key) break
            heap[i] = p
            i = parent
        }
        heap[i] = key
    }

    fun pop(): Long {
        val top = heap[0]
        val last = heap[--size]
        var i = 0
        val half = size ushr 1
        while (i < half) {
            var child = 2 * i + 1
            var c = heap[child]
            val right = child + 1
            if (right < size && heap[right] < c) {
                child = right
                c = heap[right]
            }
            if (last <= c) break
            heap[i] = c
            i = child
        }
        if (size > 0) heap[i] = last
        return top
    }
}
