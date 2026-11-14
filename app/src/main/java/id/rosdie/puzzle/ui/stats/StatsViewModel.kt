package id.rosdie.puzzle.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.rosdie.puzzle.PuzzleApplication
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.AchievementRepository
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.data.repository.StatsCalculator
import id.rosdie.puzzle.data.repository.StatsSummary
import id.rosdie.puzzle.data.repository.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StatsUiState(
    val loading: Boolean = true,
    val stats: StatsSummary? = null,
    val unlocked: Map<Achievement, Long> = emptyMap(),
    val todayKey: Int = 0,
    /** Bulan yang ditampilkan di kalender: (tahun, bulan 1..12). */
    val calendarYear: Int = 0,
    val calendarMonth: Int = 0,
)

class StatsViewModel(games: GameRepository, achievements: AchievementRepository, private val time: TimeSource) : ViewModel() {
    private val month = MutableStateFlow(time.todayKey().let { DateKeys.year(it) to DateKeys.month(it) })

    val state: StateFlow<StatsUiState> = combine(games.allResults, achievements.unlocked, month) { results, unlocked, ym ->
        StatsUiState(
            loading = false,
            stats = StatsCalculator.compute(results, time.todayEpochDay()) { DateKeys.epochDayOf(it) },
            unlocked = unlocked,
            todayKey = time.todayKey(),
            calendarYear = ym.first,
            calendarMonth = ym.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun shiftMonth(delta: Int) = month.update { (y, m) ->
        val index = y * 12 + (m - 1) + delta
        index / 12 to index % 12 + 1
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PuzzleApplication).container
                StatsViewModel(c.games, c.achievements, c.time)
            }
        }
    }
}
