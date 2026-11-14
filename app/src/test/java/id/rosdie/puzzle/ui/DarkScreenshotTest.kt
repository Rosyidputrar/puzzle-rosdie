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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tangkapan layar tema gelap untuk perbandingan before/after perbaikan audit UI/UX.
 * Semua tangkapan memakai locale default (Indonesia) dan ponsel portrait 392x860,
 * supaya sama persis dengan kondisi pengguna.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h860dp-port-night-xhdpi")
class DarkScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun waitForText(text: String, timeout: Long = 10_000) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeout)

    private fun back() {
        rule.onNodeWithContentDescription(s(R.string.action_back)).performClick()
    }

    @Test
    fun darkHome() {
        waitForText(s(R.string.home_tagline))
        rule.waitForIdle()
        rule.screenshot("20d_home_dark")
    }

    @Test
    fun darkStats() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.nav_stats)).performScrollTo().performClick()
        waitForText(s(R.string.stats_summary))
        rule.waitForIdle()
        rule.screenshot("21d_stats_dark")
    }

    @Test
    fun darkHowto() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.nav_howto)).performScrollTo().performClick()
        waitForText(s(R.string.howto_goal_title))
        rule.waitForIdle()
        rule.screenshot("22d_howto_dark")
    }

    @Test
    fun darkSettings() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.nav_settings)).performScrollTo().performClick()
        waitForText(s(R.string.settings_game))
        rule.waitForIdle()
        rule.screenshot("23d_settings_dark")
    }

    @Test
    fun darkGallery() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.nav_gallery)).performScrollTo().performClick()
        waitForText(s(R.string.gallery_builtin))
        rule.waitForIdle()
        rule.screenshot("24d_gallery_dark")
    }

    @Test
    fun darkSetupSheet() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        waitForText(s(R.string.setup_title))
        rule.waitForIdle()
        rule.screenshot("25d_setup_dark")
    }

    @Test
    fun darkGame3x3() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        waitForText(s(R.string.setup_title))
        rule.onNodeWithText(s(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(s(R.string.action_start)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, 3, 3)), 10_000)
        rule.waitForIdle()
        rule.screenshot("26d_game_dark")
    }

    /** Dock kontrol di layar sempit 320dp: label tidak boleh terpotong. */
    @Test
    @Config(qualifiers = "w320dp-h568dp-port-xhdpi")
    fun narrowGameDock() {
        waitForText(s(R.string.home_tagline))
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        waitForText(s(R.string.setup_title))
        rule.onNodeWithText(s(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(s(R.string.action_start)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, 3, 3)), 10_000)
        rule.waitForIdle()
        rule.screenshot("27_narrow_dock")
    }
}
