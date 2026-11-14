package id.rosdie.puzzle.data.repository

import id.rosdie.puzzle.data.local.AchievementDao
import id.rosdie.puzzle.data.local.AchievementEntity
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Daftar pencapaian. Judul, deskripsi, dan ikon dipetakan di lapisan UI (AchievementUi). */
enum class Achievement(val id: String) {
    FIRST_WIN_3X3("first_win_3x3"),
    WIN_5X5("win_5x5"),
    FAST_4X4("fast_4x4"),
    STREAK_7("streak_7"),
    FIRST_CUSTOM_IMAGE("first_custom_image"),
    PURE_WIN("pure_win"),
    FIRST_DAILY("first_daily"),
    TEN_WINS("ten_wins"),
    IMAGE_WIN("image_win"),
    CHALLENGE_WIN("challenge_win"),
    SPEED_3X3("speed_3x3");

    companion object {
        fun byId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

object AchievementRules {
    const val FAST_4X4_MOVES = 100
    const val SPEED_3X3_SECONDS = 30L
    const val STREAK_DAYS = 7
    const val TOTAL_WINS = 10

    fun earned(results: List<GameResult>, customImagesAdded: Int, bestStreak: Int): Set<Achievement> {
        val wins = results.filter { it.outcome == GameOutcome.WON }
        val out = mutableSetOf<Achievement>()
        if (wins.any { it.gridSize == 3 }) out += Achievement.FIRST_WIN_3X3
        if (wins.any { it.gridSize == 5 }) out += Achievement.WIN_5X5
        if (wins.any { it.gridSize == 4 && it.moves < FAST_4X4_MOVES }) out += Achievement.FAST_4X4
        if (bestStreak >= STREAK_DAYS) out += Achievement.STREAK_7
        if (customImagesAdded > 0) out += Achievement.FIRST_CUSTOM_IMAGE
        if (wins.any { !it.usedAi && !it.usedHint }) out += Achievement.PURE_WIN
        if (wins.any { it.dailyDate != null }) out += Achievement.FIRST_DAILY
        if (wins.size >= TOTAL_WINS) out += Achievement.TEN_WINS
        if (wins.any { it.mode == GameMode.IMAGE }) out += Achievement.IMAGE_WIN
        if (wins.any { it.challengeType != ChallengeType.NORMAL }) out += Achievement.CHALLENGE_WIN
        if (wins.any { it.gridSize == 3 && it.timeSeconds <= SPEED_3X3_SECONDS }) out += Achievement.SPEED_3X3
        return out
    }
}

class AchievementRepository(private val dao: AchievementDao, private val time: TimeSource) {
    /** id pencapaian -> waktu buka (epoch ms). */
    val unlocked: Flow<Map<Achievement, Long>> = dao.observeAll().map { list ->
        list.mapNotNull { e -> Achievement.byId(e.id)?.let { it to e.unlockedAt } }.toMap()
    }

    /** Buka pencapaian yang baru terpenuhi. Mengembalikan yang BARU dibuka saja. */
    suspend fun evaluate(results: List<GameResult>, customImagesAdded: Int): List<Achievement> {
        val daily = results.filter { it.outcome == GameOutcome.WON }.mapNotNull { it.dailyDate }.toSet()
        val (_, best) = StatsCalculator.streaks(daily, time.todayEpochDay())
        val already = dao.getAll().map { it.id }.toSet()
        val now = time.nowMs()
        return AchievementRules.earned(results, customImagesAdded, best)
            .filter { it.id !in already }
            .sortedBy { it.ordinal }
            .filter { dao.insert(AchievementEntity(it.id, now)) != -1L }
    }
}
