package id.rosdie.puzzle.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.datastore.ThemeMode

/**
 * Satu set radius kecil yang disengaja: tombol 12 dp, tile 12 dp, kartu 16 dp, dialog 20 dp.
 * Bentuk pil tidak dipakai sebagai default.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

val ButtonShape = RoundedCornerShape(12.dp)
val TileShape = RoundedCornerShape(12.dp)
val CardShape = RoundedCornerShape(16.dp)

/**
 * Fredoka (SIL OFL 1.1) hanya untuk angka: tile, skor, dan hitungan besar. Terminal yang
 * membulat senada dengan tile berujung bulat, berat SemiBold tetap tegas di tile 5x5 yang
 * kecil, dan bentuk angkanya jelas dibedakan (1/7, 6/9). Teks lain memakai font sistem.
 */
val NumberFont = FontFamily(Font(R.font.fredoka_semibold, FontWeight.SemiBold))

/** Angka tabular berfont Fredoka: lebar digit sama, sehingga timer dan hitungan tidak bergoyang. */
val TabularNumbers = TextStyle(fontFamily = NumberFont, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** True bila tema gelap aktif. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Dynamic color tersedia di Android 12+; di bawahnya selalu memakai palet biru. */
val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun PuzzleRosdieTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
