package id.rosdie.puzzle.ui.game

import id.rosdie.puzzle.ui.components.AppCard
import id.rosdie.puzzle.ui.components.SecondaryButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import id.rosdie.puzzle.ui.components.AppSlider
import id.rosdie.puzzle.ui.components.PrimaryButton
import id.rosdie.puzzle.ui.components.ThinBar
import id.rosdie.puzzle.ui.theme.TabularNumbers
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolverProgress
import id.rosdie.puzzle.domain.solver.SolverStrategy
import id.rosdie.puzzle.domain.solver.ThinkingPhase
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.formatNumber
import id.rosdie.puzzle.ui.components.rememberReducedMotion
import kotlin.math.roundToInt

@Composable
fun AiSpeedSlider(speedMs: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val label = stringResource(aiSpeedLabel(speedMs))
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Speed, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.ai_speed), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.ai_speed_value, label, speedMs), style = MaterialTheme.typography.labelLarge.merge(TabularNumbers))
        }
        AppSlider(
            value = speedMs.toFloat(),
            onValueChange = { onChange((it / 10f).roundToInt() * 10) },
            valueRange = AppSettings.MIN_AI_SPEED_MS.toFloat()..AppSettings.MAX_AI_SPEED_MS.toFloat(),
            enabled = enabled,
            modifier = Modifier.semantics { stateDescription = label },
        )
    }
}

fun aiSpeedLabel(ms: Int): Int = when {
    ms <= 100 -> R.string.ai_speed_very_fast
    ms <= 300 -> R.string.ai_speed_fast
    ms <= 600 -> R.string.ai_speed_normal
    else -> R.string.ai_speed_slow
}

fun strategyLabel(strategy: SolverStrategy): Int = when (strategy) {
    SolverStrategy.ASTAR_OPTIMAL -> R.string.strategy_astar
    SolverStrategy.IDA_STAR_OPTIMAL -> R.string.strategy_idastar
    SolverStrategy.WEIGHTED_ASTAR -> R.string.strategy_weighted
    SolverStrategy.STAGED -> R.string.strategy_staged
}

private fun phaseLabel(phase: ThinkingPhase, optimal: Boolean): Int = when (phase) {
    ThinkingPhase.ANALYZING -> R.string.phase_analyzing
    ThinkingPhase.SEARCHING -> if (optimal) R.string.phase_searching_optimal else R.string.phase_searching
    ThinkingPhase.DEEP_EXPLORATION -> R.string.phase_deep_exploration
    ThinkingPhase.DEEP_THINKING -> R.string.phase_deep_thinking
}

/** Panel AI: ajakan menyelesaikan, progres berpikir, anggaran habis, atau eksekusi solusi. */
@Composable
fun AiPanel(
    ai: AiUi,
    gridSize: Int,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    onQuick: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onSolve: () -> Unit,
    solveEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val d = if (reduceMotion) 0 else 200
    Column(modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = ai,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(d)) + scaleIn(tween(d), 0.98f))
                    .togetherWith(fadeOut(tween(d)) + scaleOut(tween(d), 0.98f))
                    .using(SizeTransform(clip = true) { _, _ -> tween(d) })
            },
            label = "ai",
        ) { target ->
            when (target) {
                is AiUi.Thinking -> ThinkingContent(target, gridSize, onCancel)
                is AiUi.BudgetExceeded -> BudgetContent(target.progress, onContinue, onQuick, onCancel)
                is AiUi.Executing -> ExecutingContent(target, onTogglePause, onStop)
                AiUi.Idle -> IdleContent(gridSize, onSolve, solveEnabled)
            }
        }
    }
}

@Composable
private fun IdleContent(gridSize: Int, onSolve: () -> Unit, solveEnabled: Boolean) {
    Column {
        PrimaryButton(
            text = stringResource(R.string.action_solve_ai),
            icon = null,
            accent = true,
            onClick = onSolve,
            enabled = solveEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (gridSize == 5) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ai_5x5_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThinkingContent(ai: AiUi.Thinking, gridSize: Int, onCancel: () -> Unit) {
    val p = ai.progress
    val strategy = p?.strategy ?: PuzzleSolver.strategyFor(gridSize, ai.mode)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(phaseLabel(p?.phase ?: ThinkingPhase.ANALYZING, strategy.optimal)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    stringResource(strategyLabel(strategy)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressGrid(p)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.action_cancel), onCancel, Modifier.fillMaxWidth(), icon = AppIcons.Close)
    }
}

@Composable
private fun ProgressGrid(p: SolverProgress?) {
    val calculating = stringResource(R.string.ai_calculating)
    val cells = listOf(
        stringResource(R.string.ai_iterations) to (p?.let { formatNumber(it.iterations) } ?: calculating),
        stringResource(R.string.ai_thinking_time) to (p?.let { formatClock(it.elapsedMs) } ?: "0:00"),
        stringResource(R.string.ai_active_states) to (p?.let { formatNumber(it.activeStates) } ?: calculating),
        stringResource(R.string.ai_rate) to (p?.let { formatNumber(it.iterationsPerSecond) } ?: calculating),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (k, v) -> ProgressTile(k, v, Modifier.weight(1f)) }
            }
        }
        ProgressTile(
            stringResource(R.string.ai_eta),
            p?.etaMs?.let { stringResource(R.string.ai_eta_value, formatClock(it)) } ?: calculating,
            Modifier.fillMaxWidth(),
        )
        p?.bound?.let { bound ->
            Text(
                stringResource(R.string.ai_bound, pluralStringResource(R.plurals.moves_count, bound, bound)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressTile(label: String, value: String, modifier: Modifier) {
    AppCard(modifier, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).semantics(mergeDescendants = true) {}) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall.merge(TabularNumbers), maxLines = 1)
        }
    }
}

@Composable
private fun BudgetContent(p: SolverProgress, onContinue: () -> Unit, onQuick: () -> Unit, onCancel: () -> Unit) {
    Column {
        Text(stringResource(R.string.ai_budget_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ai_budget_body, formatClock(p.elapsedMs), formatNumber(p.iterations)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.ai_quick_solve), null, onQuick, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(stringResource(R.string.ai_continue), onContinue, Modifier.weight(1f), icon = AppIcons.Play)
            SecondaryButton(stringResource(R.string.action_cancel), onCancel, Modifier.weight(1f), icon = AppIcons.Close)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.ai_quick_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ExecutingContent(ai: AiUi.Executing, onTogglePause: () -> Unit, onStop: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.ai_executing, ai.done, ai.total),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(if (ai.optimal) R.string.ai_optimal_label else R.string.ai_not_optimal_label) +
                " · " + stringResource(strategyLabel(ai.strategy)),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        ThinBar(
            progress = if (ai.total == 0) 1f else ai.done / ai.total.toFloat(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            height = 8.dp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                stringResource(if (ai.paused) R.string.action_resume else R.string.action_pause),
                onTogglePause,
                Modifier.weight(1f),
                icon = if (ai.paused) AppIcons.Play else AppIcons.Pause,
            )
            SecondaryButton(stringResource(R.string.action_stop), onStop, Modifier.weight(1f), icon = AppIcons.Stop)
        }
    }
}
