package id.rosdie.puzzle.domain.model

/**
 * Arah gerak TILE (bukan kotak kosong). [UP] berarti tile yang berada tepat di bawah
 * kotak kosong bergeser ke atas mengisi kotak kosong. Ini sama dengan arah swipe pemain.
 */
enum class Direction(val dRow: Int, val dCol: Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}

/** Satu gerakan yang sudah diterapkan: tile [tile] pindah dari indeks [from] ke [to]. */
data class Move(val tile: Int, val from: Int, val to: Int, val direction: Direction)
