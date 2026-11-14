package id.rosdie.puzzle.domain.model

/**
 * State papan yang immutable. Tile disimpan baris demi baris; nilai 0 = kotak kosong.
 * Posisi selesai: 1..N-1 berurutan dengan kotak kosong di pojok kanan bawah.
 */
class PuzzleState private constructor(
    val size: Int,
    private val cells: IntArray,
    val blankIndex: Int,
) {
    val cellCount: Int get() = cells.size

    operator fun get(index: Int): Int = cells[index]

    fun toIntArray(): IntArray = cells.copyOf()

    fun toList(): List<Int> = cells.toList()

    fun indexOf(tile: Int): Int = cells.indexOf(tile)

    fun rowOf(index: Int): Int = index / size

    fun colOf(index: Int): Int = index % size

    val isSolved: Boolean
        get() {
            for (i in 0 until cells.size - 1) if (cells[i] != i + 1) return false
            return cells[cells.size - 1] == 0
        }

    /** Menukar kotak kosong dengan sel [index] (harus bertetangga, dicek oleh engine). */
    internal fun swapBlankWith(index: Int): PuzzleState {
        val next = cells.copyOf()
        next[blankIndex] = next[index]
        next[index] = 0
        return PuzzleState(size, next, index)
    }

    override fun equals(other: Any?): Boolean =
        other is PuzzleState && other.size == size && other.cells.contentEquals(cells)

    override fun hashCode(): Int = 31 * size + cells.contentHashCode()

    override fun toString(): String = "PuzzleState(${size}x$size, ${cells.joinToString(",")})"

    companion object {
        const val MIN_SIZE = 2
        const val MAX_SIZE = 6

        fun solved(size: Int): PuzzleState {
            require(size in MIN_SIZE..MAX_SIZE) { "Ukuran grid tidak valid: $size" }
            val n = size * size
            val cells = IntArray(n) { if (it == n - 1) 0 else it + 1 }
            return PuzzleState(size, cells, n - 1)
        }

        /** Membuat state dari daftar tile; melempar [IllegalArgumentException] bila bukan permutasi 0..N-1. */
        fun of(size: Int, tiles: IntArray): PuzzleState {
            require(size in MIN_SIZE..MAX_SIZE) { "Ukuran grid tidak valid: $size" }
            val n = size * size
            require(tiles.size == n) { "Jumlah tile ${tiles.size} tidak sama dengan $n" }
            val seen = BooleanArray(n)
            var blank = -1
            for ((i, t) in tiles.withIndex()) {
                require(t in 0 until n && !seen[t]) { "Tile tidak valid atau ganda: $t" }
                seen[t] = true
                if (t == 0) blank = i
            }
            return PuzzleState(size, tiles.copyOf(), blank)
        }

        fun of(size: Int, tiles: List<Int>): PuzzleState = of(size, tiles.toIntArray())
    }
}
