package com.fneb.piibiocampus.ui.auth

import com.fneb.piibiocampus.data.error.AppException

/**
 * États spécifiques au domaine authentification.
 *
 * Ce sealed class coexiste avec [com.fneb.piibiocampus.data.ui.UiState] :
 * il est nécessaire car auth a des états métier (EmailVerificationSent,
 * EmailNotVerified, Authenticated) que UiState<T> générique ne peut pas modéliser.
 *
 * La partie erreur est alignée sur [AppException] pour rester cohérente
 * avec le reste de l'application → les activities utilisent showError(exception).
 */
sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()

    /** Inscription réussie + email de vérification envoyé → afficher EmailSentDialog */
    object EmailVerificationSent : AuthUiState()

    /** Connexion tentée mais email non vérifié → un nouvel email a été renvoyé automatiquement */
    object EmailNotVerified : AuthUiState()

    /** Authentification réussie avec le rôle Firestore de l'utilisateur */
    data class Authenticated(val role: String) : AuthUiState()

    /**
     * Erreur Firebase ou réseau — utilise [AppException] comme partout dans l'app.
     * Dans l'Activity : showError(state.exception) ou toast(state.exception.userMessage)
     */
    data class Error(val exception: AppException) : AuthUiState()
}