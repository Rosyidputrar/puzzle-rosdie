package id.rosdie.puzzle.domain.model

/** Tingkat kesulitan = ukuran grid. */
enum class Difficulty(val size: Int) {
    EASY(3),
    NORMAL(4),
    HARD(5);

    companion object {
        fun fromSize(size: Int): Difficulty =
            entries.firstOrNull { it.size == size } ?: throw IllegalArgumentException("Ukuran grid tidak didukung: $size")
    }
}
