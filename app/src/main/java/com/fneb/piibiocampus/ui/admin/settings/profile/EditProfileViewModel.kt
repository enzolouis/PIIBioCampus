package com.fneb.piibiocampus.ui.admin.settings.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {

    // ── Chargement du profil ──────────────────────────────────────────────────

    private val _profileState = MutableLiveData<UiState<String>>(UiState.Idle)
    val profileState: LiveData<UiState<String>> = _profileState

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private val _saveState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val saveState: LiveData<UiState<Unit>> = _saveState

    // ── Chargement ────────────────────────────────────────────────────────────

    fun loadProfile() {
        _profileState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val profile = UserDao.getCurrentUserProfile()
                if (profile != null) {
                    _profileState.value = UiState.Success(profile.name)
                } else {
                    _profileState.value = UiState.Error(AppException.NotAuthenticated())
                }
            } catch (e: AppException) {
                _profileState.value = UiState.Error(e)
            } catch (e: Exception) {
                _profileState.value = UiState.Error(FirebaseExceptionMapper.map(e))
            }
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    /**
     * @param oldPassword null si pas de changement de mot de passe demandé
     * @param newPassword null si pas de changement de mot de passe demandé
     */
    fun saveProfile(name: String, oldPassword: String?, newPassword: String?) {
        _saveState.value = UiState.Loading
        viewModelScope.launch {
            try {
                UserDao.updateUserProfileName(name)
                if (oldPassword != null && newPassword != null) {
                    UserDao.updatePassword(oldPassword, newPassword)
                }
                _saveState.value = UiState.Success(Unit)
            } catch (e: AppException) {
                _saveState.value = UiState.Error(e)
            } catch (e: Exception) {
                _saveState.value = UiState.Error(FirebaseExceptionMapper.map(e))
            }
        }
    }
}