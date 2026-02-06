package com.example.piibiocampus.ui.auth

import AuthUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piibiocampus.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
                    // récupérer le rôle puis émettre Authenticated
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
                .onSuccess {
                    _uiState.value = AuthUiState.Registered
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
