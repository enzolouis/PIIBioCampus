package com.fneb.piibiocampus.ui.auth

import AuthUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            repository.login(email, password)
                .onSuccess { user ->
                    try {
                        user.reload().await()
                    } catch (e: Exception) {
                        _uiState.value = AuthUiState.Error(e)
                        return@onSuccess
                    }

                    if (!user.isEmailVerified) {
                        try {
                            user.sendEmailVerification().await()
                        } catch (_: Exception) { }
                        repository.signOut()
                        _uiState.value = AuthUiState.EmailNotVerified
                        return@onSuccess
                    }

                    repository.getUserRole(user.uid)
                        .onSuccess { role ->
                            _uiState.value = AuthUiState.Authenticated(role)
                        }
                        .onFailure { err ->
                            _uiState.value = AuthUiState.Error(err)
                        }
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(err)
                }
        }
    }

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
                        _uiState.value = AuthUiState.Error(e)
                    }
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(err)
                }
        }
    }

    fun checkCurrentUserAndFetchRoleIfNeeded() {
        val user = repository.getCurrentUser()

        if (user == null) {
            _uiState.value = AuthUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.getUserRole(user.uid)
                .onSuccess { role ->
                    _uiState.value = AuthUiState.Authenticated(role)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(err)
                }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}
