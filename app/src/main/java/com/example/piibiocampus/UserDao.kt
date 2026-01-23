package com.example.piibiocampus


import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


/**
 * Data access for user operations.
 * Constructor parameters allow injecting mocks for testing.
 */
object UserDao{
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }


    suspend fun loginWithEmail(identifier: String, password: String): Result<FirebaseUser> =
        runCatching {
            val user = auth.signInWithEmailAndPassword(identifier, password)
                .await()
                .user
                ?: throw IllegalStateException("Utilisateur introuvable")
            user
        }


    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> =
        runCatching {
            val user = auth.createUserWithEmailAndPassword(email, password)
                .await()
                .user
                ?: throw IllegalStateException("Utilisateur null")

            val userData = mapOf(
                "name" to name,
                "email" to email,
                "role" to "USER"
            )

            db.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            user
        }


    fun getCurrentUser(): FirebaseUser? = auth.currentUser


    suspend fun getRoleByUid(uid: String): Result<String> = runCatching {
        val snapshot = db.collection("users")
            .document(uid)
            .get()
            .await()

        snapshot.getString("role") ?: throw NoSuchElementException("Rôle introuvable")
    }


    fun signOut() = auth.signOut()
}