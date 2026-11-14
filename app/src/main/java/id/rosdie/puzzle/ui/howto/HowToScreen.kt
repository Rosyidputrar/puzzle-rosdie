package id.rosdie.puzzle.ui.howto

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.model.StarRating
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.BackTopBar
import id.rosdie.puzzle.ui.components.PuzzleBoard
import id.rosdie.puzzle.ui.components.StarRow
import id.rosdie.puzzle.ui.components.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Gesture { NONE, TAP, SWIPE, DRAG }

private data class Page(
    val title: Int,
    val body: Int,
    val demo: @Composable () -> Unit,
)

@Composable
fun HowToScreen(onBack: () -> Unit, onTryNow: (GameConfig) -> Unit) {
    val pages = remember {
        listOf(
            Page(R.string.howto_goal_title, R.string.howto_goal_body) { GoalDemo() },
            Page(R.string.howto_move_title, R.string.howto_move_body) { MoveDemo() },
            Page(R.string.howto_ai_title, R.string.howto_ai_body) { AiDemo() },
            Page(R.string.howto_first_row_title, R.string.howto_first_row_body) { FirstRowDemo() },
            Page(R.string.howto_corner_title, R.string.howto_corner_body) { CornerDemo() },
            Page(R.string.howto_pattern_title, R.string.howto_pattern_body) { PatternDemo() },
            Page(R.string.howto_scoring_title, R.string.howto_scoring_body) { ScoringTable() },
        )
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { BackTopBar(stringResource(R.string.nav_howto), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.howto_step, index + 1, pages.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(page.title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.widthIn(max = 320.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (pager.currentPage == index) page.demo()
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(max = 560.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (i == pager.currentPage) 10.dp else 8.dp)
                            .background(
                                if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            ),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_previous)) }
                if (pager.currentPage < pages.size - 1) {
                    Button(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_next)) }
                } else {
                    Button(
                        onClick = { onTryNow(GameConfig(3, GameMode.NUMBERS, null, ChallengeType.NORMAL, null)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(AppIcons.Play, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.howto_try_now))
                    }
                }
            }
        }
    }
}

/**
 * Papan demo: memutar skrip gerakan berulang-ulang, dengan indikator jari opsional dan
 * sorotan sel. Memakai PuzzleBoard yang sama dengan permainan.
 */
@Composable
private fun DemoBoard(
    start: PuzzleState,
    script: List<Direction>,
    gesture: (Int) -> Gesture = { Gesture.NONE },
    highlight: Set<Int> = emptySet(),
    stepMs: Long = 650,
    label: (Int) -> Int? = { null },
) {
    var board by remember { mutableStateOf(start) }
    var step by remember { mutableIntStateOf(0) }
    val finger = remember { Animatable(Offset(-1f, -1f), Offset.VectorConverter) }
    val fingerAlpha = remember { Animatable(0f) }
    val reduceMotion = rememberReducedMotion()
    LaunchedEffect(start, script) {
        while (true) {
            board = start
            delay(700)
            for ((i, d) in script.withIndex()) {
                step = i
                val n = board.size
                val tile = PuzzleEngine.tileIndexFor(board, d)
                val g = gesture(i)
                if (g != Gesture.NONE && !reduceMotion) {
                    val center = Offset((tile % n + 0.5f) / n, (tile / n + 0.5f) / n)
                    finger.snapTo(center)
                    fingerAlpha.animateTo(0.85f, tween(150))
                    if (g == Gesture.SWIPE || g == Gesture.DRAG) {
                        launch { finger.animateTo(center + Offset(d.dCol.toFloat() / n, d.dRow.toFloat() / n), tween(if (g == Gesture.DRAG) 420 else 220)) }
                        delay(if (g == Gesture.DRAG) 300 else 120)
                    } else {
                        delay(160)
                    }
                }
                board = PuzzleEngine.move(board, d)!!
                if (g != Gesture.NONE && !reduceMotion) launch { fingerAlpha.animateTo(0f, tween(250)) }
                delay(stepMs)
            }
            delay(1200)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        label(step)?.let { AssistChip(onClick = {}, label = { Text(stringResource(it)) }, leadingIcon = { Icon(AppIcons.TouchApp, null, Modifier.size(18.dp)) }) }
        val desc = stringResource(R.string.a11y_demo_board)
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clearAndSetSemantics { contentDescription = desc }) {
            PuzzleBoard(board = board, interactive = false, modifier = Modifier.fillMaxSize())
            val accent = MaterialTheme.colorScheme.tertiary
            val fingerColor = MaterialTheme.colorScheme.inverseSurface
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                val n = board.size
                val cell = size.width / n
                for (h in highlight) {
                    drawRoundRect(
                        accent,
                        topLeft = Offset((h % n) * cell, (h / n) * cell),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.18f),
                        style = Stroke(4.dp.toPx()),
                    )
                }
                if (fingerAlpha.value > 0f) {
                    drawCircle(
                        fingerColor.copy(alpha = fingerAlpha.value * 0.5f),
                        radius = cell * 0.28f,
                        center = Offset(finger.value.x * size.width, finger.value.y * size.height),
                    )
                }
            }
        }
    }
}

private fun walkFromSolved(size: Int, moves: List<Direction>): Pair<PuzzleState, List<Direction>> {
    val start = PuzzleEngine.applyAll(PuzzleState.solved(size), moves)
    return start to moves.reversed().map { it.opposite }
}

@Composable
private fun GoalDemo() {
    val (start, script) = remember {
        walkFromSolved(3, listOf(Direction.DOWN, Direction.RIGHT, Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT))
    }
    DemoBoard(start, script)
}

@Composable
private fun MoveDemo() {
    val (start, script) = remember { walkFromSolved(3, listOf(Direction.RIGHT, Direction.DOWN, Direction.LEFT)) }
    val gestures = listOf(Gesture.TAP, Gesture.SWIPE, Gesture.DRAG)
    DemoBoard(
        start,
        script,
        gesture = { gestures[it % gestures.size] },
        stepMs = 900,
        label = {
            when (gestures[it % gestures.size]) {
                Gesture.TAP -> R.string.howto_gesture_tap
                Gesture.SWIPE -> R.string.howto_gesture_swipe
                else -> R.string.howto_gesture_drag
            }
        },
    )
}

@Composable
private fun AiDemo() {
    val (start, script) = remember {
        walkFromSolved(
            3,
            listOf(Direction.DOWN, Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.UP, Direction.LEFT),
        )
    }
    DemoBoard(start, script, stepMs = 380, label = { R.string.howto_ai_chip })
}

@Composable
private fun FirstRowDemo() {
    val (start, script) = remember {
        walkFromSolved(4, listOf(Direction.DOWN, Direction.DOWN, Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP))
    }
    DemoBoard(start, script, highlight = setOf(0, 1, 2, 3), stepMs = 600)
}

@Composable
private fun CornerDemo() {
    val (start, script) = remember {
        walkFromSolved(4, listOf(Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.RIGHT, Direction.UP, Direction.LEFT, Direction.DOWN))
    }
    DemoBoard(start, script, highlight = setOf(2, 3), stepMs = 600)
}

@Composable
private fun PatternDemo() {
    // Pola siklus 2x2: memutar tiga tile mengelilingi kotak kosong.
    val cycle = listOf(Direction.DOWN, Direction.RIGHT, Direction.UP, Direction.LEFT)
    val (start, script) = remember { walkFromSolved(3, cycle + cycle) }
    DemoBoard(start, script, highlight = setOf(4, 5, 7, 8), stepMs = 450)
}

@Composable
private fun ScoringTable() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (size in 3..5) {
                val (three, two) = StarRating.thresholdsFor(size)
                Text(stringResource(R.string.grid_label, size, size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                ScoreRow(3, stringResource(R.string.howto_score_max, pluralStringResource(R.plurals.moves_count, three, three)))
                ScoreRow(2, stringResource(R.string.howto_score_max, pluralStringResource(R.plurals.moves_count, two, two)))
                ScoreRow(1, stringResource(R.string.howto_score_more, pluralStringResource(R.plurals.moves_count, two, two)))
            }
        }
    }
}

@Composable
private fun ScoreRow(stars: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarRow(stars, size = 18)
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
