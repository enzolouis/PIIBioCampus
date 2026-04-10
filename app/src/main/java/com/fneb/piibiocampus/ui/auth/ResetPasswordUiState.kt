package com.fneb.piibiocampus.ui.auth

import com.fneb.piibiocampus.data.error.AppException

sealed class ResetPasswordUiState {
    object Idle      : ResetPasswordUiState()
    object Loading   : ResetPasswordUiState()
    object EmailSent : ResetPasswordUiState()

    /** Erreur Firebase/réseau — userMessage garanti via AppException */
    data class Error(val exception: AppException) : ResetPasswordUiState()

    /**
     * Cooldown local (règle métier, pas une erreur Firebase).
     * Séparé de Error car il porte le décompte typé, pas un AppException.
     */
    data class CooldownError(val secondsLeft: Long) : ResetPasswordUiState()
}