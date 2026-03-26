sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()

    /** Inscription réussie + email de vérification envoyé → rediriger vers connexion */
    object EmailVerificationSent : AuthUiState()

    /** Connexion tentée mais email non vérifié → un nouvel email a été renvoyé automatiquement */
    object EmailNotVerified : AuthUiState()

    data class Authenticated(val role: String) : AuthUiState()
    data class Error(val throwable: Throwable) : AuthUiState()
}