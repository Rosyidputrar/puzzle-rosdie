package id.rosdie.puzzle.ui.stats

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.ui.components.AppIcons

data class AchievementUi(@param:StringRes val title: Int, @param:StringRes val description: Int, val icon: ImageVector) {
    companion object {
        fun of(a: Achievement): AchievementUi = when (a) {
            Achievement.FIRST_WIN_3X3 -> AchievementUi(R.string.ach_first_win_3x3, R.string.ach_first_win_3x3_desc, AppIcons.Flag)
            Achievement.WIN_5X5 -> AchievementUi(R.string.ach_win_5x5, R.string.ach_win_5x5_desc, AppIcons.Grid)
            Achievement.FAST_4X4 -> AchievementUi(R.string.ach_fast_4x4, R.string.ach_fast_4x4_desc, AppIcons.Bolt)
            Achievement.STREAK_7 -> AchievementUi(R.string.ach_streak_7, R.string.ach_streak_7_desc, AppIcons.Fire)
            Achievement.FIRST_CUSTOM_IMAGE -> AchievementUi(R.string.ach_custom_image, R.string.ach_custom_image_desc, AppIcons.AddPhoto)
            Achievement.PURE_WIN -> AchievementUi(R.string.ach_pure_win, R.string.ach_pure_win_desc, AppIcons.Check)
            Achievement.FIRST_DAILY -> AchievementUi(R.string.ach_first_daily, R.string.ach_first_daily_desc, AppIcons.Calendar)
            Achievement.TEN_WINS -> AchievementUi(R.string.ach_ten_wins, R.string.ach_ten_wins_desc, AppIcons.Trophy)
            Achievement.IMAGE_WIN -> AchievementUi(R.string.ach_image_win, R.string.ach_image_win_desc, AppIcons.Image)
            Achievement.CHALLENGE_WIN -> AchievementUi(R.string.ach_challenge_win, R.string.ach_challenge_win_desc, AppIcons.Timer)
            Achievement.SPEED_3X3 -> AchievementUi(R.string.ach_speed_3x3, R.string.ach_speed_3x3_desc, AppIcons.Speed)
        }
    }
}
