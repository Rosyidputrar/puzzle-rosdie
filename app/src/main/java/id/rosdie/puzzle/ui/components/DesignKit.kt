package id.rosdie.puzzle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.rosdie.puzzle.ui.theme.ButtonShape
import id.rosdie.puzzle.ui.theme.CardShape

/** Nada warna ikon: biru merek, netral, atau merah error. Amber tidak tersedia di sini (aksen). */
enum class Tone { PRIMARY, NEUTRAL, ERROR }

/**
 * Kartu datar: permukaan solid, garis tepi tipis di tema terang, tanpa bayangan (elevasi
 * hanya untuk tile yang bergerak, modal, dan tombol utama).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val border = BorderStroke(1.dp, c.outlineVariant)
    Surface(
        modifier = if (onClick != null) {
            modifier.clip(shape).clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
        } else {
            modifier
        },
        shape = shape,
        color = c.surface,
        border = border,
    ) {
        Box(content = content)
    }
}

/** Ikon dalam kotak bertint datar (radius 12 dp). */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tone: Tone = Tone.PRIMARY, size: Dp = 40.dp) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        Tone.PRIMARY -> c.primaryContainer to c.onPrimaryContainer
        Tone.NEUTRAL -> c.surfaceContainerHighest to c.onSurfaceVariant
        Tone.ERROR -> c.errorContainer to c.onErrorContainer
    }
    Box(modifier.size(size).background(bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = fg)
    }
}

/** Kartu seksi dengan judul (ikon opsional). */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.PRIMARY,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    IconBadge(icon, tone = tone, size = 32.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                trailing()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Label seksi kecil di atas kontrol (dipakai Pengaturan + lembar setup). */
@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = modifier.padding(bottom = 8.dp))
}

/** Bilah progres tipis, warna solid. */
@Composable
fun ThinBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.height(height).semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (progress > 0f) drawRoundRect(color, size = Size(size.width * progress.coerceIn(0f, 1f), size.height), cornerRadius = r)
    }
}

/** Cincin progres solid (penyimpanan galeri). */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, stroke: Dp = 6.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(size.width - w, size.height - w)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/**
 * Pemilih bersegmen (radius 12 dp). Indikator bergeser dengan tween singkat ke pilihan baru:
 * gerak hanya memberi tahu perubahan pilihan, tanpa pantulan.
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    label: @Composable (T) -> String,
    sublabel: (@Composable (T) -> String)? = null,
) {
    val c = MaterialTheme.colorScheme
    val reduceMotion = rememberReducedMotion()
    val animDuration = if (reduceMotion) 0 else 180
    val index = options.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(ButtonShape)
            .background(c.surfaceContainerHigh)
            .padding(3.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * index, tween(animDuration), label = "segment")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .heightIn(min = height - 6.dp)
                .fillMaxHeight()
                .background(c.primary, RoundedCornerShape(10.dp)),
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                val textColor by animateColorAsState(if (isSelected) c.onPrimary else c.onSurface, tween(animDuration), label = "segmentText")
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = height - 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .selectable(isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(label(option), style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    if (sublabel != null) {
                        Text(sublabel(option), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Tombol kontrol papan: ikon + label, datar. Aktif = latar biru tipis. */
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    loading: Boolean = false,
    toggle: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val content = when {
        !enabled -> c.onSurface.copy(alpha = 0.38f)
        active -> c.onPrimaryContainer
        else -> c.onSurface
    }
    Column(
        modifier
            .clip(ButtonShape)
            .then(
                if (toggle) Modifier.toggleable(value = active, enabled = enabled, role = Role.Button, onValueChange = { onClick() })
                else Modifier.clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            )
            .background(if (active && enabled) c.primaryContainer else Color.Transparent)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
            } else {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Tombol utama: biru merek solid, radius 12 dp, sedikit terangkat (tombol utama boleh punya
 * elevasi). [accent] = amber, khusus untuk momen kunci (tombol AI Solver).
 */
@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    destructive: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val container = when {
        destructive -> c.error
        accent -> c.tertiary
        else -> c.primary
    }
    val content = when {
        destructive -> c.onError
        accent -> c.onTertiary
        else -> c.onPrimary
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** Tombol sekunder: garis tepi, datar. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        modifier = modifier.heightIn(min = 52.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Label kecil. [accent] = amber (hanya untuk "Rekor Baru"). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, accent: Boolean = false, icon: ImageVector? = null) {
    val c = MaterialTheme.colorScheme
    val bg = if (accent) c.tertiary else c.primaryContainer
    val fg = if (accent) c.onTertiary else c.onPrimaryContainer
    Row(
        modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Dialog konfirmasi: permukaan solid di atas scrim gelap bawaan platform. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    dismissText: String? = null,
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    IconBadge(icon, tone = if (destructive) Tone.ERROR else Tone.PRIMARY, size = 48.dp)
                    Spacer(Modifier.height(14.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PrimaryButton(confirmText, null, onConfirm, Modifier.fillMaxWidth(), destructive = destructive)
                if (dismissText != null) {
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(dismissText, onDismiss, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Slider: track dan thumb solid biru merek. */
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
        thumb = { Box(Modifier.size(22.dp).background(active, RoundedCornerShape(50))) },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span == 0f) 0f else (state.value - state.valueRange.start) / span
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                drawRoundRect(active, size = Size(size.width * fraction, size.height), cornerRadius = r)
            }
        },
    )
}
