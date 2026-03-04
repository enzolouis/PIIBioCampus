package com.example.piibiocampus.data.dao

import com.example.piibiocampus.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserDao {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        return auth.signInWithEmailAndPassword(email, password)
            .await()
            .user
            ?: throw IllegalStateException("Utilisateur introuvable")
    }

    suspend fun createUser(
        email: String,
        password: String,
        name: String
    ): FirebaseUser {

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

        return user
    }

    suspend fun getUserRole(uid: String): String {
        val snapshot = db.collection("users")
            .document(uid)
            .get()
            .await()

        return snapshot.getString("role")
            ?: throw NoSuchElementException("Rôle introuvable")
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = auth.signOut()

    suspend fun getCurrentUserProfile(): UserProfile? {
        val currentUser = auth.currentUser ?: return null

        val snapshot = db.collection("users")
            .document(currentUser.uid)
            .get()
            .await()

        return snapshot.toObject(UserProfile::class.java)
    }


    suspend fun getUsersPage(limit: Long): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "USER")
            .orderBy("name")
            .limit(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }

    suspend fun getUsersPageAfter(lastName: String, limit: Long): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "USER")
            .orderBy("name")
            .startAfter(lastName)
            .limit(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }

    suspend fun searchUsers(query: String, limit: Long): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "USER")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }

    suspend fun searchUsersAfter(query: String, lastName: String, limit: Long): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "USER")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .startAfter(lastName)
            .limit(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }

    suspend fun banUser(uid: String) {
        val pictures = db.collection("pictures")
            .whereEqualTo("userRef", uid)
            .get()
            .await()

        for (doc in pictures.documents) {
            doc.reference.delete().await()
        }

        db.collection("users")
            .document(uid)
            .delete()
            .await()
    }


    suspend fun getAllUsersWithUid(): List<UserProfile> {
        val snapshot = db.collection("users")
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }
}
