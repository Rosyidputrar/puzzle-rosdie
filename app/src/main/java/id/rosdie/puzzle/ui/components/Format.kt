package id.rosdie.puzzle.ui.components

import java.text.NumberFormat
import java.util.Locale

/** Waktu sebagai menit:detik (atau jam:menit:detik bila >= 1 jam). */
fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

fun formatSecondsClock(seconds: Long): String = formatClock(seconds * 1000)

fun formatNumber(value: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

fun formatDecimal(value: Double, digits: Int = 1, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(value)

fun formatMegabytes(bytes: Long, locale: Locale = Locale.getDefault()): String =
    formatDecimal(bytes / (1024.0 * 1024.0), 1, locale)
