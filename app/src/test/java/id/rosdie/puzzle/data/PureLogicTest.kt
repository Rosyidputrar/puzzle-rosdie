package id.rosdie.puzzle.data

import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.AchievementRules
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.StatsCalculator
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult
import id.rosdie.puzzle.domain.model.StarRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PureLogicTest {
    private fun result(
        size: Int = 4,
        moves: Int = 120,
        time: Long = 90,
        outcome: GameOutcome = GameOutcome.WON,
        ai: Boolean = false,
        hint: Boolean = false,
        daily: Int? = null,
        mode: GameMode = GameMode.NUMBERS,
        challenge: ChallengeType = ChallengeType.NORMAL,
        at: Long = 0,
    ) = GameResult(0, size, mode, moves, time, ai, hint, StarRating.stars(size, moves), challenge, daily, outcome, at)

    @Test
    fun dateKeysRoundTripAgainstCalendar() {
        val utc = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(utc)
        var day = DateKeys.daysFromCivil(1999, 12, 25)
        repeat(12_000) {
            cal.timeInMillis = day * 86_400_000L
            val key = cal.get(Calendar.YEAR) * 10_000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
            assertEquals(key, DateKeys.keyFromEpochDay(day))
            assertEquals(day, DateKeys.epochDayFromKey(key))
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Senin = 0
            assertEquals(dow, DateKeys.dayOfWeekMondayFirst(day))
            day += 1
        }
        assertEquals(29, DateKeys.daysInMonth(2028, 2))
        assertEquals(28, DateKeys.daysInMonth(2027, 2))
        assertEquals(20261114, DateKeys.keyOf(DateKeys.epochDayFromKey(20261114) * 86_400_000L + 1, utc))
    }

    @Test
    fun starThresholdsMatchSpec() {
        assertEquals(3, StarRating.stars(3, 20)); assertEquals(2, StarRating.stars(3, 21)); assertEquals(2, StarRating.stars(3, 40)); assertEquals(1, StarRating.stars(3, 41))
        assertEquals(3, StarRating.stars(4, 200)); assertEquals(2, StarRating.stars(4, 400)); assertEquals(1, StarRating.stars(4, 401))
        assertEquals(3, StarRating.stars(5, 500)); assertEquals(2, StarRating.stars(5, 800)); assertEquals(1, StarRating.stars(5, 801))
    }

    @Test
    fun streakCountsConsecutiveDaysAndBreaksOnGap() {
        val today = DateKeys.epochDayFromKey(20261114)
        fun k(d: Long) = DateKeys.keyFromEpochDay(d)
        // Hari ini belum main: streak masih hidup dari kemarin.
        assertEquals(3 to 3, StatsCalculator.streaks(setOf(k(today - 1), k(today - 2), k(today - 3)), today))
        assertEquals(4 to 4, StatsCalculator.streaks(setOf(k(today), k(today - 1), k(today - 2), k(today - 3)), today))
        // Terlewat kemarin: streak putus.
        assertEquals(0 to 2, StatsCalculator.streaks(setOf(k(today - 2), k(today - 3)), today))
        assertEquals(1 to 5, StatsCalculator.streaks(setOf(k(today)) + (10L..14L).map { k(today - it) }, today))
        assertEquals(0 to 0, StatsCalculator.streaks(emptySet(), today))
    }

    @Test
    fun statsUseOnlyRealDataAndRecordsExcludeAiAndHints() {
        val today = 20_000L
        val empty = StatsCalculator.compute(emptyList(), today) { it / 86_400_000L }
        assertFalse(empty.hasGames)
        assertNull(empty.winRate)
        assertTrue(empty.perGrid.all { it.record == null && it.averageMoves == null })

        val day = today * 86_400_000L
        val results = listOf(
            result(moves = 150, time = 100, at = day),
            result(moves = 90, time = 200, hint = true, at = day),
            result(moves = 80, time = 50, ai = true, outcome = GameOutcome.AI_SOLVED, at = day),
            result(moves = 30, time = 20, outcome = GameOutcome.ABANDONED, at = day - 86_400_000L),
            result(size = 3, moves = 22, time = 15, at = day),
        )
        val s = StatsCalculator.compute(results, today) { it / 86_400_000L }
        assertEquals(5, s.totalGames)
        assertEquals(3, s.totalWins)
        assertEquals(0.6, s.winRate!!, 1e-9)
        val g4 = s.perGrid.first { it.gridSize == 4 }
        assertEquals(150, g4.record!!.bestMoves) // hint dan AI tidak masuk rekor
        assertEquals(100L, g4.record.bestTimeSeconds)
        assertEquals(120.0, g4.averageMoves!!, 1e-9) // rata-rata kemenangan (150, 90)
        assertEquals(4, s.last7Days.last().games)
        assertEquals(3, s.last7Days.last().wins)
        assertEquals(1, s.last7Days[5].games)
    }

    @Test
    fun achievementRules() {
        assertTrue(AchievementRules.earned(emptyList(), 0, 0).isEmpty())
        val earned = AchievementRules.earned(
            listOf(
                result(size = 3, moves = 18, time = 25),
                result(size = 4, moves = 95, hint = true),
                result(size = 5, moves = 300, daily = 20261114, mode = GameMode.IMAGE, challenge = ChallengeType.TIME_ATTACK),
            ),
            customImagesAdded = 1,
            bestStreak = 7,
        )
        assertEquals(
            setOf(
                Achievement.FIRST_WIN_3X3, Achievement.WIN_5X5, Achievement.FAST_4X4, Achievement.STREAK_7,
                Achievement.FIRST_CUSTOM_IMAGE, Achievement.PURE_WIN, Achievement.FIRST_DAILY, Achievement.IMAGE_WIN,
                Achievement.CHALLENGE_WIN, Achievement.SPEED_3X3,
            ),
            earned,
        )
        // AI_SOLVED tidak dihitung menang.
        assertTrue(AchievementRules.earned(listOf(result(size = 3, outcome = GameOutcome.AI_SOLVED, ai = true)), 0, 0).isEmpty())
        assertTrue(Achievement.TEN_WINS in AchievementRules.earned(List(10) { result() }, 0, 0))
    }
}
