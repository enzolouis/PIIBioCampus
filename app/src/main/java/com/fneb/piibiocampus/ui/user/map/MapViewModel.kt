package com.fneb.piibiocampus.ui.user.map

import android.util.Log
import androidx.lifecycle.ViewModel
import com.fneb.piibiocampus.data.dao.CampusDao
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.model.Campus
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.flow.*

class MapViewModel : ViewModel() {

    private val TAG = "MapViewModel"

    private val _picturesState = MutableStateFlow<UiState<List<Map<String, Any>>>>(UiState.Idle)
    val picturesState: StateFlow<UiState<List<Map<String, Any>>>> = _picturesState.asStateFlow()

    private val _campusState = MutableStateFlow<UiState<List<Campus>>>(UiState.Idle)
    val campusState: StateFlow<UiState<List<Campus>>> = _campusState.asStateFlow()

    // ── Chargement des photos ─────────────────────────────────────────────────

    fun loadAllPictures() {
        _picturesState.value = UiState.Loading
        PictureDao.getAllPicturesEnriched(
            onSuccess = { list -> _picturesState.value = UiState.Success(list) },
            onError   = { e ->
                Log.e(TAG, "Erreur récupération pictures : ${e.userMessage}", e)
                _picturesState.value = UiState.Error(e)
            }
        )
    }

    fun loadPicturesNear(lat: Double, lon: Double, radiusMeters: Double) {
        _picturesState.value = UiState.Loading
        PictureDao.getPicturesNearLocationEnriched(
            centerLat    = lat,
            centerLon    = lon,
            radiusMeters = radiusMeters,
            onSuccess    = { list -> _picturesState.value = UiState.Success(list) },
            onError      = { e ->
                Log.e(TAG, "Erreur récupération nearby pictures : ${e.userMessage}", e)
                _picturesState.value = UiState.Error(e)
            }
        )
    }

    // ── Chargement des campus ─────────────────────────────────────────────────

    fun loadCampus() {
        _campusState.value = UiState.Loading
        CampusDao.getAll(
            onComplete = { list -> _campusState.value = UiState.Success(list) },
            onError    = { e ->
                Log.e(TAG, "Erreur récupération campus : ${e.userMessage}", e)
                _campusState.value = UiState.Error(e)
            }
        )
    }
}