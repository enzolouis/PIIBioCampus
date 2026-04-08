package com.fneb.piibiocampus.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fneb.piibiocampus.data.dao.CensusDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.ui.census.CensusNode

class CensusRepository(private val dao: CensusDao) {

    // ── Roots (navigation pure) ───────────────────────────────────────────────
    // Séparé du uiState : le ViewModel en a besoin pour la navigation
    // indépendamment de l'état de chargement.
    private val _roots = MutableLiveData<List<CensusNode>>(emptyList())
    val roots: LiveData<List<CensusNode>> = _roots

    // ── État de chargement exposé à la Vue ────────────────────────────────────
    private val _uiState = MutableLiveData<UiState<List<CensusNode>>>(UiState.Idle)
    val uiState: LiveData<UiState<List<CensusNode>>> = _uiState

    fun loadRoots() {
        _uiState.postValue(UiState.Loading)
        dao.fetchCensusTree(
            onComplete = { roots ->
                _roots.postValue(roots)
                _uiState.postValue(UiState.Success(roots))
            },
            onError = { e ->
                _uiState.postValue(UiState.Error(e))
            }
        )
    }
}