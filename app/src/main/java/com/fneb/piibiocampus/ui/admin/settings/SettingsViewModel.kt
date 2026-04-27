package com.fneb.piibiocampus.ui.admin.settings

import android.app.ActivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.dao.ExportDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Événements ponctuels (navigation, non rejouables) ─────────────────────────

sealed class SettingsEvent {
    /** Déconnexion ou suppression réussie → naviguer vers ConnectionActivity */
    object NavigateToLogin : SettingsEvent()
}

class SettingsViewModel : ViewModel() {

    // ── Rôle utilisateur (pour masquer la ligne "supprimer" si SUPER_ADMIN) ──

    private val _isSuperAdmin = MutableLiveData(false)
    val isSuperAdmin: LiveData<Boolean> = _isSuperAdmin

    // ── Suppression du compte ─────────────────────────────────────────────────

    private val _deleteState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val deleteState: LiveData<UiState<Unit>> = _deleteState

    // ── Export CSV ────────────────────────────────────────────────────────────

    private val _exportState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val exportState: LiveData<UiState<Unit>> = _exportState

    // ── Événements ponctuels ──────────────────────────────────────────────────

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events

    // ── Chargement initial ────────────────────────────────────────────────────

    fun loadUserRole() {
        viewModelScope.launch {
            try {
                val profile = UserDao.getCurrentUserProfile()
                _isSuperAdmin.value = profile?.role == "SUPER_ADMIN"
            } catch (_: Exception) {
                // Non bloquant : si on ne peut pas charger le rôle,
                // on laisse la ligne supprimer visible par sécurité
                _isSuperAdmin.value = false
            }
        }
    }

    // ── Déconnexion ───────────────────────────────────────────────────────────

    fun signOut(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                UserDao.signOut()
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.clearApplicationUserData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _events.emit(SettingsEvent.NavigateToLogin)
                }
            }
        }
    }

    // ── Suppression du compte ─────────────────────────────────────────────────

    fun deleteAccount(password: String) {
        _deleteState.value = UiState.Loading
        viewModelScope.launch {
            try {
                UserDao.deleteCurrentUser(password)
                _deleteState.value = UiState.Success(Unit)
                _events.emit(SettingsEvent.NavigateToLogin)
            } catch (e: AppException) {
                _deleteState.value = UiState.Error(e)
            } catch (e: Exception) {
                _deleteState.value = UiState.Error(FirebaseExceptionMapper.map(e))
            }
        }
    }

    // ── Export des données ────────────────────────────────────────────────────

    fun exportData(context: Context) {
        _exportState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val uid = UserDao.getCurrentUser()?.uid
                    ?: throw AppException.NotAuthenticated()
                ExportDao.exportUserDataAsCsv(context, uid)
                _exportState.value = UiState.Success(Unit)
            } catch (e: AppException) {
                _exportState.value = UiState.Error(e)
            } catch (e: Exception) {
                _exportState.value = UiState.Error(FirebaseExceptionMapper.map(e))
            }
        }
    }
}