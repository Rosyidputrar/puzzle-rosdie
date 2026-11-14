package id.rosdie.puzzle.data.repository

import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult

data class GridStats(
    val gridSize: Int,
    val games: Int,
    val wins: Int,
    val averageMoves: Double?,
    val averageTimeSeconds: Double?,
    /** Rekor: hanya kemenangan tanpa AI/hint. */
    val record: GridRecord?,
)

data class DayActivity(val epochDay: Long, val games: Int, val wins: Int)

data class StatsSummary(
    val totalGames: Int,
    val totalWins: Int,
    val perGrid: List<GridStats>,
    val last7Days: List<DayActivity>,
    val dailyCompleted: Set<Int>,
    val currentStreak: Int,
    val bestStreak: Int,
    val todayDailyDone: Boolean,
) {
    val hasGames: Boolean get() = totalGames > 0
    val winRate: Double? get() = if (totalGames == 0) null else totalWins.toDouble() / totalGames
    val totalSolved: Int get() = totalWins
}

/** Statistik dihitung dari data lokal saja. Tanpa data → nilai kosong, bukan angka palsu. */
object StatsCalculator {
    val GRID_SIZES = listOf(3, 4, 5)

    fun compute(results: List<GameResult>, todayEpochDay: Long, epochDayOf: (Long) -> Long): StatsSummary {
        val wins = results.filter { it.outcome == GameOutcome.WON }
        val perGrid = GRID_SIZES.map { size ->
            val games = results.filter { it.gridSize == size }
            val gridWins = wins.filter { it.gridSize == size }
            val eligible = gridWins.filter { it.eligibleForRecord }
            GridStats(
                gridSize = size,
                games = games.size,
                wins = gridWins.size,
                averageMoves = gridWins.takeIf { it.isNotEmpty() }?.map { it.moves }?.average(),
                averageTimeSeconds = gridWins.takeIf { it.isNotEmpty() }?.map { it.timeSeconds }?.average(),
                record = eligible.takeIf { it.isNotEmpty() }?.let { list ->
                    GridRecord(size, list.minOf { it.moves }, list.minOf { it.timeSeconds })
                },
            )
        }
        val byDay = results.groupBy { epochDayOf(it.playedAt) }
        val last7 = (6 downTo 0).map { back ->
            val day = todayEpochDay - back
            val list = byDay[day].orEmpty()
            DayActivity(day, list.size, list.count { it.outcome == GameOutcome.WON })
        }
        val daily = wins.mapNotNull { it.dailyDate }.toSet()
        val todayKey = DateKeys.keyFromEpochDay(todayEpochDay)
        val (current, best) = streaks(daily, todayEpochDay)
        return StatsSummary(
            totalGames = results.size,
            totalWins = wins.size,
            perGrid = perGrid,
            last7Days = last7,
            dailyCompleted = daily,
            currentStreak = current,
            bestStreak = best,
            todayDailyDone = todayKey in daily,
        )
    }

    /**
     * Streak harian: jumlah hari berturut-turut dengan Puzzle Harian selesai. Streak saat ini
     * dihitung mundur dari hari ini (atau kemarin bila hari ini belum dimainkan); terputus
     * bila ada hari yang terlewat.
     */
    fun streaks(dailyKeys: Set<Int>, todayEpochDay: Long): Pair<Int, Int> {
        if (dailyKeys.isEmpty()) return 0 to 0
        val days = dailyKeys.map { DateKeys.epochDayFromKey(it) }.toSortedSet()
        var best = 0
        var run = 0
        var prev: Long? = null
        for (d in days) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        var anchor = if (todayEpochDay in days) todayEpochDay else todayEpochDay - 1
        var current = 0
        while (anchor in days) {
            current++
            anchor--
        }
        return current to best
    }
}
