package id.rosdie.puzzle

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolverResult
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test layar utama dan satu permainan 3x3 sampai selesai, di perangkat/emulator. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun homeScreenShowsMainActions() {
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.home_tagline)), 10_000)
        rule.onNodeWithText(s(R.string.action_play)).assertExists()
        rule.onNodeWithText(s(R.string.home_daily)).performScrollTo().assertExists()
        rule.onNodeWithText(s(R.string.nav_settings)).performScrollTo().assertExists()
    }

    @Test
    fun play3x3UntilSolved() {
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.home_tagline)), 10_000)
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.setup_title)), 10_000)
        rule.onNodeWithText(s(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(s(R.string.action_start)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, 3, 3)), 10_000)
        rule.waitForIdle()

        val prefix = s(R.string.a11y_tile, 1, 1, 1).substringBefore("1")
        val cells = IntArray(9)
        rule.onAllNodes(SemanticsMatcher("tile") {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.startsWith(prefix) == true
        }, useUnmergedTree = true).fetchSemanticsNodes().forEach { n ->
            val nums = Regex("""\d+""").findAll(n.config[SemanticsProperties.ContentDescription].first()).map { it.value.toInt() }.toList()
            cells[(nums[1] - 1) * 3 + nums[2] - 1] = nums[0]
        }
        var board = PuzzleState.of(3, cells)
        val solution = runBlocking { PuzzleSolver().solve(board) } as SolverResult.Solved
        for (d in solution.moves) {
            val idx = PuzzleEngine.tileIndexFor(board, d)
            rule.onNodeWithContentDescription(s(R.string.a11y_tile, board[idx], idx / 3 + 1, idx % 3 + 1), useUnmergedTree = true)
                .performClick()
            rule.waitForIdle()
            board = PuzzleEngine.move(board, d)!!
        }
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.win_title)), 10_000)
    }
}
