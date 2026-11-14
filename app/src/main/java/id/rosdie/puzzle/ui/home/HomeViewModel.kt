package id.rosdie.puzzle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.rosdie.puzzle.PuzzleApplication
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.data.repository.SavedGame
import id.rosdie.puzzle.data.repository.StatsCalculator
import id.rosdie.puzzle.data.repository.StatsSummary
import id.rosdie.puzzle.data.repository.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val stats: StatsSummary? = null,
    val saved: SavedGame? = null,
    val settings: AppSettings = AppSettings(),
    val todayKey: Int = 0,
)

class HomeViewModel(games: GameRepository, settings: Flow<AppSettings>, time: TimeSource) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(games.allResults, games.savedGame, settings) { results, saved, s ->
        HomeUiState(
            loading = false,
            stats = StatsCalculator.compute(results, time.todayEpochDay()) { DateKeys.epochDayOf(it) },
            saved = saved,
            settings = s,
            todayKey = time.todayKey(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PuzzleApplication).container
                HomeViewModel(c.games, c.settings, c.time)
            }
        }
    }
}
