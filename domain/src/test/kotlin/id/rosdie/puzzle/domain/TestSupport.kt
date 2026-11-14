package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.engine.SeededRandom
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.PackedTileSpace

/** Jarak eksak semua 181.440 state 3x3 dari posisi selesai (BFS). */
object Bfs3x3 {
    val distances: Map<Long, Int> by lazy {
        val start = PuzzleState.solved(3)
        val dist = HashMap<Long, Int>(400_000)
        val queue = ArrayDeque<PuzzleState>()
        dist[PackedTileSpace.pack(start.toIntArray())] = 0
        queue.add(start)
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            val d = dist.getValue(PackedTileSpace.pack(s.toIntArray()))
            for (m in PuzzleEngine.validMoves(s)) {
                val next = PuzzleEngine.move(s, m)!!
                val key = PackedTileSpace.pack(next.toIntArray())
                if (key !in dist) {
                    dist[key] = d + 1
                    queue.add(next)
                }
            }
        }
        dist
    }

    fun distance(state: PuzzleState): Int = distances.getValue(PackedTileSpace.pack(state.toIntArray()))
}

/** BFS kecil untuk mendapatkan jarak optimal eksak instans dangkal (4x4). */
fun bfsDistance(start: PuzzleState, maxDepth: Int): Int? {
    if (start.isSolved) return 0
    val seen = HashSet<PuzzleState>()
    var frontier = listOf(start)
    seen += start
    for (depth in 1..maxDepth) {
        val next = ArrayList<PuzzleState>()
        for (s in frontier) for (m in PuzzleEngine.validMoves(s)) {
            val c = PuzzleEngine.move(s, m)!!
            if (c.isSolved) return depth
            if (seen.add(c)) next += c
        }
        frontier = next
    }
    return null
}

/** Random walk pendek tanpa langkah balik, untuk instans dengan kedalaman terkendali. */
fun walk(size: Int, steps: Int, seed: Long): PuzzleState {
    val rnd = SeededRandom(seed)
    var s = PuzzleState.solved(size)
    var prev: Direction? = null
    repeat(steps) {
        val options = PuzzleEngine.validMoves(s).filter { it != prev?.opposite }
        val d = options[rnd.nextInt(options.size)]
        s = PuzzleEngine.move(s, d)!!
        prev = d
    }
    return s
}
