package id.rosdie.puzzle.ui.game

import id.rosdie.puzzle.ui.components.Tag
import id.rosdie.puzzle.ui.components.Tag
import id.rosdie.puzzle.ui.components.SecondaryButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.rosdie.puzzle.R
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.PrimaryButton
import id.rosdie.puzzle.ui.components.ResultDialog
import id.rosdie.puzzle.ui.components.ResultStat
import id.rosdie.puzzle.ui.components.StarRow
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.formatDecimal
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.stats.AchievementUi

@Composable
fun WinDialog(
    win: WinSummary,
    celebrate: Boolean,
    onPlayAgain: () -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    onHome: () -> Unit,
) {
    ResultDialog(
        icon = AppIcons.Trophy,
        iconTint = MaterialTheme.colorScheme.onTertiary,
        iconBackground = MaterialTheme.colorScheme.tertiary,
        title = stringResource(if (win.isDaily) R.string.win_title_daily else R.string.win_title),
        subtitle = stringResource(R.string.win_subtitle, stringResource(R.string.grid_label, win.gridSize, win.gridSize)),
        celebrate = celebrate,
    ) {
        StarRow(win.stars, size = 44)
        Spacer(Modifier.height(16.dp))
        val none = stringResource(R.string.value_none)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultStat(stringResource(R.string.stat_moves), win.moves.toString(), Modifier.weight(1f))
            ResultStat(stringResource(R.string.stat_time), formatClock(win.timeMs), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultStat(stringResource(R.string.stat_moves_per_minute), win.movesPerMinute?.let { formatDecimal(it) } ?: none, Modifier.weight(1f))
            ResultStat(stringResource(R.string.stat_seconds_per_move), win.secondsPerMove?.let { formatDecimal(it, 2) } ?: none, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        RecordSection(win)
        if (win.newAchievements.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.win_new_achievements), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                win.newAchievements.forEach { a ->
                    val ui = AchievementUi.of(a)
                    Tag(stringResource(ui.title), Modifier.padding(vertical = 4.dp), icon = ui.icon)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(if (win.isDaily) R.string.action_new_puzzle else R.string.action_play_again),
            icon = AppIcons.Shuffle,
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(stringResource(R.string.action_retry), onRetry, Modifier.weight(1f), icon = AppIcons.Reset)
            SecondaryButton(stringResource(R.string.action_share), onShare, Modifier.weight(1f), icon = AppIcons.Share)
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.action_home), onHome, Modifier.fillMaxWidth(), icon = AppIcons.Home)
    }
}

@Composable
private fun RecordSection(win: WinSummary) {
    val c = MaterialTheme.colorScheme
    if (!win.eligibleForRecord) {
        Text(stringResource(R.string.win_not_eligible), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (win.newMovesRecord || win.newTimeRecord) {
            Tag(stringResource(R.string.win_new_record), accent = true, icon = AppIcons.Star)
            Spacer(Modifier.height(8.dp))
        }
        val prev = win.previous
        if (prev == null) {
            Text(stringResource(R.string.win_first_record), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        } else {
            Text(stringResource(R.string.win_record_moves, prev.bestMoves), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.win_record_time, formatSecondsClock(prev.bestTimeSeconds)), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
