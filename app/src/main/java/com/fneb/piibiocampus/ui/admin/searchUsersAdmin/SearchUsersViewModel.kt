package com.fneb.piibiocampus.ui.admin.searchUsersAdmin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import kotlinx.coroutines.launch

class SearchUsersAdminViewModel : ViewModel() {

    private val _usersState = MutableLiveData<UiState<List<UserProfile>>>(UiState.Idle)
    val usersState: LiveData<UiState<List<UserProfile>>> = _usersState

    // État pour les actions (Ban ou Changement de rôle) : contient (position, message de succès)
    private val _actionState = MutableLiveData<UiState<Pair<Int, String>>>(UiState.Idle)
    val actionState: LiveData<UiState<Pair<Int, String>>> = _actionState

    private val allUsersCache = mutableListOf<UserProfile>()
    private var currentQuery = ""

    fun loadAllUsers() {
        _usersState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = UserDao.getAllUsersAndAdmins()
                allUsersCache.clear()
                allUsersCache.addAll(result)
                applyFilter(currentQuery)
            } catch (e: Exception) {
                _usersState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
            }
        }
    }

    fun applyFilter(query: String) {
        currentQuery = query
        val filtered = if (currentQuery.isBlank()) {
            allUsersCache.toList()
        } else {
            allUsersCache.filter {
                it.name?.lowercase()?.contains(currentQuery.lowercase()) == true
            }
        }
        _usersState.value = UiState.Success(filtered)
    }

    fun banUser(user: UserProfile, position: Int) {
        viewModelScope.launch {
            try {
                UserDao.banUser(user.uid)
                allUsersCache.remove(user)
                _actionState.postValue(UiState.Success(Pair(position, "${user.name} a été banni.")))
            } catch (e: Exception) {
                _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
            }
        }
    }

    fun updateUserRole(user: UserProfile, newRole: String, position: Int) {
        UserDao.updateUserRole(
            userId = user.uid,
            newRole = newRole,
            onSuccess = {
                user.role = newRole
                _actionState.postValue(UiState.Success(Pair(position, "Rôle mis à jour")))
            },
            onError = { e ->
                _actionState.postValue(UiState.Error(FirebaseExceptionMapper.map(e)))
            }
        )
    }
}