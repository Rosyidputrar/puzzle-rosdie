package id.rosdie.puzzle.domain.solver

import kotlin.math.abs

/** Ruang pencarian dengan state yang dikemas ke dalam satu Long. */
internal interface SearchSpace {
    fun heuristic(state: Long): Int
    fun isGoal(state: Long): Boolean

    /** Tulis anak-anak [state] ke [outStates]/[outDirs] (ordinal Direction), kembalikan jumlahnya. */
    fun expand(state: Long, outStates: LongArray, outDirs: IntArray): Int
}

/**
 * A* / weighted A* generik. f = g + w*h dengan w = [weightNum]/[weightDen].
 * Open set: binary heap; tie-breaking: f terkecil, lalu h terkecil (lebih dekat ke tujuan),
 * lalu node terbaru (LIFO). Closed/visited: hash map Long -> indeks node.
 */
internal class BestFirstSearch(
    private val space: SearchSpace,
    private val weightNum: Int,
    private val weightDen: Int,
    private val maxNodes: Int,
    private val monitor: SearchMonitor?,
) {
    private var states = LongArray(1 shl 12)
    private var parents = IntArray(states.size)
    private var gs = IntArray(states.size)
    private var dirs = ByteArray(states.size)
    private var count = 0

    /** Mengembalikan daftar ordinal Direction dari start ke goal, atau null bila tidak ada solusi. */
    fun search(start: Long): IntArray? {
        require(maxNodes < IDX_MASK) { "maxNodes terlalu besar" }
        val open = LongMinHeap()
        val best = LongIntMap()
        val childStates = LongArray(4)
        val childDirs = IntArray(4)

        val h0 = space.heuristic(start)
        var minH = h0
        if (monitor != null && weightNum > weightDen) {
            monitor.etaProvider = { rate ->
                // Perkiraan kasar dari laju turunnya h terbaik. Hanya ditampilkan bila sudah ada kemajuan.
                val progressed = h0 - minH
                if (progressed <= 0 || monitor.runElapsedMs < 300) null
                else {
                    val perUnit = monitor.iterations / progressed.toDouble()
                    (minH * perUnit / rate * 1000).toLong()
                }
            }
        }
        val startIdx = addNode(start, -1, 0, -1)
        best.put(start, startIdx)
        open.push(key(0, h0, startIdx))

        while (!open.isEmpty()) {
            val k = open.pop()
            val idx = (IDX_MASK - (k and IDX_MASK)).toInt()
            val s = states[idx]
            if (best.get(s) != idx) continue // entri usang
            val g = gs[idx]
            if (space.isGoal(s)) return path(idx)

            if (monitor != null) {
                monitor.iterations++
                if (monitor.iterations and 1023L == 0L) monitor.check(open.size.toLong(), null)
            }

            val n = space.expand(s, childStates, childDirs)
            val parentDir = dirs[idx].toInt()
            for (c in 0 until n) {
                val d = childDirs[c]
                if (parentDir >= 0 && d == OPPOSITE[parentDir]) continue
                val cs = childStates[c]
                val ng = g + 1
                val existing = best.get(cs)
                if (existing >= 0 && gs[existing] <= ng) continue
                if (count >= maxNodes) throw MemoryLimitSignal()
                val h = space.heuristic(cs)
                if (h < minH) minH = h
                val ci = addNode(cs, idx, ng, d)
                best.put(cs, ci)
                open.push(key(ng, h, ci))
            }
        }
        return null
    }

    private fun key(g: Int, h: Int, idx: Int): Long {
        val f = g.toLong() * weightDen + h.toLong() * weightNum
        return (f shl 40) or (h.toLong() shl 28) or (IDX_MASK - idx)
    }

    private fun addNode(state: Long, parent: Int, g: Int, dir: Int): Int {
        if (count == states.size) {
            val n = states.size * 2
            states = states.copyOf(n)
            parents = parents.copyOf(n)
            gs = gs.copyOf(n)
            dirs = dirs.copyOf(n)
        }
        states[count] = state
        parents[count] = parent
        gs[count] = g
        dirs[count] = dir.toByte()
        return count++
    }

    private fun path(goalIdx: Int): IntArray {
        val out = IntArray(gs[goalIdx])
        var i = goalIdx
        var p = out.size - 1
        while (parents[i] >= 0) {
            out[p--] = dirs[i].toInt()
            i = parents[i]
        }
        return out
    }

    companion object {
        private const val IDX_MASK = (1L shl 28) - 1
        /** OPPOSITE[d] untuk ordinal Direction UP, DOWN, LEFT, RIGHT. */
        val OPPOSITE = intArrayOf(1, 0, 3, 2)
        val D_ROW = intArrayOf(-1, 1, 0, 0)
        val D_COL = intArrayOf(0, 0, -1, 1)
    }
}

/** Papan lengkap N<=4 dikemas 4 bit per sel. Heuristik MD + LC. */
internal class PackedTileSpace(private val size: Int) : SearchSpace {
    private val n = size * size
    private val goal: Long
    private val scratch = IntArray(n)

    init {
        require(size <= 4) { "PackedTileSpace hanya untuk N <= 4" }
        var g = 0L
        for (i in 0 until n - 1) g = g or ((i + 1).toLong() shl (4 * i))
        goal = g
    }

    override fun heuristic(state: Long): Int {
        unpack(state, scratch)
        return Heuristics.estimate(scratch, size)
    }

    override fun isGoal(state: Long): Boolean = state == goal

    override fun expand(state: Long, outStates: LongArray, outDirs: IntArray): Int {
        var blank = 0
        while (((state ushr (4 * blank)) and 0xF) != 0L) blank++
        val br = blank / size
        val bc = blank % size
        var count = 0
        for (d in 0 until 4) {
            // Tile berada di blank - delta(d) dan bergeser searah d ke kotak kosong.
            val r = br - BestFirstSearch.D_ROW[d]
            val c = bc - BestFirstSearch.D_COL[d]
            if (r !in 0 until size || c !in 0 until size) continue
            val t = r * size + c
            val v = (state ushr (4 * t)) and 0xF
            outStates[count] = state - (v shl (4 * t)) + (v shl (4 * blank))
            outDirs[count] = d
            count++
        }
        return count
    }

    companion object {
        fun pack(cells: IntArray): Long {
            var s = 0L
            for (i in cells.indices) s = s or (cells[i].toLong() shl (4 * i))
            return s
        }

        fun unpack(state: Long, out: IntArray) {
            for (i in out.indices) out[i] = ((state ushr (4 * i)) and 0xF).toInt()
        }
    }
}

/**
 * Ruang abstrak untuk solver bertahap: hanya posisi kotak kosong dan tile target yang
 * dilacak (6 bit per entitas, maks 10 entitas). Tile lain tidak dibedakan, sel terkunci
 * tidak boleh dilewati. Heuristik: jumlah Manhattan tile target (admissible).
 */
internal class GroupSpace(
    private val size: Int,
    private val locked: BooleanArray,
    private val targetGoals: IntArray,
) : SearchSpace {
    private val entities = targetGoals.size + 1

    init {
        require(entities <= 10) { "Terlalu banyak entitas" }
    }

    private fun pos(state: Long, e: Int): Int = ((state ushr (6 * e)) and 0x3F).toInt()

    override fun heuristic(state: Long): Int {
        var h = 0
        for (i in targetGoals.indices) {
            val p = pos(state, i + 1)
            val g = targetGoals[i]
            h += abs(p / size - g / size) + abs(p % size - g % size)
        }
        return h
    }

    override fun isGoal(state: Long): Boolean {
        for (i in targetGoals.indices) if (pos(state, i + 1) != targetGoals[i]) return false
        return true
    }

    override fun expand(state: Long, outStates: LongArray, outDirs: IntArray): Int {
        val blank = pos(state, 0)
        val br = blank / size
        val bc = blank % size
        var count = 0
        for (d in 0 until 4) {
            val r = br - BestFirstSearch.D_ROW[d]
            val c = bc - BestFirstSearch.D_COL[d]
            if (r !in 0 until size || c !in 0 until size) continue
            val t = r * size + c
            if (locked[t]) continue
            var next = (state and 0x3FL.inv()) or t.toLong()
            for (e in 1 until entities) {
                if (pos(state, e) == t) {
                    val shift = 6 * e
                    next = (next and (0x3FL shl shift).inv()) or (blank.toLong() shl shift)
                    break
                }
            }
            outStates[count] = next
            outDirs[count] = d
            count++
        }
        return count
    }

    companion object {
        fun pack(blank: Int, tilePositions: IntArray): Long {
            var s = blank.toLong()
            for (i in tilePositions.indices) s = s or (tilePositions[i].toLong() shl (6 * (i + 1)))
            return s
        }
    }
}
