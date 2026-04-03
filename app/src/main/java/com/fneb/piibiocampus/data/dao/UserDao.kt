package com.fneb.piibiocampus.data.dao

import com.fneb.piibiocampus.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

    suspend fun getUserProfileById(userId: String): UserProfile? {
        val snapshot = db.collection("users")
            .document(userId)
            .get()
            .await()

        return snapshot.toObject(UserProfile::class.java)?.copy(uid = snapshot.id)
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

    suspend fun deleteCurrentUser() {
        val user = auth.currentUser ?: throw IllegalStateException("Aucun utilisateur connecté")
        val uid = user.uid
        banUser(uid)
        user.delete().await()
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

    suspend fun getAllUsers(): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "USER")
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }
    }

    /**
    (CGU) Récupérer les données d'un profil pour le sauvegarde CGU des données
     */
    suspend fun getCurrentUserDataForExport(): Map<String, String> {
        val user = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email" to (user.email ?: ""),
            "name" to (profile.name ?: ""),
            "description" to (profile.description ?: ""),
            "profilePictureUrl" to (profile.profilePictureUrl ?: "")
        )
    }

    /**
     * Update profile (concerne les 3 prochaines fonctions)
     */

    suspend fun updateUserProfile(name: String, description: String) {
        val user = auth.currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .update(mapOf("name" to name, "description" to description))
            .await()
    }

    suspend fun updateCurrentBadge(badgeId: String) {
        val user = auth.currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .update("currentBadge", badgeId)
            .await()
    }

    suspend fun uploadProfilePicture(context: android.content.Context, imageBytes: ByteArray): String {
        val user = auth.currentUser ?: throw IllegalStateException("Utilisateur non connecté")
        val webpFile = PictureDao.bytesToWebpFile(context, imageBytes)
        val ref = FirebaseStorage.getInstance().reference.child("profile_pictures/${user.uid}/avatar.webp")
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/webp")
            .build()
        ref.putFile(android.net.Uri.fromFile(webpFile), metadata).await()
        val url = ref.downloadUrl.await().toString()
        db.collection("users").document(user.uid).update("profilePictureUrl", url).await()
        webpFile.delete()
        return url
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser ?: throw IllegalStateException("Utilisateur non connecté")
        val email = user.email ?: throw IllegalStateException("Email introuvable")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun getCurrentUserData(): Map<String, String> {
        val user = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email" to (user.email ?: ""),
            "name" to (profile.name),
            "description" to (profile.description),
            "profilePictureUrl" to (profile.profilePictureUrl)
        )
    }
}
