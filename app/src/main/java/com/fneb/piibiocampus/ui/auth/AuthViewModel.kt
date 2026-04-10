package com.fneb.piibiocampus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // ── Connexion ─────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            repository.login(email, password)
                .onSuccess { user ->
                    try {
                        user.reload().await()
                    } catch (e: Exception) {
                        _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(e))
                        return@onSuccess
                    }

                    if (!user.isEmailVerified) {
                        try { user.sendEmailVerification().await() } catch (_: Exception) { }
                        repository.signOut()
                        _uiState.value = AuthUiState.EmailNotVerified
                        return@onSuccess
                    }

                    repository.getUserRole(user.uid)
                        .onSuccess { role ->
                            _uiState.value = AuthUiState.Authenticated(role)
                        }
                        .onFailure { err ->
                            // map(Throwable) : nouvelle surcharge de FirebaseExceptionMapper
                            _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(err))
                        }
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(err))
                }
        }
    }

    // ── Inscription ───────────────────────────────────────────────────────────

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            repository.register(email, password, username)
                .onSuccess { user ->
                    try {
                        user.sendEmailVerification().await()
                        repository.signOut()
                        _uiState.value = AuthUiState.EmailVerificationSent
                    } catch (e: Exception) {
                        repository.signOut()
                        _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(e))
                    }
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(err))
                }
        }
    }

    // ── Session existante ─────────────────────────────────────────────────────

    fun checkCurrentUserAndFetchRoleIfNeeded() {
        val user = repository.getCurrentUser()
        if (user == null) { _uiState.value = AuthUiState.Idle; return }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.getUserRole(user.uid)
                .onSuccess { role -> _uiState.value = AuthUiState.Authenticated(role) }
                .onFailure { err  -> _uiState.value = AuthUiState.Error(FirebaseExceptionMapper.map(err)) }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}