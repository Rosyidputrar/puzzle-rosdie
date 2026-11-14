package id.rosdie.puzzle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rosdie.puzzle.ui.navigation.PuzzleNavHost
import id.rosdie.puzzle.ui.theme.PuzzleRosdieTheme

/** Satu-satunya Activity. AppCompatActivity diperlukan untuk bahasa per-aplikasi di Android < 13. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as PuzzleApplication).container
        // Tahan splash sistem sampai pengaturan (tema) terbaca supaya tidak berkedip.
        splash.setKeepOnScreenCondition { !container.settingsLoaded.value }
        enableEdgeToEdge()
        setContent {
            val settings by container.settings.collectAsStateWithLifecycle()
            PuzzleRosdieTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                PuzzleNavHost(container = container, showSplash = savedInstanceState == null)
            }
        }
    }
}
