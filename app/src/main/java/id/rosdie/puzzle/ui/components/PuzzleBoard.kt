package id.rosdie.puzzle.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import id.rosdie.puzzle.R
import id.rosdie.puzzle.ui.theme.NumberFont
import id.rosdie.puzzle.ui.theme.CardShape
import id.rosdie.puzzle.ui.theme.tileBlue
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.PuzzleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Papan sliding puzzle.
 *
 * Performa: setiap tile punya Animatable posisi sendiri yang dibaca di fase layout
 * (Modifier.offset { }), jadi animasi tidak memicu recomposition papan. Mode gambar memakai
 * SATU ImageBitmap yang digambar dengan offset sumber per tile (tanpa decode ulang).
 */
@Composable
fun PuzzleBoard(
    board: PuzzleState,
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
    interactive: Boolean = true,
    hidden: Boolean = false,
    hintIndex: Int? = null,
    stiffnessFactor: Float = 1f,
    showNumbersOnImage: Boolean = true,
    markCorrect: Boolean = false,
    onTap: (Int) -> Unit = {},
    onSwipe: (Direction) -> Unit = {},
    onDragCommit: (Int) -> Unit = {},
) {
    val n = board.size
    val reduceMotion = rememberReducedMotion()
    val colors = MaterialTheme.colorScheme
    val outerShape = CardShape
    val boardDescription = stringResource(R.string.a11y_board, n, n)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(outerShape)
            .background(colors.surfaceContainerHigh)
            .padding(6.dp)
            .semantics { contentDescription = boardDescription },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val cellPx = constraints.maxWidth.toFloat() / n
            val gapPx = with(density) { (if (n >= 5) 5.dp else 6.dp).toPx() }
            val tilePx = cellPx - gapPx
            val tileDp = with(density) { tilePx.toDp() }
            // Radius tile tetap 12 dp (sedikit lebih kecil di 5x5 dan pratinjau mini).
            val corner = minOf(12.dp, with(density) { (tilePx * 0.16f).toDp() })
            val tileShape = RoundedCornerShape(corner)
            // Ukuran angka proporsional dengan tile (juga di pratinjau kecil); skala font
            // sistem dibatasi 1.3x agar angka tidak meluber dari tile.
            val numberSize = cappedFontSize(tilePx * 0.40f, density)
            val badgeSize = with(density) { cappedFontSize((tilePx * 0.13f).coerceAtLeast(9.dp.toPx()), density) }

            fun offsetFor(index: Int) = Offset((index % n) * cellPx + gapPx / 2, (index / n) * cellPx + gapPx / 2)

            val positions = remember(board) { IntArray(n * n).also { for (i in 0 until n * n) it[board[i]] = i } }
            val anims = remember(n, cellPx) {
                Array(n * n) { v -> Animatable(offsetFor(positions[v]), Offset.VectorConverter) }
            }
            val scope = rememberCoroutineScope()
            var dragTile by remember { mutableIntStateOf(0) }
            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            val shakes = remember(n) { Array(n * n) { Animatable(0f) } }
            val moveSpec = spring<Offset>(dampingRatio = 0.74f, stiffness = Spring.StiffnessMedium * 0.6f * stiffnessFactor)

            val currentBoard by rememberUpdatedState(board)
            val currentInteractive by rememberUpdatedState(interactive)
            val tapState by rememberUpdatedState(onTap)
            val swipeState by rememberUpdatedState(onSwipe)
            val commitState by rememberUpdatedState(onDragCommit)

            // Animasikan setiap tile yang targetnya berubah. Scope animasi tidak ikut batal
            // saat papan berubah lagi, jadi gerakan beruntun tetap mulus.
            LaunchedEffect(positions, anims) {
                for (v in 1 until n * n) {
                    val target = offsetFor(positions[v])
                    val anim = anims[v]
                    if (dragTile == v) {
                        anim.snapTo(anim.value + dragOffset)
                        dragTile = 0
                        dragOffset = Offset.Zero
                    }
                    if (anim.value != target || anim.targetValue != target) {
                        if (reduceMotion) anim.snapTo(target) else scope.launch { anim.animateTo(target, moveSpec) }
                    }
                }
            }

            val slot = colors.onSurface
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (hidden) return@drawBehind
                        // Kotak kosong: border putus-putus + titik penanda.
                        val o = offsetFor(board.blankIndex)
                        val inset = 2.dp.toPx()
                        drawRoundRect(
                            color = slot.copy(alpha = 0.7f),
                            topLeft = o + Offset(inset, inset),
                            size = Size(tilePx - 2 * inset, tilePx - 2 * inset),
                            cornerRadius = CornerRadius(corner.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                            ),
                        )
                        drawCircle(slot.copy(alpha = 0.6f), radius = 3.5.dp.toPx(), center = o + Offset(tilePx / 2, tilePx / 2))
                    }
                    .pointerInput(n, cellPx) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!currentInteractive) return@awaitEachGesture
                            down.consume()
                            val b = currentBoard
                            val col = (down.position.x / cellPx).toInt().coerceIn(0, n - 1)
                            val row = (down.position.y / cellPx).toInt().coerceIn(0, n - 1)
                            val idx = row * n + col
                            val tile = b[idx]
                            val movable = PuzzleEngine.directionForTile(b, idx)
                            var total = Offset.Zero
                            var mode = 0 // 0 = belum jelas (tap), 1 = seret tile, 2 = swipe
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                tracker.addPosition(change.uptimeMillis, change.position)
                                if (!change.pressed) break
                                total += change.positionChange()
                                change.consume()
                                if (mode == 0 && total.getDistance() > slop) {
                                    mode = if (movable != null && dominant(total) == movable) 1 else 2
                                    if (tile != 0) dragTile = tile
                                }
                                if (dragTile == tile && tile != 0) {
                                    dragOffset = if (mode == 1 && movable != null) {
                                        val p = project(total, movable).coerceIn(0f, cellPx)
                                        Offset(movable.dCol * p, movable.dRow * p)
                                    } else {
                                        rubberBand(total, cellPx * 0.12f)
                                    }
                                }
                            }
                            when (mode) {
                                0 -> {
                                    if (tile != 0 && PuzzleEngine.lineMoves(b, idx).isEmpty() && !reduceMotion) {
                                        scope.launch {
                                            val s = shakes[tile]
                                            s.snapTo(0f)
                                            s.animateTo(0f, keyframes {
                                                durationMillis = 260
                                                7f at 40; -7f at 100; 4f at 160; -3f at 210
                                            })
                                        }
                                    }
                                    tapState(idx)
                                }
                                1 -> {
                                    val m = movable!!
                                    val v = tracker.calculateVelocity()
                                    val progress = project(total, m)
                                    val velocity = v.x * m.dCol + v.y * m.dRow
                                    val commit = progress > cellPx * 0.42f || (velocity > 700f && progress > cellPx * 0.1f)
                                    if (commit) {
                                        commitState(idx)
                                        // Cadangan bila papan tidak berubah (mis. input ditolak).
                                        scope.launch {
                                            delay(300)
                                            if (dragTile == tile) animateBack(anims[tile], offsetFor(idx), dragOffset, moveSpec) {
                                                dragTile = 0; dragOffset = Offset.Zero
                                            }
                                        }
                                    } else {
                                        val from = dragOffset
                                        scope.launch {
                                            animateBack(anims[tile], offsetFor(idx), from, moveSpec) {
                                                dragTile = 0; dragOffset = Offset.Zero
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    if (tile != 0 && dragTile == tile) {
                                        val from = dragOffset
                                        scope.launch {
                                            animateBack(anims[tile], offsetFor(idx), from, moveSpec) {
                                                dragTile = 0; dragOffset = Offset.Zero
                                            }
                                        }
                                    }
                                    if (total.getDistance() > slop * 1.5f) swipeState(dominant(total))
                                }
                            }
                        }
                    },
            ) {
                if (!hidden) {
                    val hintTile = hintIndex?.let { board[it] }?.takeIf { it != 0 }
                    for (v in 1 until n * n) {
                        val index = positions[v]
                        val anim = anims[v]
                        val moving by remember(anim) { derivedStateOf { anim.isRunning || dragTile == v } }
                        val lift by animateFloatAsState(
                            targetValue = if (moving && !reduceMotion) 1f else 0f,
                            animationSpec = tween(if (moving) 70 else 160),
                            label = "lift",
                        )
                        val canMove = PuzzleEngine.lineMoves(board, index).isNotEmpty()
                        val desc = stringResource(R.string.a11y_tile, v, index / n + 1, index % n + 1)
                        val glow = colors.primary
                        val correct = index == v - 1
                        val correctLabel = stringResource(R.string.a11y_tile_correct)
                        val moveLabel = stringResource(R.string.a11y_move_tile)
                        Box(
                            Modifier
                                .zIndex(if (moving) 1f else 0f)
                                .offset {
                                    val p = anim.value + if (dragTile == v) dragOffset else Offset.Zero
                                    IntOffset(p.x.roundToInt(), p.y.roundToInt())
                                }
                                .size(tileDp)
                                .graphicsLayer {
                                    val s = 1f + 0.045f * lift
                                    scaleX = s
                                    scaleY = s
                                    // Hanya tile yang sedang bergerak terangkat dan berpendar biru;
                                    // tile diam datar.
                                    shadowElevation = (12f * lift).dp.toPx()
                                    ambientShadowColor = glow
                                    spotShadowColor = glow
                                    shape = tileShape
                                    clip = true
                                    translationX = shakes[v].value.dp.toPx()
                                }
                                .clearAndSetSemantics {
                                    contentDescription = desc
                                    if (correct) stateDescription = correctLabel
                                    if (canMove && interactive) {
                                        onClick(label = moveLabel) { tapState(index); true }
                                        customActions = listOf(CustomAccessibilityAction(moveLabel) { tapState(index); true })
                                    }
                                },
                        ) {
                            if (image != null) {
                                ImageTile(image, v, n, showNumbersOnImage && tileDp >= 40.dp, badgeSize)
                            } else {
                                NumberTile(v, numberSize, markCorrect && correct, tileDp)
                            }
                            if (v == hintTile) HintGlow(tileShape)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun animateBack(
    anim: Animatable<Offset, *>,
    base: Offset,
    from: Offset,
    spec: AnimationSpec<Offset>,
    clear: () -> Unit,
) {
    anim.snapTo(anim.value + from)
    clear()
    anim.animateTo(base, spec)
}

@Composable
private fun NumberTile(value: Int, fontSize: TextUnit, correct: Boolean, tileSize: Dp) {
    val base = tileBlue()
    Box(
        Modifier
            .fillMaxSize()
            .background(base),
        contentAlignment = Alignment.Center,
    ) {
        // Penanda halus "sudah di tempatnya": titik di pojok kanan atas.
        val dot by animateFloatAsState(if (correct) 1f else 0f, tween(220), label = "correctDot")
        if (dot > 0f) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(tileSize * 0.09f)
                    .size(tileSize * 0.09f)
                    .graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot }
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50)),
            )
        }
        Text(
            text = value.toString(),
            color = Color.White,
            fontFamily = NumberFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
        )
    }
}

@Composable
private fun ImageTile(image: ImageBitmap, value: Int, n: Int, showNumber: Boolean, badgeSize: TextUnit) {
    val goal = value - 1
    val srcW = image.width / n
    val srcH = image.height / n
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = image,
                srcOffset = IntOffset((goal % n) * srcW, (goal / n) * srcH),
                srcSize = IntSize(srcW, srcH),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        if (showNumber) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = badgeSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(3.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

/** Sorotan hint: bingkai tebal statis (tanpa animasi berdenyut). */
@Composable
private fun HintGlow(shape: RoundedCornerShape) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .border(4.dp, c.onSurface, shape)
            .padding(4.dp)
            .border(2.dp, Color.White, shape),
    )
}

private fun dominant(d: Offset): Direction =
    if (abs(d.x) > abs(d.y)) (if (d.x > 0) Direction.RIGHT else Direction.LEFT)
    else (if (d.y > 0) Direction.DOWN else Direction.UP)

private fun project(d: Offset, dir: Direction): Float = d.x * dir.dCol + d.y * dir.dRow

/** Tarikan dengan hambatan untuk tile yang tidak bisa bergerak (kembali dengan animasi). */
private fun rubberBand(d: Offset, max: Float): Offset {
    fun f(x: Float) = sign(x) * max * (1f - 1f / (1f + abs(x) / (max * 3f)))
    val len = sqrt(d.x * d.x + d.y * d.y)
    return if (len == 0f) Offset.Zero else Offset(f(d.x), f(d.y))
}
