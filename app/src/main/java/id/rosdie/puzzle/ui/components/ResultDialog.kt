package id.rosdie.puzzle.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.rosdie.puzzle.ui.theme.TabularNumbers

/**
 * Dialog hasil permainan (menang, kalah, AI selesai): permukaan solid dengan elevasi di atas
 * scrim gelap. Confetti (opsional) adalah satu-satunya animasi, hanya saat menang.
 */
@Composable
fun ResultDialog(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
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
                color = c.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(72.dp).background(iconBackground, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(40.dp), tint = iconTint)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    content()
                }
            }
            if (celebrate && !reduceMotion) {
                Confetti(Modifier.fillMaxSize(), colors = listOf(c.primary, c.tertiary, Color(0xFF16A34A), Color(0xFF93C5FD)))
            }
        }
    }
}

/** Angka ringkasan hasil: angka besar Fredoka + label. */
@Composable
fun ResultStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.merge(TabularNumbers), maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}
