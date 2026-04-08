package com.fneb.piibiocampus.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.dao.CampusDao
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.model.Campus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── État UI ───────────────────────────────────────────────────────────────────

data class MapUiState(
    val pictures:   List<Map<String, Any>> = emptyList(),
    val campusList: List<Campus>           = emptyList()
)

// ── Événements ponctuels ──────────────────────────────────────────────────────

sealed class MapEvent {
    data class ShowError(val message: String) : MapEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MapViewModel : ViewModel() {

    private val TAG = "MapViewModel"

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MapEvent>()
    val events: SharedFlow<MapEvent> = _events.asSharedFlow()

    // ── Chargement des photos ─────────────────────────────────────────────────

    fun loadAllPictures() {
        PictureDao.getAllPicturesEnriched(
            onSuccess = { list ->
                _uiState.update { it.copy(pictures = list) }
            },
            onError = { e ->
                Log.e(TAG, "Erreur récupération pictures : ${e.userMessage}", e)
                emit(MapEvent.ShowError(e.userMessage))
                _uiState.update { it.copy(pictures = emptyList()) }
            }
        )
    }

    fun loadPicturesNear(lat: Double, lon: Double, radiusMeters: Double) {
        PictureDao.getPicturesNearLocationEnriched(
            centerLat    = lat,
            centerLon    = lon,
            radiusMeters = radiusMeters,
            onSuccess    = { list ->
                _uiState.update { it.copy(pictures = list) }
            },
            onError = { e ->
                Log.e(TAG, "Erreur récupération nearby pictures : ${e.userMessage}", e)
                emit(MapEvent.ShowError(e.userMessage))
                _uiState.update { it.copy(pictures = emptyList()) }
            }
        )
    }

    // ── Chargement des campus ─────────────────────────────────────────────────

    fun loadCampus() {
        // CampusDao.getAll → onError: (AppException) → userMessage directement accessible
        CampusDao.getAll(
            onComplete = { list ->
                _uiState.update { it.copy(campusList = list) }
            },
            onError = { e ->
                Log.e(TAG, "Erreur récupération campus : ${e.userMessage}", e)
                emit(MapEvent.ShowError(e.userMessage))
            }
        )
    }

    // ── Helper interne ────────────────────────────────────────────────────────

    private fun emit(event: MapEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}