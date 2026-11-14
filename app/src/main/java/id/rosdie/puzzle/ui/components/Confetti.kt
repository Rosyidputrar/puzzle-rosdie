package id.rosdie.puzzle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import id.rosdie.puzzle.domain.engine.SeededRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class Piece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val drift: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val round: Boolean,
)

/** Confetti yang digambar di Canvas (tanpa library). Berhenti otomatis setelah [durationMs]. */
@Composable
fun Confetti(modifier: Modifier = Modifier, colors: List<Color>, durationMs: Long = 3_200, seed: Long = 7) {
    val pieces = remember(seed, colors) {
        val r = SeededRandom(seed)
        List(110) {
            Piece(
                x = r.nextInt(1000) / 1000f,
                delay = r.nextInt(900) / 1000f,
                speed = 0.35f + r.nextInt(50) / 100f,
                drift = (r.nextInt(200) - 100) / 1000f,
                spin = 180f + r.nextInt(540),
                size = 6f + r.nextInt(8),
                color = colors[r.nextInt(colors.size)],
                round = r.nextInt(3) == 0,
            )
        }
    }
    var elapsed by remember { mutableLongStateOf(0L) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (elapsed < durationMs) {
            withFrameNanos { elapsed = (it - start) / 1_000_000 }
        }
        finished = true
    }
    if (finished) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        val fade = ((durationMs / 1000f - t) / 0.6f).coerceIn(0f, 1f)
        for (p in pieces) {
            val local = t - p.delay
            if (local < 0) continue
            val y = -20f + local * p.speed * size.height * 0.9f
            if (y > size.height + 20) continue
            val x = p.x * size.width + sin((local * 2.6f + p.x * 6f).toDouble()).toFloat() * 24f + p.drift * size.width * local
            val angle = local * p.spin
            val c = p.color.copy(alpha = fade)
            val px = p.size * density / 2.2f
            if (p.round) {
                drawCircle(c, radius = px * 0.6f, center = Offset(x, y))
            } else {
                rotate(angle, pivot = Offset(x, y)) {
                    val squash = cos(angle * PI / 180).toFloat().let { if (it < 0) -it else it }.coerceAtLeast(0.2f)
                    drawRect(c, topLeft = Offset(x - px / 2, y - px * squash / 2), size = Size(px, px * 1.6f * squash))
                }
            }
        }
    }
}
