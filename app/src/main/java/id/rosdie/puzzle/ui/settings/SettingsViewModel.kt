package id.rosdie.puzzle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.PuzzleApplication
import id.rosdie.puzzle.data.datastore.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settings

    private val _clearing = MutableStateFlow(false)
    val clearing: StateFlow<Boolean> = _clearing.asStateFlow()

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) = container.updateSettings(transform)

    fun clearAllData() {
        _clearing.value = true
        viewModelScope.launch {
            container.clearAllData()
            _clearing.value = false
            _cleared.value = true
        }
    }

    fun consumeCleared() {
        _cleared.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel((this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PuzzleApplication).container)
            }
        }
    }
}
