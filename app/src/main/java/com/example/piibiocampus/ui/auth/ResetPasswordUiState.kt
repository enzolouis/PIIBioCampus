package com.example.piibiocampus.ui.auth

sealed class ResetPasswordUiState {
    object Idle : ResetPasswordUiState()
    object Loading : ResetPasswordUiState()
    object EmailSent : ResetPasswordUiState()
    data class Error(val message: String) : ResetPasswordUiState()
}
