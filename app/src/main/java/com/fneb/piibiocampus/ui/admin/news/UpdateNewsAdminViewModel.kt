package com.fneb.piibiocampus.ui.admin.news

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.ui.UiState

class UpdateNewsAdminViewModel : ViewModel() {

    // On précise UiState<String> pour le message de succès
    private val _actionState = MutableLiveData<UiState<String>>(UiState.Idle)
    val actionState: LiveData<UiState<String>> = _actionState

    var newsId: String = ""
    var behavior: String? = null
    var order: Int = 0
    var status: String? = null
    var currentImageUrl: String = ""

    fun deleteNews() {
        if (newsId.isEmpty()) return
        _actionState.value = UiState.Loading

        NewsDao.deleteNews(
            newsId = newsId,
            onSuccess = {
                _actionState.postValue(UiState.Success("Suppression effectuée"))
            },
            onError = { e ->
                // On utilise le Mapper pour transformer l'Exception en AppException
                _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
            }
        )
    }

    fun saveNews(context: Context, title: String, source: String, imageUri: Uri?) {
        if (title.isBlank()) {
            // Ici on crée directement une erreur métier connue
            _actionState.value = UiState.Error(AppException.InvalidData())
            return
        }

        _actionState.value = UiState.Loading

        if (status == "update") {
            handleUpdate(context, title, source, imageUri)
        } else {
            handleCreate(context, title, source, imageUri)
        }
    }

    private fun handleUpdate(context: Context, title: String, source: String, imageUri: Uri?) {
        if (imageUri != null) {
            NewsDao.uploadNewsImage(
                context = context,
                imageUri = imageUri,
                onSuccess = { url -> updateInfos(title, source, url) },
                onError = { e -> _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e))) }
            )
        } else {
            updateInfos(title, source, currentImageUrl)
        }
    }

    private fun handleCreate(context: Context, title: String, source: String, imageUri: Uri?) {
        if (imageUri == null) {
            _actionState.value = UiState.Error(AppException.ImageProcessingError())
            return
        }

        NewsDao.uploadNewsImage(
            context = context,
            imageUri = imageUri,
            onSuccess = { url ->
                NewsDao.createNews(
                    titre = title,
                    imageUrl = url,
                    source = source,
                    behavior = behavior,
                    order = order,
                    onSuccess = { _actionState.postValue(UiState.Success("Création réussie")) },
                    onError = { e -> _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e))) }
                )
            },
            onError = { e -> _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e))) }
        )
    }

    private fun updateInfos(title: String, source: String, url: String) {
        NewsDao.updateNews(
            newsId = newsId,
            title = title,
            source = source,
            imageUrl = url,
            onSuccess = { _actionState.postValue(UiState.Success("Mise à jour effectuée")) },
            onError = { e -> _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e))) }
        )
    }
}