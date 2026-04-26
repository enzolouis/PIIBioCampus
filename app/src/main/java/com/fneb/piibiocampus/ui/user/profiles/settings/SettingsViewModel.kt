package com.fneb.piibiocampus.ui.user.profiles.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.data.dao.ExportDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── États UI ──────────────────────────────────────────────────────────────

    /** Export CSV : Loading pendant l'export, Success(fileName) quand terminé */
    private val _exportState = MutableLiveData<UiState<String>>(UiState.Idle)
    val exportState: LiveData<UiState<String>> = _exportState

    /**
     * Suppression de compte : Success déclenche la navigation vers ConnectionActivity.
     * Idle entre chaque tentative pour éviter de rejouer la navigation au retour.
     */
    private val _deleteState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val deleteState: LiveData<UiState<Unit>> = _deleteState

    // ── Actions ───────────────────────────────────────────────────────────────

    fun exportData(context: Context) {
        _exportState.value = UiState.Loading
        scope.launch {
            try {
                val uid = UserDao.getCurrentUser()?.uid ?: throw AppException.NotAuthenticated()
                val fileName = ExportDao.exportUserDataAsCsv(context, uid)
                _exportState.postValue(UiState.Success(fileName))
            } catch (e: AppException) {
                _exportState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _exportState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    /**
     * Déconnexion synchrone — pas besoin d'UiState,
     * c'est le Fragment qui navigue immédiatement après.
     */
    fun signOut() = UserDao.signOut()

    fun deleteAccount(password: String) {
        _deleteState.value = UiState.Loading
        scope.launch {
            try {
                UserDao.deleteCurrentUser(password)
                _deleteState.postValue(UiState.Success(Unit))
            } catch (e: AppException) {
                _deleteState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _deleteState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    /** Remet deleteState à Idle après navigation pour éviter de rejouer l'effet. */
    fun resetDeleteState() {
        _deleteState.value = UiState.Idle
    }

    /** Remet exportState à Idle après affichage du message de succès. */
    fun resetExportState() {
        _exportState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}

class SettingsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel() as T
    }
}