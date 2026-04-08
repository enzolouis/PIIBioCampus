package com.fneb.piibiocampus.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Événements ponctuels émis vers le Fragment ────────────────────────────────

sealed class PicturesViewerEvent {
    /** Feedback de succès à afficher en Toast */
    data class ShowToast(val message: String) : PicturesViewerEvent()
    /** Erreur métier — le Fragment appelle showError(exception) */
    data class ShowError(val exception: AppException) : PicturesViewerEvent()
    data class PictureValidated(val pictureId: String, val newValue: Boolean) : PicturesViewerEvent()
    data class PictureDeleted(val pictureId: String) : PicturesViewerEvent()
    object CensusUpdated : PicturesViewerEvent()
    object Dismiss : PicturesViewerEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PicturesViewerViewModel(initialState: PhotoViewerState) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<PhotoViewerState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PicturesViewerEvent>()
    val events: SharedFlow<PicturesViewerEvent> = _events.asSharedFlow()

    // ── Actions ───────────────────────────────────────────────────────────────

    fun validatePicture() {
        val current  = _uiState.value
        val newValue = !current.adminValidated

        PictureDao.setAdminValidated(
            pictureId = current.pictureId,
            validated = newValue,
            onSuccess = {
                _uiState.update { it.copy(adminValidated = newValue) }
                emit(PicturesViewerEvent.PictureValidated(current.pictureId, newValue))
                emit(PicturesViewerEvent.ShowToast(if (newValue) "Photo validée" else "Photo invalidée"))
            },
            onError = { e ->
                // setAdminValidated déclare (Exception) mais mappe toujours via FirebaseExceptionMapper
                val appException = (e as? AppException) ?: AppException.Unknown(e)
                emit(PicturesViewerEvent.ShowError(appException))
            }
        )
    }

    fun deletePicture() {
        val pictureId = _uiState.value.pictureId

        PictureDao.deletePicture(
            pictureId = pictureId,
            onSuccess = {
                emit(PicturesViewerEvent.ShowToast("Photo supprimée"))
                emit(PicturesViewerEvent.PictureDeleted(pictureId))
                emit(PicturesViewerEvent.Dismiss)
            },
            onError = { e ->
                val appException = (e as? AppException) ?: AppException.Unknown(e)
                emit(PicturesViewerEvent.ShowError(appException))
            }
        )
    }

    fun reloadPicture() {
        // getPictureEnrichedById → onError: (AppException) directement
        PictureDao.getPictureEnrichedById(
            pictureId = _uiState.value.pictureId,
            onSuccess = { updatedPhoto ->
                _uiState.update { current ->
                    current.copy(
                        family          = updatedPhoto["family"]          as? String,
                        genre           = updatedPhoto["genre"]           as? String,
                        specie          = updatedPhoto["specie"]          as? String,
                        censusRef       = updatedPhoto["censusRef"]       as? String,
                        recordingStatus = updatedPhoto["recordingStatus"] as? Boolean ?: false,
                        adminValidated  = updatedPhoto["adminValidated"]  as? Boolean ?: false
                    )
                }
                emit(PicturesViewerEvent.CensusUpdated)
            },
            onError = { appException ->
                _uiState.update { it.copy(recordingStatus = true) }
                emit(PicturesViewerEvent.ShowError(appException))
                emit(PicturesViewerEvent.CensusUpdated)
            }
        )
    }

    // ── Helper interne ────────────────────────────────────────────────────────

    private fun emit(event: PicturesViewerEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(initialState: PhotoViewerState): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PicturesViewerViewModel(initialState) }
            }
    }
}