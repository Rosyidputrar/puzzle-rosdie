package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolveMode
import id.rosdie.puzzle.domain.solver.SolveRequest
import id.rosdie.puzzle.domain.solver.SolverResult
import kotlinx.coroutines.runBlocking

/**
 * Benchmark solver: `./gradlew :domain:benchmark`. Instans = acakan aplikasi (seed 1..N).
 * Mencetak tabel Markdown untuk README.
 */
fun main() = runBlocking {
    val solver = PuzzleSolver()
    // Pemanasan JIT
    repeat(3) { solver.solve(PuzzleEngine.shuffle(4, 1000L + it), SolveRequest(mode = SolveMode.QUICK)) }

    println("| Grid | Strategi | Instans | Selesai | Panjang rata-rata | Waktu median | Waktu maks | Iterasi rata-rata |")
    println("|---|---|---|---|---|---|---|---|")
    bench(solver, 3, SolveMode.BEST, 200, null)
    bench(solver, 4, SolveMode.BEST, 30, 60_000)
    bench(solver, 4, SolveMode.QUICK, 100, null)
    bench(solver, 5, SolveMode.BEST, 100, null)
}

private suspend fun bench(solver: PuzzleSolver, size: Int, mode: SolveMode, count: Int, budget: Long?) {
    val times = ArrayList<Long>()
    val lengths = ArrayList<Int>()
    val iterations = ArrayList<Long>()
    var strategyName = ""
    var solved = 0
    for (seed in 1..count) {
        val s: PuzzleState = PuzzleEngine.shuffle(size, seed.toLong())
        val t0 = System.nanoTime()
        val r = solver.solve(s, SolveRequest(mode = mode, timeBudgetMs = budget))
        val ms = (System.nanoTime() - t0) / 1_000_000
        times += ms
        when (r) {
            is SolverResult.Solved -> {
                check(PuzzleEngine.applyAll(s, r.moves).isSolved)
                solved++
                lengths += r.moves.size
                iterations += r.stats.iterations
                strategyName = r.strategy.name
            }
            is SolverResult.BudgetExceeded -> {
                iterations += r.stats.iterations
                strategyName = r.resume.strategy.name
            }
            SolverResult.Unsolvable -> error("tidak mungkin")
        }
    }
    times.sort()
    val budgetNote = budget?.let { " (anggaran ${it / 1000} dtk)" } ?: ""
    println(
        "| ${size}x$size | $strategyName$budgetNote | $count | $solved | " +
            "${"%.1f".format(lengths.average())} | ${times[times.size / 2]} ms | ${times.last()} ms | " +
            "${"%,d".format(iterations.average().toLong())} |",
    )
}
