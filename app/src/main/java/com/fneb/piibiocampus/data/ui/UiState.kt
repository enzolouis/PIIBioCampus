package com.fneb.piibiocampus.data.ui

import com.fneb.piibiocampus.data.error.AppException

// ── UiState générique ─────────────────────────────────────────────────────────

/**
 * État UI générique utilisé dans tous les ViewModels.
 *
 * Usage dans un ViewModel :
 * ```kotlin
 * private val _state = MutableStateFlow<UiState<MyData>>(UiState.Idle)
 * val state: StateFlow<UiState<MyData>> = _state
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

// ── Extension utilitaire pour les Fragment/Activity ───────────────────────────

/**
 * Affiche le message d'erreur d'une [AppException] dans une Snackbar.
 * À appeler depuis le Fragment après avoir collecté un [UiState.Error].
 *
 * Exemple :
 * ```kotlin
 * is UiState.Error -> {
 *     showLoading(false)
 *     showError(state.exception)
 * }
 * ```
 */
fun androidx.fragment.app.Fragment.showError(
    exception: AppException,
    anchor: android.view.View = requireView()
) {
    com.google.android.material.snackbar.Snackbar
        .make(anchor, exception.userMessage, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
        .show()
}

// ── Exemple de ViewModel utilisant NetworkMonitor ─────────────────────────────
//
// Le lien entre NetworkMonitor et les erreurs DAO est simple :
//
//  1. NetworkMonitor détecte la perte réseau → le ViewModel peut afficher
//     un message AVANT même que Firebase échoue (proactif).
//
//  2. Quand Firebase échoue malgré tout → FirebaseExceptionMapper le traduit
//     en AppException.NetworkUnavailable (réactif).
//
// Le ViewModel combine les deux sans coupler NetworkMonitor aux DAO.
//
// ─────────────────────────────────────────────────────────────────────────────
//
// class ExampleViewModel(
//     private val networkMonitor: NetworkMonitor
// ) : ViewModel() {
//
//     private val _state = MutableStateFlow<UiState<MyData>>(UiState.Idle)
//     val state: StateFlow<UiState<MyData>> = _state
//
//     init {
//         // Observation proactive du réseau (optionnel, selon les écrans)
//         viewModelScope.launch {
//             networkMonitor.isOnline
//                 .filter { !it }            // ne réagit qu'à la perte réseau
//                 .collect {
//                     if (_state.value is UiState.Loading) {
//                         _state.value = UiState.Error(AppException.NetworkUnavailable())
//                     }
//                 }
//         }
//     }
//
//     fun loadData() {
//         viewModelScope.launch {
//             // Vérification proactive avant la requête
//             if (!networkMonitor.isCurrentlyOnline()) {
//                 _state.value = UiState.Error(AppException.NetworkUnavailable())
//                 return@launch
//             }
//             _state.value = UiState.Loading
//             try {
//                 val data = MyDao.fetchSomething()
//                 _state.value = UiState.Success(data)
//             } catch (e: AppException) {
//                 _state.value = UiState.Error(e)
//             }
//         }
//     }
// }