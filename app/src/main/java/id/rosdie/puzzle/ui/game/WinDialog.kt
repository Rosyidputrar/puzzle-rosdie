package id.rosdie.puzzle.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.rosdie.puzzle.R
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.Confetti
import id.rosdie.puzzle.ui.components.StarRow
import id.rosdie.puzzle.ui.components.StatValue
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.formatDecimal
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.components.rememberReducedMotion
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
    val reduceMotion = rememberReducedMotion()
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val c = MaterialTheme.colorScheme
            if (celebrate && !reduceMotion) {
                Confetti(
                    Modifier.fillMaxSize(),
                    colors = listOf(c.primary, c.tertiary, Color(0xFF60A5FA), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF22C55E)),
                )
            }
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val pop = remember { Animatable(if (reduceMotion) 1f else 0.4f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                    Surface(
                        shape = CircleShape,
                        color = c.tertiaryContainer,
                        modifier = Modifier
                            .size(84.dp)
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Trophy, null, Modifier.size(48.dp), tint = c.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(if (win.isDaily) R.string.win_title_daily else R.string.win_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text(
                        stringResource(R.string.win_subtitle, stringResource(R.string.grid_label, win.gridSize, win.gridSize)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    StarRow(win.stars, size = 40)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatValue(stringResource(R.string.stat_moves), win.moves.toString())
                        StatValue(stringResource(R.string.stat_time), formatClock(win.timeMs))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatValue(
                            stringResource(R.string.stat_moves_per_minute),
                            win.movesPerMinute?.let { formatDecimal(it) } ?: stringResource(R.string.value_none),
                        )
                        StatValue(
                            stringResource(R.string.stat_seconds_per_move),
                            win.secondsPerMove?.let { formatDecimal(it, 2) } ?: stringResource(R.string.value_none),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    RecordSection(win)
                    if (win.newAchievements.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.win_new_achievements), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        win.newAchievements.forEach { a ->
                            val ui = AchievementUi.of(a)
                            AssistChip(
                                onClick = {},
                                label = { Text(stringResource(ui.title)) },
                                leadingIcon = { Icon(ui.icon, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Shuffle, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(if (win.isDaily) R.string.action_new_puzzle else R.string.action_play_again))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_retry))
                        }
                        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                            Icon(AppIcons.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.action_share))
                        }
                    }
                    TextButton(onClick = onHome) { Text(stringResource(R.string.action_home)) }
                }
            }
        }
    }
}

@Composable
private fun RecordSection(win: WinSummary) {
    val c = MaterialTheme.colorScheme
    if (!win.eligibleForRecord) {
        Text(
            stringResource(R.string.win_not_eligible),
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    if (win.newMovesRecord || win.newTimeRecord) {
        Surface(color = c.primaryContainer, shape = MaterialTheme.shapes.medium) {
            Text(
                stringResource(R.string.win_new_record),
                style = MaterialTheme.typography.titleMedium,
                color = c.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    val prev = win.previous
    if (prev == null) {
        Text(stringResource(R.string.win_first_record), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    } else {
        Text(
            stringResource(R.string.win_record_moves, prev.bestMoves),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(R.string.win_record_time, formatSecondsClock(prev.bestTimeSeconds)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
