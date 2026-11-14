package id.rosdie.puzzle.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.rosdie.puzzle.PuzzleApplication
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.repository.CustomImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryUiState(
    val images: List<CustomImage> = emptyList(),
    val usageBytes: Long = 0,
    val difficultySize: Int = 4,
) {
    val usageFraction: Float get() = (usageBytes.toDouble() / ImageRepository.MAX_TOTAL_BYTES).toFloat().coerceIn(0f, 1f)
    val nearlyFull: Boolean get() = usageFraction >= ImageRepository.WARN_FRACTION
}

class GalleryViewModel(private val images: ImageRepository, settings: Flow<AppSettings>) : ViewModel() {
    val state: StateFlow<GalleryUiState> = combine(images.customImages, settings) { list, s ->
        GalleryUiState(list, list.sumOf { it.sizeBytes }, s.difficulty.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    fun delete(id: String) {
        viewModelScope.launch { images.delete(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PuzzleApplication).container
                GalleryViewModel(c.images, c.settings)
            }
        }
    }
}
