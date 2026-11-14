package id.rosdie.puzzle.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.rosdie.puzzle.R
import id.rosdie.puzzle.ui.components.rememberReducedMotion
import id.rosdie.puzzle.ui.theme.BrandBlue
import id.rosdie.puzzle.ui.theme.BrandBlueDark
import id.rosdie.puzzle.ui.theme.BrandBlueLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Posisi awal acak (dalam satuan sel) untuk animasi tile menyusun diri. */
private val SCATTER = listOf(
    Offset(1f, 2f), Offset(2f, 0f), Offset(0f, 1f), Offset(2f, 1f),
    Offset(0f, 0f), Offset(1f, 0f), Offset(2f, 2f), Offset(1f, 1f),
)

/**
 * Logo: grid 3x3 dengan satu kotak kosong. Dengan [progress] < 1, tile bergerak dari posisi
 * acak menuju posisi benar.
 */
@Composable
fun LogoMark(modifier: Modifier = Modifier, animate: Boolean = false, progress: Float? = null) {
    val reduceMotion = rememberReducedMotion()
    val anim = remember { Animatable(if (animate && !reduceMotion && progress == null) 0f else 1f) }
    if (animate && progress == null) {
        LaunchedEffect(Unit) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    }
    val p = progress ?: anim.value
    val desc = stringResource(R.string.a11y_logo)
    Canvas(modifier.semantics { contentDescription = desc }) {
        val cell = size.minDimension / 3f
        val gap = cell * 0.12f
        val tile = cell - gap
        val r = CornerRadius(tile * 0.22f)
        for (i in 0 until 8) {
            val goal = Offset((i % 3).toFloat(), (i / 3).toFloat())
            val from = SCATTER[i]
            val t = ((p * 1.4f) - i * 0.05f).coerceIn(0f, 1f)
            val pos = from + (goal - from) * t
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(pos.x * cell + gap / 2, pos.y * cell + gap / 2),
                size = Size(tile, tile),
                cornerRadius = r,
            )
        }
        val o = Offset(2 * cell + gap / 2, 2 * cell + gap / 2)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f * p),
            topLeft = o + Offset(gap / 3, gap / 3),
            size = Size(tile - gap * 2 / 3, tile - gap * 2 / 3),
            cornerRadius = r,
            style = Stroke(width = gap * 0.35f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(gap * 0.8f, gap * 0.5f))),
        )
        drawCircle(Color.White.copy(alpha = 0.8f * p), radius = gap * 0.45f, center = o + Offset(tile / 2, tile / 2))
    }
}

/** Splash dalam aplikasi: tile menyusun diri (maks 1,5 detik) lalu ke Beranda. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val reduceMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val titleAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(300)
        } else {
            launch { progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) }
            delay(450)
            titleAlpha.animateTo(1f, tween(450))
            delay(250)
        }
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BrandBlueLight, BrandBlue, BrandBlueDark))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            LogoMark(Modifier.size(140.dp), progress = progress.value)
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
            )
        }
    }
}
