package com.fneb.piibiocampus.ui.user.profiles.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── États UI ──────────────────────────────────────────────────────────────

    /** Chargement initial du profil */
    private val _profileState = MutableLiveData<UiState<UserProfile>>(UiState.Idle)
    val profileState: LiveData<UiState<UserProfile>> = _profileState

    /**
     * Nombre de photos de l'utilisateur.
     * Utilisé pour calculer les badges débloqués.
     */
    private val _photoCount = MutableLiveData<Int>(0)
    val photoCount: LiveData<Int> = _photoCount

    /** Résultat de la sauvegarde (profil + mot de passe) */
    private val _saveState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val saveState: LiveData<UiState<Unit>> = _saveState

    // ── Chargement ────────────────────────────────────────────────────────────

    init {
        loadProfile()
    }

    fun loadProfile() {
        _profileState.value = UiState.Loading
        scope.launch {
            try {
                val user    = UserDao.getCurrentUser() ?: throw AppException.NotAuthenticated()
                val profile = UserDao.getCurrentUserProfile() ?: throw AppException.DocumentNotFound()

                // Comptage des photos pour les badges débloqués
                val photos = PictureDao.getPicturesByUserEnrichedSortedByDate(user.uid)
                _photoCount.postValue(photos.size)

                _profileState.postValue(UiState.Success(profile))
            } catch (e: AppException) {
                _profileState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _profileState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    /**
     * Sauvegarde le profil complet :
     * 1. Upload photo de profil (si modifiée)
     * 2. Mise à jour nom + description
     * 3. Mise à jour badge (si sélectionné)
     * 4. Changement de mot de passe (si demandé)
     *
     * Les étapes sont séquentielles : une erreur sur l'une arrête les suivantes.
     */
    fun saveProfile(
        context: Context,
        name: String,
        description: String,
        selectedBadgeId: String,
        imageBytes: ByteArray?,
        oldPassword: String,
        newPassword: String
    ) {
        _saveState.value = UiState.Loading
        scope.launch {
            try {
                if (imageBytes != null) {
                    UserDao.uploadProfilePicture(context, imageBytes)
                }
                UserDao.updateUserProfile(name, description)
                if (selectedBadgeId.isNotEmpty()) {
                    UserDao.updateCurrentBadge(selectedBadgeId)
                }
                if (oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                    UserDao.updatePassword(oldPassword, newPassword)
                }
                _saveState.postValue(UiState.Success(Unit))
            } catch (e: AppException) {
                _saveState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _saveState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}

class EditProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditProfileViewModel() as T
    }
}