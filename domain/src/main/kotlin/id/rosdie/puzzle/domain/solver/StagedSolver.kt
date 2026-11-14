package id.rosdie.puzzle.domain.solver

/**
 * Solver bertahap untuk grid besar (5x5, dan cadangan untuk 4x4):
 * selesaikan baris teratas lalu kolom paling kiri yang tersisa (berulang) sampai sisa 3x3,
 * lalu sisa 3x3 diselesaikan dengan A*. Setiap tahap mencari jalur terpendek untuk kelompok
 * kecil tile (dua tile terakhir tiap baris/kolom ditempatkan bersama), tetapi gabungannya
 * TIDAK dijamin terpendek.
 */
internal class StagedSolver(private val size: Int, private val monitor: SearchMonitor?) {

    fun solve(cells: IntArray, maxNodesPerStage: Int): IntArray {
        val board = cells.copyOf()
        val locked = BooleanArray(size * size)
        val result = ArrayList<Int>()
        for (group in plan()) {
            val moves = solveGroup(board, locked, group, maxNodesPerStage)
            for (d in moves) {
                applyMove(board, d)
                result.add(d)
            }
            for (goal in group) locked[goal] = true
        }
        return simplify(result)
    }

    /** Daftar kelompok posisi tujuan (indeks sel), sesuai urutan pengerjaan. */
    internal fun plan(): List<IntArray> {
        val groups = ArrayList<IntArray>()
        for (k in 0 until size - 3) {
            groups += lineGroups((k until size).map { k * size + it })
            groups += lineGroups((k + 1 until size).map { it * size + k })
        }
        val start = size - 3
        val rest = ArrayList<Int>()
        for (r in start until size) for (c in start until size) {
            val idx = r * size + c
            if (idx != size * size - 1) rest += idx
        }
        groups += rest.toIntArray()
        return groups
    }

    /** Semua tile kecuali dua terakhir dalam kelompok berisi maks 3; dua terakhir bersama. */
    private fun lineGroups(cells: List<Int>): List<IntArray> {
        if (cells.size <= 2) return listOf(cells.toIntArray())
        val head = cells.dropLast(2)
        val out = head.chunked(3).map { it.toIntArray() }.toMutableList()
        out += cells.takeLast(2).toIntArray()
        return out
    }

    private fun solveGroup(board: IntArray, locked: BooleanArray, goals: IntArray, maxNodes: Int): IntArray {
        val positions = IntArray(goals.size) { i -> board.indexOf(goals[i] + 1) }
        val start = GroupSpace.pack(board.indexOf(0), positions)
        val space = GroupSpace(size, locked, goals)
        if (space.isGoal(start)) return IntArray(0)
        return BestFirstSearch(space, 1, 1, maxNodes, monitor).search(start)
            ?: throw IllegalStateException("Tahap tidak bisa diselesaikan")
    }

    private fun applyMove(board: IntArray, d: Int) {
        val blank = board.indexOf(0)
        val r = blank / size - BestFirstSearch.D_ROW[d]
        val c = blank % size - BestFirstSearch.D_COL[d]
        val t = r * size + c
        board[blank] = board[t]
        board[t] = 0
    }

    /** Hapus pasangan gerakan yang saling membatalkan (mis. di batas antar tahap). */
    private fun simplify(moves: List<Int>): IntArray {
        val stack = IntArray(moves.size)
        var top = 0
        for (d in moves) {
            if (top > 0 && stack[top - 1] == BestFirstSearch.OPPOSITE[d]) top-- else stack[top++] = d
        }
        return stack.copyOf(top)
    }
}
