package id.rosdie.puzzle.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/** True bila animasi sistem dimatikan (Developer options / aksesibilitas "Hapus animasi"). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    return if (inspection) {
        false
    } else {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
