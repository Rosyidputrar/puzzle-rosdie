package id.rosdie.puzzle.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import id.rosdie.puzzle.ui.theme.TabularNumbers

/** Nada warna untuk badge/aksen. Diturunkan dari skema aktif (ikut tema gelap & warna dinamis). */
enum class Tone { PRIMARY, SECONDARY, TERTIARY, ERROR, NEUTRAL }

@Composable
fun toneContainer(tone: Tone): Color = when (tone) {
    Tone.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    Tone.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    Tone.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.ERROR -> MaterialTheme.colorScheme.errorContainer
    Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
}

@Composable
fun toneContent(tone: Tone): Color = when (tone) {
    Tone.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Gradient aksen dari warna primary skema aktif (bukan warna statis). */
@Composable
fun accentBrush(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(lerp(c.primary, Color.White, 0.18f), c.primary, lerp(c.primary, Color.Black, 0.12f)))
}

/** Ikon di dalam kotak membulat berwarna. */
@Composable
fun IconBadge(icon: ImageVector, tone: Tone, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .background(toneContainer(tone), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = toneContent(tone))
    }
}

/** Kartu seksi dengan judul + ikon. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.PRIMARY,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    IconBadge(icon, tone, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                trailing()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Statistik ringkas untuk HUD permainan: label kecil, angka besar tabular, dan bilah
 * progres tipis opsional (mis. sisa waktu tantangan atau tile yang sudah benar).
 */
@Composable
fun HudStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    progress: Float? = null,
    warning: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val accent by animateColorAsState(if (warning) c.error else c.primary, label = "hudAccent")
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = c.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).semantics(mergeDescendants = true) {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, Modifier.size(14.dp), tint = accent)
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                color = if (warning) c.error else c.onSurface,
                maxLines = 1,
            )
            // Ruang bilah progres selalu disediakan agar semua kartu HUD sama tinggi.
            val p by animateFloatAsState((progress ?: 0f).coerceIn(0f, 1f), label = "hudProgress")
            Spacer(Modifier.height(6.dp))
            if (progress != null) ThinBar(p, accent, Modifier.fillMaxWidth()) else Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun ThinBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (progress > 0f) drawRoundRect(color, size = Size(size.width * progress, size.height), cornerRadius = r)
    }
}

/** Cincin progres untuk ringkasan (galeri, statistik). */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, stroke: Dp = 6.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(size.width - w, size.height - w)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/**
 * Pemilih bersegmen kustom dengan indikator yang meluncur (spring). Menggantikan
 * SegmentedButton default. Aksesibel sebagai grup radio.
 */
@Composable
fun <T> PillSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    label: @Composable (T) -> String,
    sublabel: (@Composable (T) -> String)? = null,
) {
    val c = MaterialTheme.colorScheme
    val index = options.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(CircleShape)
            .background(c.surfaceContainerHigh)
            .padding(4.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * index, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "pill")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .heightIn(min = height - 8.dp)
                .fillMaxHeight()
                .shadow(2.dp, CircleShape)
                .background(c.primary, CircleShape),
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                val textColor by animateColorAsState(if (isSelected) c.onPrimary else c.onSurfaceVariant, label = "pillText")
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = height - 8.dp)
                        .clip(CircleShape)
                        .selectable(isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (sublabel != null) {
                        Text(sublabel(option), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.85f), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Satu aksi di dock kontrol: ikon dalam kotak membulat + label. */
@Composable
fun DockAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    loading: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when {
            !enabled -> Color.Transparent
            active -> c.primary
            else -> c.surfaceContainerHighest
        },
        label = "dockBg",
    )
    val content = when {
        !enabled -> c.onSurface.copy(alpha = 0.38f)
        active -> c.onPrimary
        else -> c.onSurface
    }
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = active, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp, 40.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
            } else {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tombol utama dengan gradient aksen dan bayangan lembut. */
@Composable
fun GradientButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier.heightIn(min = 56.dp),
        shadowElevation = if (enabled) 4.dp else 0.dp,
    ) {
        Row(
            Modifier
                .background(if (enabled) accentBrush() else Brush.linearGradient(listOf(c.surfaceContainerHighest, c.surfaceContainerHighest)))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (enabled) c.onPrimary else c.onSurface.copy(alpha = 0.38f)
            if (icon != null) {
                Icon(icon, null, tint = tint)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
        }
    }
}

/** Slider bergaya aplikasi: track membulat penuh + thumb bulat berbayang. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val active = if (enabled) c.primary else c.onSurface.copy(alpha = 0.38f)
    val track = c.surfaceContainerHighest
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                Modifier
                    .size(24.dp)
                    .shadow(3.dp, CircleShape)
                    .background(c.surfaceContainerLowest, CircleShape)
                    .padding(4.dp)
                    .background(active, CircleShape),
            )
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span == 0f) 0f else (state.value - state.valueRange.start) / span
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                drawRoundRect(active, size = Size(size.width * fraction, size.height), cornerRadius = r)
            }
        },
    )
}
