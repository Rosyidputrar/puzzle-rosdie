package id.rosdie.puzzle.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.ui.gallery.GalleryScreen
import id.rosdie.puzzle.ui.game.GameScreen
import id.rosdie.puzzle.ui.home.HomeScreen
import id.rosdie.puzzle.ui.howto.HowToScreen
import id.rosdie.puzzle.ui.settings.SettingsScreen
import id.rosdie.puzzle.ui.splash.SplashScreen
import id.rosdie.puzzle.ui.stats.StatsScreen

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
    NavHost(
        navController = nav,
        startDestination = if (showSplash) SplashRoute else HomeRoute,
        // Transisi ala iOS: layar baru meluncur dari kanan, layar lama bergeser sedikit (parallax)
        // dan meredup; kembali = kebalikannya. Spring membuatnya terasa luwes.
        enterTransition = {
            slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(220))
        },
        exitTransition = {
            slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { -it / 4 } + fadeOut(tween(260))
        },
        popEnterTransition = {
            slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { -it / 4 } + fadeIn(tween(260))
        },
        popExitTransition = {
            slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut(tween(220))
        },
    ) {
        composable<SplashRoute>(enterTransition = { fadeIn() }, exitTransition = { fadeOut(tween(250)) }) {
            SplashScreen(onFinished = {
                nav.navigate(HomeRoute) { popUpTo(SplashRoute) { inclusive = true } }
            })
        }
        composable<HomeRoute>(enterTransition = { fadeIn(tween(300)) }) {
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
