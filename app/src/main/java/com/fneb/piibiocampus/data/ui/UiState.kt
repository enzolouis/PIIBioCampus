package com.fneb.piibiocampus.data.ui

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.data.error.AppException

// ── UiState générique ─────────────────────────────────────────────────────────

/**
 * État UI générique utilisé dans tous les ViewModels.
 *
 * Usage dans un ViewModel :
 * ```kotlin
 * private val _state = MutableLiveData<UiState<MyData>>(UiState.Idle)
 * val state: LiveData<UiState<MyData>> = _state
 * ```
 */
sealed class UiState<out T> {
    /** État initial, aucune opération en cours */
    data object Idle : UiState<Nothing>()

    /** Opération en cours */
    data object Loading : UiState<Nothing>()

    /** Succès avec données */
    data class Success<T>(val data: T) : UiState<T>()

    /** Erreur métier */
    data class Error(val exception: AppException) : UiState<Nothing>()
}

// ── Extensions showError ──────────────────────────────────────────────────────

/**
 * Affiche le message d'erreur d'une [AppException] dans une Snackbar.
 * Version Fragment — à appeler depuis un Fragment après avoir collecté un [UiState.Error].
 *
 * ```kotlin
 * is UiState.Error -> { showLoading(false); showError(state.exception) }
 * ```
 */
fun Fragment.showError(exception: AppException) {
    Toast.makeText(requireContext(), exception.userMessage, Toast.LENGTH_LONG).show()
}

/**
 * Affiche le message d'erreur d'une [AppException] dans un Toast.
 * Version Activity.
 */
fun AppCompatActivity.showError(exception: AppException) {
    Toast.makeText(this, exception.userMessage, Toast.LENGTH_LONG).show()
}