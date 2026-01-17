package com.example.piibiocampus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class UserDao {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun loginWithEmail(identifier: String, password: String): Result<FirebaseUser> {
        return try {
            // On suppose que identifier est toujours un email
            val authResult = auth.signInWithEmailAndPassword(identifier, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Utilisateur introuvable"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup( email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val user = result.user
                ?: return Result.failure(Exception("User null"))

            val userData = mapOf(
                "name" to name,
                "email" to email,
                "role" to "USER"
            )

            db.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getEmailByUsername(name: String): Result<String> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("name", name)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Result.failure(Exception("Utilisateur introuvable"))
            } else {
                // On prend le premier match
                val email = querySnapshot.documents[0].getString("email")
                if (email.isNullOrEmpty()) {
                    Result.failure(Exception("Email introuvable pour cet utilisateur"))
                } else {
                    Result.success(email)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
