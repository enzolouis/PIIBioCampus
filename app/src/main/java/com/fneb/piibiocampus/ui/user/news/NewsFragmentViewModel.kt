package com.fneb.piibiocampus.ui.user.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.data.ui.UiState

class NewsFragmentViewModel : ViewModel() {

    private val _loadState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val loadState: LiveData<UiState<Unit>> = _loadState

    private val _dynamicNews = MutableLiveData<List<ItemNews>>(emptyList())
    val dynamicNews: LiveData<List<ItemNews>> = _dynamicNews

    private val _staticNews = MutableLiveData<List<ItemNews>>(emptyList())
    val staticNews: LiveData<List<ItemNews>> = _staticNews

    fun loadNews() {
        _loadState.value = UiState.Loading

        var dynamicLoaded = false
        var staticLoaded = false
        var errorOccurred = false

        fun checkSuccess() {
            if (dynamicLoaded && staticLoaded && !errorOccurred) {
                _loadState.postValue(UiState.Success(Unit))
            }
        }

        // dynamic news
        NewsDao.getDynamicNews(
            onSuccess = { items ->
                _dynamicNews.postValue(items)
                dynamicLoaded = true
                checkSuccess()
            },
            onError = { e ->
                if (!errorOccurred) {
                    errorOccurred = true
                    _loadState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
                }
            }
        )

        // static news
        NewsDao.getStaticNews(
            onSuccess = { items ->
                _staticNews.postValue(items)
                staticLoaded = true
                checkSuccess()
            },
            onError = { e ->
                if (!errorOccurred) {
                    errorOccurred = true
                    _loadState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
                }
            }
        )
    }
}