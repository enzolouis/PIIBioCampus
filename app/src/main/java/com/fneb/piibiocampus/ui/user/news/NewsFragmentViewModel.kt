package com.fneb.piibiocampus.ui.user.news

import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NewsFragmentViewModel {

    private val _state = MutableStateFlow<UiState<List<ItemNews>>>(UiState.Idle)
    val state: StateFlow<UiState<List<ItemNews>>> = _state.asStateFlow()

    fun loadDynamicNews() {
        _state.value = UiState.Loading
        NewsDao.getDynamicNews(
            onSuccess = { _state.value = UiState.Success(it) },
            onError   = { _state.value = UiState.Error(it) }
        )
    }

}