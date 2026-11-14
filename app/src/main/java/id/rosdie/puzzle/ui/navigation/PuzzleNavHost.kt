package id.rosdie.puzzle.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.ui.components.rememberReducedMotion
import id.rosdie.puzzle.ui.gallery.GalleryScreen
import id.rosdie.puzzle.ui.game.GameScreen
import id.rosdie.puzzle.ui.home.HomeScreen
import id.rosdie.puzzle.ui.howto.HowToScreen
import id.rosdie.puzzle.ui.settings.SettingsScreen
import id.rosdie.puzzle.ui.splash.SplashScreen
import id.rosdie.puzzle.ui.stats.StatsScreen
import kotlin.math.roundToInt

@Composable
fun PuzzleNavHost(
    container: AppContainer,
    showSplash: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val nav = navController
    fun back() {
        if (!nav.popBackStack()) nav.navigate(HomeRoute)
    }
    fun home() {
        if (!nav.popBackStack(HomeRoute, inclusive = false)) nav.navigate(HomeRoute) { popUpTo(0) }
    }
    val settings by container.settings.collectAsStateWithLifecycle()
    val reduceMotion = rememberReducedMotion()
    // Satu sistem tween untuk semua transisi layar: durasi mengikuti kecepatan animasi di
    // Pengaturan, dan hilang total bila animasi sistem dimatikan. Splash tetap fade-only
    // karena ia pengambilalihan layar penuh, bukan perpindahan antar layar.
    val navDuration = (300 / settings.animationSpeed.stiffnessFactor).roundToInt()
    NavHost(
        navController = nav,
        startDestination = if (showSplash) SplashRoute else HomeRoute,
        enterTransition = {
            if (reduceMotion) EnterTransition.None
            else slideInHorizontally(tween(navDuration)) { it / 4 } + fadeIn(tween(navDuration))
        },
        exitTransition = {
            if (reduceMotion) ExitTransition.None
            else slideOutHorizontally(tween(navDuration)) { -it / 4 } + fadeOut(tween(navDuration))
        },
        popEnterTransition = {
            if (reduceMotion) EnterTransition.None
            else slideInHorizontally(tween(navDuration)) { -it / 4 } + fadeIn(tween(navDuration))
        },
        popExitTransition = {
            if (reduceMotion) ExitTransition.None
            else slideOutHorizontally(tween(navDuration)) { it / 4 } + fadeOut(tween(navDuration))
        },
    ) {
        composable<SplashRoute>(enterTransition = { fadeIn() }, exitTransition = { fadeOut(tween(250)) }) {
            SplashScreen(onFinished = {
                nav.navigate(HomeRoute) { popUpTo(SplashRoute) { inclusive = true } }
            })
        }
        composable<HomeRoute> {
            HomeScreen(
                container = container,
                onPlay = { nav.navigate(GameRoute.newGame(it)) },
                onResume = { nav.navigate(GameRoute(resume = true)) },
                onDaily = { nav.navigate(GameRoute(daily = true)) },
                onGallery = { nav.navigate(GalleryRoute) },
                onStats = { nav.navigate(StatsRoute) },
                onHowTo = { nav.navigate(HowToRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<GameRoute> {
            GameScreen(container = container, onBack = ::back, onHome = ::home)
        }
        composable<GalleryRoute> {
            GalleryScreen(
                container = container,
                onBack = ::back,
                onUseImage = { config -> nav.navigate(GameRoute.newGame(config)) { popUpTo(HomeRoute) } },
            )
        }
        composable<StatsRoute> { StatsScreen(onBack = ::back) }
        composable<HowToRoute> {
            HowToScreen(
                onBack = ::back,
                onTryNow = { config -> nav.navigate(GameRoute.newGame(config)) { popUpTo(HomeRoute) } },
            )
        }
        composable<SettingsRoute> { SettingsScreen(container = container, onBack = ::back) }
    }
}
