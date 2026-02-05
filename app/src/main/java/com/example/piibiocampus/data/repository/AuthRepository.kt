package com.example.piibiocampus.data.repository

import com.example.piibiocampus.data.dao.UserDao
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> =
        runCatching {
            UserDao.login(email, password)
        }

    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<FirebaseUser> =
        runCatching {
            UserDao.createUser(email, password, username)
        }

    suspend fun getUserRole(uid: String): Result<String> =
        runCatching {
            UserDao.getUserRole(uid)
        }

    fun getCurrentUser(): FirebaseUser? =
        UserDao.getCurrentUser()

    fun signOut() =
        UserDao.signOut()
}
