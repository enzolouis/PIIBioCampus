package com.fneb.piibiocampus.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.error.AppException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Événements ponctuels émis vers le Fragment ────────────────────────────────

sealed class PicturesViewerEvent {
    data class ShowToast(val message: String) : PicturesViewerEvent()
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

        // Signature DAO : onError(Exception) — en pratique toujours une AppException
        // car le DAO mappe systématiquement via FirebaseExceptionMapper.map()
        PictureDao.setAdminValidated(
            pictureId = current.pictureId,
            validated = newValue,
            onSuccess = {
                _uiState.update { it.copy(adminValidated = newValue) }
                emit(PicturesViewerEvent.PictureValidated(current.pictureId, newValue))
                emit(PicturesViewerEvent.ShowToast(
                    if (newValue) "Photo validée" else "Photo invalidée"
                ))
            },
            onError = { e ->
                // On caste vers AppException pour récupérer userMessage ;
                // si le cast échoue (cas imprévu), on affiche le message brut.
                val message = (e as? AppException)?.userMessage ?: e.message ?: "Erreur inattendue"
                emit(PicturesViewerEvent.ShowToast(message))
            }
        )
    }

    fun deletePicture() {
        val pictureId = _uiState.value.pictureId

        // Même signature que setAdminValidated : onError(Exception)
        PictureDao.deletePicture(
            pictureId = pictureId,
            onSuccess = {
                emit(PicturesViewerEvent.ShowToast("Photo supprimée"))
                emit(PicturesViewerEvent.PictureDeleted(pictureId))
                emit(PicturesViewerEvent.Dismiss)
            },
            onError = { e ->
                val message = (e as? AppException)?.userMessage ?: e.message ?: "Erreur inattendue"
                emit(PicturesViewerEvent.ShowToast(message))
            }
        )
    }

    fun reloadPicture() {
        // Signature DAO : onError(AppException) → userMessage directement accessible
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
                // Erreur réseau ou Firestore : mise à jour partielle (recensement marqué terminé)
                // pour ne pas bloquer l'UI, et notification du parent quand même
                _uiState.update { it.copy(recordingStatus = true) }
                emit(PicturesViewerEvent.ShowToast(appException.userMessage))
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