package id.rosdie.puzzle.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

/** Tangkapan layar landscape, tema gelap, dan bahasa Inggris untuk tinjauan visual. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotVariantsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun openGame(size: Int, label: Int) {
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.home_tagline)), 10_000)
        rule.onNodeWithText(s(R.string.action_play)).performClick()
        rule.waitUntilAtLeastOneExists(hasText(s(R.string.setup_title)), 10_000)
        rule.onNodeWithText(s(label)).performClick()
        rule.onNodeWithText(s(R.string.mode_image)).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.action_start)).performScrollTo().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription(s(R.string.a11y_board, size, size)), 10_000)
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w860dp-h392dp-land-xhdpi")
    fun landscapeImageMode5x5() {
        openGame(5, R.string.difficulty_hard)
        rule.screenshot("20_landscape_5x5_image")
    }

    @Test
    @Config(qualifiers = "en-w392dp-h860dp-port-night-xhdpi")
    fun darkEnglishImageMode4x4() {
        openGame(4, R.string.difficulty_normal)
        rule.screenshot("21_dark_en_4x4_image")
    }
}
