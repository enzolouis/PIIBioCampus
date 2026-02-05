sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Registered : AuthUiState()
    data class Authenticated(val role: String) : AuthUiState()
    data class Error(val throwable: Throwable) : AuthUiState()
}
