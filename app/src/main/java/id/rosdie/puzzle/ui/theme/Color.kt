package id.rosdie.puzzle.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Palet Puzzle Rosdie (sengaja dibatasi):
 * - Netral (dasar): slate. Terang #F8FAFC / putih / teks #0F172A, #64748B.
 *   Gelap #0F172A / #1E293B / teks #F1F5F9.
 * - Biru merek dari logo web, SOLID (tanpa gradien): #2563EB untuk tombol utama dan tile,
 *   #1D4ED8 sebagai varian gelap.
 * - Satu aksen: amber #F59E0B, HANYA untuk momen kunci: tombol AI Solver, bintang menang,
 *   label Rekor Baru. Di Material ia dipetakan ke peran `tertiary`.
 * - Hijau sukses dan merah error adalah warna sistem, bukan warna merek.
 */
val BrandBlue = Color(0xFF2563EB)
val BrandBlueDark = Color(0xFF1D4ED8)
val Amber = Color(0xFFF59E0B)
val OnAmber = Color(0xFF0F172A)
val Success = Color(0xFF16A34A)

/** Teks sukses/aksen yang lolos kontras di kedua tema (audit UI/UX 14 Nov 2026). */
val SuccessTextLight = Color(0xFF15803D)
val SuccessTextDark = Color(0xFF4ADE80)
val AccentTextDark = Color(0xFF93C5FD)
val ConfettiBlue = Color(0xFF93C5FD)

private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Amber,
    onTertiary = OnAmber,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceTint = Color.Transparent,
    inverseSurface = Slate800,
    inverseOnSurface = Slate100,
    outline = Slate300,
    outlineVariant = Slate200,
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Slate200,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,
)

val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    inversePrimary = BrandBlueDark,
    secondary = Slate400,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Amber,
    onTertiary = OnAmber,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFF87171),
    onError = Slate900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    surfaceTint = Color.Transparent,
    inverseSurface = Slate100,
    inverseOnSurface = Slate800,
    outline = Slate600,
    outlineVariant = Slate700,
    scrim = Color.Black,
    surfaceBright = Slate700,
    surfaceDim = Slate900,
    surfaceContainerLowest = Slate900,
    surfaceContainerLow = Slate800,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerHighest = Slate700,
)
