package id.rosdie.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Kerangka dialog hasil permainan: header gradient sesuai nada (menang/kalah/AI) dengan ikon
 * yang "meletup", isi bebas, dan confetti opsional di atas kartu.
 */
@Composable
fun ResultDialog(
    icon: ImageVector,
    tone: Tone,
    title: String,
    subtitle: String?,
    celebrate: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = rememberReducedMotion()
    val c = MaterialTheme.colorScheme
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = c.surfaceContainerLow,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val headerBase = toneContainer(tone)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(headerBase, lerp(headerBase, c.surfaceContainerLow, 0.85f))))
                            .padding(top = 28.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val pop = remember { Animatable(if (reduceMotion) 1f else 0.3f) }
                        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)) }
                        Surface(
                            shape = CircleShape,
                            color = c.surfaceContainerLowest,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(88.dp)
                                .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, Modifier.size(48.dp), tint = toneContent(tone))
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        if (subtitle != null) {
                            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(16.dp))
                        content()
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
            if (celebrate && !reduceMotion) {
                Confetti(
                    Modifier.fillMaxSize(),
                    colors = listOf(c.primary, c.tertiary, Color(0xFF60A5FA), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF22C55E)),
                )
            }
        }
    }
}

/** Kotak angka kecil untuk ringkasan hasil. */
@Composable
fun ResultStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge.merge(id.rosdie.puzzle.ui.theme.TabularNumbers), maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
