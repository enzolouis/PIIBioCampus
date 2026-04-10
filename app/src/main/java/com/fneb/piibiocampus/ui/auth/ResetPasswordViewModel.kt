package com.fneb.piibiocampus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResetPasswordViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val uiState: StateFlow<ResetPasswordUiState> = _uiState

    private var lastResetRequestTime = 0L
    private val RESET_COOLDOWN = 60_000L

    fun sendResetEmail(email: String) {
        val elapsed = System.currentTimeMillis() - lastResetRequestTime

        if (elapsed < RESET_COOLDOWN) {
            _uiState.value = ResetPasswordUiState.CooldownError((RESET_COOLDOWN - elapsed) / 1000)
            return
        }

        lastResetRequestTime = System.currentTimeMillis()
        _uiState.value = ResetPasswordUiState.Loading

        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = ResetPasswordUiState.EmailSent
                delay(RESET_COOLDOWN)
                _uiState.value = ResetPasswordUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ResetPasswordUiState.Error(FirebaseExceptionMapper.map(e))
            }
        }
    }
}