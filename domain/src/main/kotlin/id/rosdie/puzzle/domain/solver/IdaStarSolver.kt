package id.rosdie.puzzle.domain.solver

import kotlin.math.abs

/**
 * IDA* optimal untuk N <= 4 dengan Manhattan + Linear Conflict yang diperbarui secara
 * inkremental. Linear conflict per baris/kolom diambil dari tabel (key = isi baris, 4 bit
 * per sel), jadi biaya per node O(1). Memori hanya O(kedalaman solusi).
 */
internal class IdaStarSolver(private val size: Int, private val monitor: SearchMonitor) {
    private val n = size * size
    private val board = IntArray(n)
    private var blank = 0
    private val goalRow = IntArray(n)
    private val goalCol = IntArray(n)
    private val rowKey = IntArray(size)
    private val colKey = IntArray(size)
    private var md = 0
    private var lc = 0
    private val tables = LcTables.forSize(size)

    /** neighbor[pos*4 + d] = indeks tile yang bergeser searah d ke kotak kosong di pos, atau -1. */
    private val neighbor = IntArray(n * 4)
    private val path = IntArray(MAX_DEPTH)
    private var depth = 0

    /** Jumlah node per iterasi yang sudah selesai (untuk perkiraan sisa waktu). */
    val iterationNodes = ArrayList<Long>()
    var currentBound = 0
        private set
    private var iterationStartNodes = 0L

    init {
        require(size in 2..4) { "IDA* tabel hanya untuk N <= 4" }
        for (t in 1 until n) {
            goalRow[t] = (t - 1) / size
            goalCol[t] = (t - 1) % size
        }
        for (pos in 0 until n) {
            val r = pos / size
            val c = pos % size
            for (d in 0 until 4) {
                val tr = r - BestFirstSearch.D_ROW[d]
                val tc = c - BestFirstSearch.D_COL[d]
                neighbor[pos * 4 + d] = if (tr in 0 until size && tc in 0 until size) tr * size + tc else -1
            }
        }
        monitor.etaProvider = { rate -> estimateRemainingMs(rate) }
    }

    /**
     * Cari solusi optimal. Melempar [BudgetExceededSignal] bila anggaran habis; [currentBound]
     * lalu berisi batas iterasi yang belum selesai sehingga bisa dilanjutkan.
     */
    fun solve(cells: IntArray, startBound: Int?, history: List<Long>): IntArray {
        load(cells)
        iterationNodes.clear()
        iterationNodes.addAll(history)
        val h0 = md + lc
        var bound = maxOf(h0, startBound ?: h0)
        while (true) {
            currentBound = bound
            iterationStartNodes = monitor.iterations
            depth = 0
            val t = dfs(0, bound, -1)
            if (t == FOUND) return path.copyOf(depth)
            iterationNodes.add(monitor.iterations - iterationStartNodes)
            check(t != Int.MAX_VALUE) { "Tidak ada solusi (state tidak solvable?)" }
            bound = t
        }
    }

    private fun load(cells: IntArray) {
        for (i in 0 until n) {
            board[i] = cells[i]
            if (cells[i] == 0) blank = i
        }
        md = 0
        for (i in 0 until n) {
            val t = board[i]
            if (t != 0) md += abs(i / size - goalRow[t]) + abs(i % size - goalCol[t])
        }
        lc = 0
        for (line in 0 until size) {
            var rk = 0
            var ck = 0
            for (j in 0 until size) {
                rk = rk or (board[line * size + j] shl (4 * j))
                ck = ck or (board[j * size + line] shl (4 * j))
            }
            rowKey[line] = rk
            colKey[line] = ck
            lc += tables.row[line * tables.stride + rk] + tables.col[line * tables.stride + ck]
        }
    }

    /** Geser tile di [tilePos] ke kotak kosong, perbarui MD, LC, dan key baris/kolom. */
    private fun slide(tilePos: Int) {
        val v = board[tilePos]
        val b = blank
        val tr = tilePos / size
        val tc = tilePos % size
        val br = b / size
        val bc = b % size
        if (tc == bc) {
            // Vertikal: tile pindah baris tr -> br di kolom yang sama.
            md += abs(br - goalRow[v]) - abs(tr - goalRow[v])
            val stride = tables.stride
            lc -= tables.row[tr * stride + rowKey[tr]] + tables.row[br * stride + rowKey[br]]
            val shift = 4 * tc
            rowKey[tr] = rowKey[tr] and (0xF shl shift).inv()
            rowKey[br] = rowKey[br] or (v shl shift)
            lc += tables.row[tr * stride + rowKey[tr]] + tables.row[br * stride + rowKey[br]]
            // Urutan tile di kolom tidak berubah, hanya key-nya.
            colKey[tc] = (colKey[tc] and (0xF shl (4 * tr)).inv()) or (v shl (4 * br))
        } else {
            // Horizontal: tile pindah kolom tc -> bc di baris yang sama.
            md += abs(bc - goalCol[v]) - abs(tc - goalCol[v])
            val stride = tables.stride
            lc -= tables.col[tc * stride + colKey[tc]] + tables.col[bc * stride + colKey[bc]]
            val shift = 4 * tr
            colKey[tc] = colKey[tc] and (0xF shl shift).inv()
            colKey[bc] = colKey[bc] or (v shl shift)
            lc += tables.col[tc * stride + colKey[tc]] + tables.col[bc * stride + colKey[bc]]
            rowKey[tr] = (rowKey[tr] and (0xF shl (4 * tc)).inv()) or (v shl (4 * bc))
        }
        board[b] = v
        board[tilePos] = 0
        blank = tilePos
    }

    private fun dfs(g: Int, bound: Int, prevDir: Int): Int {
        val h = md + lc
        val f = g + h
        if (f > bound) return f
        if (h == 0) {
            depth = g
            return FOUND
        }
        val it = ++monitor.iterations
        if (it and 4095L == 0L) monitor.check(g.toLong(), bound)
        var min = Int.MAX_VALUE
        val base = blank * 4
        for (d in 0 until 4) {
            if (prevDir >= 0 && d == BestFirstSearch.OPPOSITE[prevDir]) continue
            val tilePos = neighbor[base + d]
            if (tilePos < 0) continue
            val from = blank
            slide(tilePos)
            path[g] = d
            val t = dfs(g + 1, bound, d)
            if (t == FOUND) return FOUND
            slide(from) // batalkan: tile kembali ke posisi semula
            if (t < min) min = t
        }
        return min
    }

    /**
     * Perkiraan sisa waktu iterasi (batas f) yang sedang berjalan, dari rasio pertumbuhan
     * node antar iterasi. Null bila belum ada cukup data (< 2 iterasi selesai) atau bila
     * iterasi sudah melampaui perkiraan.
     */
    private fun estimateRemainingMs(rate: Double): Long? {
        if (iterationNodes.size < 2) return null
        val last = iterationNodes[iterationNodes.size - 1].toDouble()
        val prev = iterationNodes[iterationNodes.size - 2].toDouble()
        if (prev <= 0 || last <= 0) return null
        val growth = last / prev
        val expected = last * growth
        val done = (monitor.iterations - iterationStartNodes).toDouble()
        val remaining = expected - done
        if (remaining <= 0) return null
        return (remaining / rate * 1000).toLong()
    }

    internal class LcTables(val row: ByteArray, val col: ByteArray, val stride: Int) {
        companion object {
            private val cache = arrayOfNulls<LcTables>(5)

            @Synchronized
            fun forSize(size: Int): LcTables = cache[size] ?: build(size).also { cache[size] = it }

            private fun build(size: Int): LcTables {
                val stride = 1 shl (4 * size)
                val n = size * size
                val row = ByteArray(size * stride)
                val col = ByteArray(size * stride)
                val buf = IntArray(size)
                for (line in 0 until size) {
                    for (key in 0 until stride) {
                        var kr = 0
                        var kc = 0
                        for (j in 0 until size) {
                            val t = (key ushr (4 * j)) and 0xF
                            if (t == 0 || t >= n) continue
                            if ((t - 1) / size == line) buf[kr++] = (t - 1) % size
                        }
                        row[line * stride + key] = (2 * (kr - Heuristics.lisLength(buf, kr))).toByte()
                        for (j in 0 until size) {
                            val t = (key ushr (4 * j)) and 0xF
                            if (t == 0 || t >= n) continue
                            if ((t - 1) % size == line) buf[kc++] = (t - 1) / size
                        }
                        col[line * stride + key] = (2 * (kc - Heuristics.lisLength(buf, kc))).toByte()
                    }
                }
                return LcTables(row, col, stride)
            }
        }
    }

    private companion object {
        const val FOUND = -1
        const val MAX_DEPTH = 256
    }
}
