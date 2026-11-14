package id.rosdie.puzzle.domain.engine

import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.PuzzleState

/** Aturan sliding puzzle. Semua fungsi murni; state input tidak pernah diubah. */
object PuzzleEngine {
    const val MIN_SHUFFLE_STEPS = 1000
    const val MAX_SHUFFLE_STEPS = 2000

    /** Indeks tile yang akan bergerak bila arah [direction] dipilih, atau -1 bila tidak valid. */
    fun tileIndexFor(state: PuzzleState, direction: Direction): Int {
        val size = state.size
        val br = state.blankIndex / size
        val bc = state.blankIndex % size
        val r = br - direction.dRow
        val c = bc - direction.dCol
        return if (r in 0 until size && c in 0 until size) r * size + c else -1
    }

    fun canMove(state: PuzzleState, direction: Direction): Boolean = tileIndexFor(state, direction) >= 0

    fun validMoves(state: PuzzleState): List<Direction> = Direction.entries.filter { canMove(state, it) }

    /** Menerapkan satu gerakan. Mengembalikan null bila gerakan tidak valid. */
    fun move(state: PuzzleState, direction: Direction): PuzzleState? {
        val idx = tileIndexFor(state, direction)
        return if (idx < 0) null else state.swapBlankWith(idx)
    }

    fun applyAll(state: PuzzleState, moves: List<Direction>): PuzzleState =
        moves.fold(state) { s, d -> move(s, d) ?: throw IllegalArgumentException("Gerakan tidak valid: $d pada $s") }

    /** Arah gerak bila tile di [index] bersebelahan dengan kotak kosong, null bila tidak. */
    fun directionForTile(state: PuzzleState, index: Int): Direction? =
        Direction.entries.firstOrNull { tileIndexFor(state, it) == index }

    /**
     * Gerak barisan: bila tile di [index] satu baris/kolom dengan kotak kosong, kembalikan
     * urutan gerakan yang menggeser seluruh barisan tile di antaranya (seperti puzzle fisik).
     * Setiap tile yang bergeser dihitung sebagai satu langkah. Kosong bila tidak sejajar.
     */
    fun lineMoves(state: PuzzleState, index: Int): List<Direction> {
        val size = state.size
        if (index !in 0 until state.cellCount || index == state.blankIndex) return emptyList()
        val r = index / size
        val c = index % size
        val br = state.blankIndex / size
        val bc = state.blankIndex % size
        return when {
            r == br && c < bc -> List(bc - c) { Direction.RIGHT }
            r == br && c > bc -> List(c - bc) { Direction.LEFT }
            c == bc && r < br -> List(br - r) { Direction.DOWN }
            c == bc && r > br -> List(r - br) { Direction.UP }
            else -> emptyList()
        }
    }

    fun isSolved(state: PuzzleState): Boolean = state.isSolved

    /** Jumlah inversi (pasangan tile terbalik), kotak kosong diabaikan. */
    fun inversions(state: PuzzleState): Int {
        var inv = 0
        val n = state.cellCount
        for (i in 0 until n) {
            val a = state[i]
            if (a == 0) continue
            for (j in i + 1 until n) {
                val b = state[j]
                if (b != 0 && b < a) inv++
            }
        }
        return inv
    }

    /**
     * Aturan inversi:
     * - lebar ganjil: solvable jika jumlah inversi genap.
     * - lebar genap: solvable jika (inversi + baris kotak kosong dari atas, 0-based) ganjil.
     *   Pada posisi selesai inversi = 0 dan kotak kosong di baris N-1 (ganjil), dan setiap
     *   gerak vertikal mengubah paritas inversi dan baris sekaligus.
     */
    fun isSolvable(state: PuzzleState): Boolean {
        val inv = inversions(state)
        return if (state.size % 2 == 1) {
            inv % 2 == 0
        } else {
            (inv + state.blankIndex / state.size) % 2 == 1
        }
    }

    /**
     * Acakan dijamin solvable: random walk 1000..2000 langkah valid dari posisi selesai.
     * Langkah yang langsung membatalkan langkah sebelumnya ditolak. Bila hasil akhir sama
     * dengan posisi selesai, acak ulang.
     */
    fun shuffle(size: Int, random: SeededRandom): PuzzleState {
        while (true) {
            val steps = random.nextInt(MIN_SHUFFLE_STEPS, MAX_SHUFFLE_STEPS + 1)
            var state = PuzzleState.solved(size)
            var previous: Direction? = null
            val options = ArrayList<Direction>(4)
            repeat(steps) {
                options.clear()
                for (d in Direction.entries) {
                    if (d != previous?.opposite && canMove(state, d)) options.add(d)
                }
                val chosen = options[random.nextInt(options.size)]
                state = move(state, chosen)!!
                previous = chosen
            }
            if (!state.isSolved) return state
        }
    }

    fun shuffle(size: Int, seed: Long): PuzzleState = shuffle(size, SeededRandom(seed))

    /** Seed Puzzle Harian dari tanggal lokal yyyyMMdd. */
    fun dailySeed(dateKey: Int): Long = SeededRandom(dateKey.toLong() xor DAILY_SALT).nextLong()

    /** Puzzle Harian: selalu 4x4 mode angka agar semua pemain mendapat puzzle yang sama. */
    fun dailyPuzzle(dateKey: Int): PuzzleState = shuffle(DAILY_SIZE, dailySeed(dateKey))

    const val DAILY_SIZE = 4
    private const val DAILY_SALT = 0x524F534449455A5AL // "ROSDIEZZ"
}
