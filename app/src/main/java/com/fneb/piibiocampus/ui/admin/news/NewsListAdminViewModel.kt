package com.fneb.piibiocampus.ui.admin.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.data.ui.UiState

class NewsListAdminViewModel : ViewModel() {

    // ── État de chargement (Loading / Success / Error) ────────────────────────
    private val _loadState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val loadState: LiveData<UiState<Unit>> = _loadState

    // ── Données ───────────────────────────────────────────────────────────────
    private val _dynamicNews = MutableLiveData<List<ItemNews>>(emptyList())
    val dynamicNews: LiveData<List<ItemNews>> = _dynamicNews

    private val _staticNews = MutableLiveData<List<ItemNews>>(emptyList())
    val staticNews: LiveData<List<ItemNews>> = _staticNews

    // ── Chargement ────────────────────────────────────────────────────────────
    fun loadNews() {
        _loadState.value = UiState.Loading

        var dynamicLoaded = false
        var staticLoaded = false
        var errorOccurred = false

        // Fonction locale pour vérifier si tout est terminé
        fun checkSuccess() {
            if (dynamicLoaded && staticLoaded && !errorOccurred) {
                _loadState.postValue(UiState.Success(Unit))
            }
        }

        // Chargement des actualités dynamiques
        NewsDao.getDynamicNews(
            onSuccess = { items ->
                _dynamicNews.postValue(items)
                dynamicLoaded = true
                checkSuccess()
            },
            onError = { e ->
                if (!errorOccurred) {
                    errorOccurred = true
                    // Utilisation du Mapper pour transformer l'Exception en AppException
                    _loadState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
                }
            }
        )

        // Chargement des actualités statiques
        NewsDao.getStaticNews(
            onSuccess = { items ->
                _staticNews.postValue(items)
                staticLoaded = true
                checkSuccess()
            },
            onError = { e ->
                if (!errorOccurred) {
                    errorOccurred = true
                    // Utilisation du Mapper pour transformer l'Exception en AppException
                    _loadState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
                }
            }
        )
    }
}

class NewsListAdminViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NewsListAdminViewModel() as T
}