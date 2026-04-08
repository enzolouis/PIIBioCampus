package com.fneb.piibiocampus.data.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fneb.piibiocampus.data.error.AppException
import com.google.android.material.snackbar.Snackbar

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
fun androidx.fragment.app.Fragment.showError(
    exception: AppException,
    anchor: View = requireView()
) {
    Snackbar.make(anchor, exception.userMessage, Snackbar.LENGTH_LONG).show()
}

/**
 * Affiche le message d'erreur d'une [AppException] dans une Snackbar.
 * Version Activity — à appeler depuis une AppCompatActivity après avoir collecté un [UiState.Error].
 *
 * ```kotlin
 * is UiState.Error -> { showLoading(false); showError(state.exception) }
 * ```
 */
fun AppCompatActivity.showError(
    exception: AppException,
    anchor: View = findViewById(android.R.id.content)
) {
    Snackbar.make(anchor, exception.userMessage, Snackbar.LENGTH_LONG).show()
}