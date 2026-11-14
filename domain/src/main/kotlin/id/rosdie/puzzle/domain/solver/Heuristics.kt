package id.rosdie.puzzle.domain.solver

import id.rosdie.puzzle.domain.model.PuzzleState
import kotlin.math.abs

/**
 * Heuristik Manhattan Distance + Linear Conflict (admissible dan konsisten).
 *
 * Linear conflict dihitung secara eksak per baris/kolom: dari tile-tile yang tujuan barisnya
 * adalah baris itu, minimal (k - LIS) tile harus keluar dari baris lalu kembali (2 langkah
 * tambahan masing-masing). Gerak vertikal yang dibutuhkan konflik baris dan gerak horizontal
 * yang dibutuhkan konflik kolom tidak pernah dihitung oleh Manhattan, jadi jumlahnya tetap
 * admissible (Hansson, Mayer & Yung, 1992). Diverifikasi exhaustif untuk 3x3 di unit test.
 */
object Heuristics {

    fun manhattan(state: PuzzleState): Int = manhattan(state.toIntArray(), state.size)

    fun manhattan(cells: IntArray, size: Int): Int {
        var sum = 0
        for (i in cells.indices) {
            val t = cells[i]
            if (t == 0) continue
            val goal = t - 1
            sum += abs(i / size - goal / size) + abs(i % size - goal % size)
        }
        return sum
    }

    fun linearConflict(state: PuzzleState): Int = linearConflict(state.toIntArray(), state.size)

    fun linearConflict(cells: IntArray, size: Int): Int {
        val buf = IntArray(size)
        var total = 0
        for (line in 0 until size) {
            // Baris `line`
            var k = 0
            for (c in 0 until size) {
                val t = cells[line * size + c]
                if (t != 0 && (t - 1) / size == line) buf[k++] = (t - 1) % size
            }
            total += 2 * (k - lisLength(buf, k))
            // Kolom `line`
            k = 0
            for (r in 0 until size) {
                val t = cells[r * size + line]
                if (t != 0 && (t - 1) % size == line) buf[k++] = (t - 1) / size
            }
            total += 2 * (k - lisLength(buf, k))
        }
        return total
    }

    fun estimate(state: PuzzleState): Int = estimate(state.toIntArray(), state.size)

    fun estimate(cells: IntArray, size: Int): Int = manhattan(cells, size) + linearConflict(cells, size)

    /** Panjang longest strictly increasing subsequence dari values[0 until count] (count <= 6). */
    internal fun lisLength(values: IntArray, count: Int): Int {
        if (count <= 1) return count
        val best = IntArray(count)
        var result = 0
        for (i in 0 until count) {
            var b = 1
            for (j in 0 until i) if (values[j] < values[i] && best[j] + 1 > b) b = best[j] + 1
            best[i] = b
            if (b > result) result = b
        }
        return result
    }
}
