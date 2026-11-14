package id.rosdie.puzzle.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.rosdie.puzzle.MainActivity
import id.rosdie.puzzle.R
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolverResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h860dp-port-xhdpi")
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun waitForText(text: String, timeout: Long = 10_000) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeout)

    @Test
    fun homeShowsHonestEmptyStateAndEveryScreenOpens() {
        waitForText(s(R.string.home_tagline))
        waitForText(s(R.string.no_games_yet))
        rule.onNodeWithText(s(R.string.no_games_yet)).performScrollTo().assertExists()
        rule.screenshot("01_home")

        rule.onNodeWithText(s(R.string.nav_stats)).performScrollTo().performClick()
        waitForText(s(R.string.stats_summary))
        rule.screenshot("02_stats_empty")
        rule.onNodeWithContentDescription(s(R.string.action_back)).performClick()

        rule.onNodeWithText(s(R.string.nav_howto)).performScrollTo().performClick()
        waitForText(s(R.string.howto_goal_title))
        rule.screenshot("03_howto")
        rule.onNodeWithText(s(R.string.action_next)).performClick()
        waitForText(s(R.string.howto_move_title))
        rule.onNodeWithContentDescription(s(R.string.action_back)).performClick()

        rule.onNodeWithText(s(R.string.nav_settings)).performScrollTo().performClick()
        waitForText(s(R.string.settings_game))
        rule.screenshot("04_settings")
        rule.onNodeWithContentDescription(s(R.string.action_back)).performClick()

        rule.onNodeWithText(s(R.string.nav_gallery)).performScrollTo().performClick()
        waitForText(s(R.string.gallery_builtin))
        rule.screenshot("05_gallery")
        rule.onNodeWithContentDescription(s(R.string.action_back)).performClick()
        waitForText(s(R.string.home_tagline))
    }

    @Test
    fun play3x3ToWinByTappingTilesThenStatsShowRealRecord() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        waitForText(s(R.string.setup_title))
        rule.onNodeWithText(s(R.string.difficulty_easy)).performClick()
        rule.screenshot("06_setup_sheet")
        rule.onNodeWithText(s(R.string.action_start)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, 3, 3)), 10_000)
        rule.waitForIdle()
        rule.screenshot("07_game_3x3")

        val prefix = s(R.string.a11y_tile, 1, 1, 1).substringBefore("1")
        var board = rule.readBoard(3, prefix)
        assertTrue(PuzzleEngine.isSolvable(board))
        val solution = runBlocking { PuzzleSolver(Dispatchers.Default).solve(board) } as SolverResult.Solved
        for (d in solution.moves) {
            val idx = PuzzleEngine.tileIndexFor(board, d)
            val v = board[idx]
            rule.onNodeWithContentDescription(s(R.string.a11y_tile, v, idx / 3 + 1, idx % 3 + 1))
                .performClick()
            rule.waitForIdle()
            board = PuzzleEngine.move(board, d)!!
            if (board.isSolved) break
            check(rule.readBoard(3, prefix) == board) { "papan UI tidak sesuai harapan" }
        }
        waitForText(s(R.string.win_title))
        rule.onNodeWithText(s(R.string.win_new_record)).assertExists()
        rule.mainClock.advanceTimeBy(1_500)
        rule.screenshot("08_win_dialog")
        rule.onNodeWithText(s(R.string.action_home)).performScrollTo().performClick()
        waitForText(s(R.string.home_total_solved))
        rule.onNodeWithText(s(R.string.no_games_yet)).assertDoesNotExist()
        rule.screenshot("09_home_with_stats")
    }

    @Test
    fun aiSolves4x4AndPanelShowsHonestLabel() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.home_daily)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, 4, 4)), 10_000)
        rule.screenshot("10_daily_4x4")
        rule.onNodeWithText(s(R.string.action_solve_ai)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.ai_done_title)), 120_000)
        rule.screenshot("11_ai_done")
        rule.onNodeWithText(s(R.string.ai_done_optimal)).assertExists()
    }
}
