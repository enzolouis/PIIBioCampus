import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piibiocampus.ui.auth.ResetPasswordUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResetPasswordViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val uiState: StateFlow<ResetPasswordUiState> = _uiState

    private var lastResetRequestTime = 0L
    private val RESET_COOLDOWN = 60_000L

    fun sendResetEmail(email: String) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastResetRequestTime

        if (elapsedTime < RESET_COOLDOWN) {
            val secondsLeft = ((RESET_COOLDOWN - elapsedTime) / 1000)
            _uiState.value = ResetPasswordUiState.Error(
                "Veuillez attendre $secondsLeft secondes avant une nouvelle demande"
            )
            return
        }

        lastResetRequestTime = currentTime
        _uiState.value = ResetPasswordUiState.Loading

        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = ResetPasswordUiState.EmailSent

                // Réactiver le cooldown après 60 secondes
                delay(RESET_COOLDOWN)
                _uiState.value = ResetPasswordUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ResetPasswordUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
}
