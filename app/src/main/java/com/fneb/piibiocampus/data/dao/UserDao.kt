package com.fneb.piibiocampus.data.dao

import android.content.Context
import android.net.Uri
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

object UserDao {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * @throws AppException
     */
    suspend fun login(email: String, password: String): FirebaseUser {
        return try {
            auth.signInWithEmailAndPassword(email, password)
                .await()
                .user
                ?: throw AppException.NotAuthenticated()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun createUser(email: String, password: String, name: String): FirebaseUser {
        return try {
            val user = auth.createUserWithEmailAndPassword(email, password)
                .await()
                .user
                ?: throw AppException.NotAuthenticated()

            db.collection("users")
                .document(user.uid)
                .set(mapOf("name" to name, "email" to email, "role" to "USER"))
                .await()

            user
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun updatePassword(oldPassword: String, newPassword: String) {
        try {
            val user  = auth.currentUser ?: throw AppException.NotAuthenticated()
            val email = user.email       ?: throw AppException.NotAuthenticated()

            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, oldPassword)).await()
            } catch (e: Exception) {
                // L'échec de ré-auth doit être traduit en ReauthenticationFailed, pas en InvalidCredentials
                throw AppException.ReauthenticationFailed()
            }

            user.updatePassword(newPassword).await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = auth.signOut()

    // ── Lecture ───────────────────────────────────────────────────────────────

    /**
     * @throws AppException
     */
    suspend fun getUserRole(uid: String): String {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getString("role") ?: throw AppException.InvalidData()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * Retourne null si l'utilisateur n'est pas connecté (cas normal, pas une erreur).
     * @throws AppException pour toute erreur réseau ou Firestore
     */
    suspend fun getCurrentUserProfile(): UserProfile? {
        return try {
            val currentUser = auth.currentUser ?: return null
            db.collection("users").document(currentUser.uid).get().await()
                .toObject(UserProfile::class.java)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun getUserProfileById(userId: String): UserProfile? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.toObject(UserProfile::class.java)?.copy(uid = snapshot.id)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun getAllUsersWithUid(): List<UserProfile> {
        return try {
            db.collection("users").orderBy("name").get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun getAllUsers(): List<UserProfile> {
        return try {
            db.collection("users").whereEqualTo("role", "USER").orderBy("name").get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    /**
     * @throws AppException
     */
    suspend fun updateUserProfile(name: String, description: String) {
        try {
            val user = auth.currentUser ?: throw AppException.NotAuthenticated()
            db.collection("users")
                .document(user.uid)
                .update(mapOf("name" to name, "description" to description))
                .await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun updateCurrentBadge(badgeId: String) {
        try {
            val user = auth.currentUser ?: throw AppException.NotAuthenticated()
            db.collection("users").document(user.uid).update("currentBadge", badgeId).await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * @throws AppException
     */
    suspend fun uploadProfilePicture(context: Context, imageBytes: ByteArray): String {
        return try {
            val user = auth.currentUser ?: throw AppException.NotAuthenticated()
            val webpFile = try {
                PictureDao.bytesToWebpFile(context, imageBytes)
            } catch (e: Exception) {
                throw AppException.ImageProcessingError(e)
            }
            val ref = FirebaseStorage.getInstance()
                .reference.child("profile_pictures/${user.uid}.webp")
            val metadata = StorageMetadata.Builder().setContentType("image/webp").build()
            ref.putFile(Uri.fromFile(webpFile), metadata).await()
            val url = ref.downloadUrl.await().toString()
            db.collection("users").document(user.uid).update("profilePictureUrl", url).await()
            webpFile.delete()
            url
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    /**
     * Supprime toutes les photos puis le compte Firestore d'un utilisateur (action admin).
     * @throws AppException
     */
    suspend fun banUser(uid: String) {
        try {
            val pictures = db.collection("pictures").whereEqualTo("userRef", uid).get().await()
            for (doc in pictures.documents) doc.reference.delete().await()
            db.collection("users").document(uid).delete().await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    /**
     * Supprime le compte de l'utilisateur actuellement connecté.
     * @throws AppException
     */
    suspend fun deleteCurrentUser() {
        try {
            val user = auth.currentUser ?: throw AppException.NotAuthenticated()
            banUser(user.uid)
            user.delete().await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Données brutes pour l'export CSV — ne lance pas d'exception (retourne une map vide si non connecté).
     * @throws AppException uniquement sur erreur Firestore
     */
    suspend fun getCurrentUserDataForExport(): Map<String, String> {
        val user = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email"             to (user.email ?: ""),
            "name"              to (profile.name ?: ""),
            "description"       to (profile.description ?: ""),
            "profilePictureUrl" to (profile.profilePictureUrl ?: "")
        )
    }

    suspend fun getCurrentUserData(): Map<String, String> {
        val user = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email"             to (user.email ?: ""),
            "name"              to (profile.name),
            "description"       to (profile.description),
            "profilePictureUrl" to (profile.profilePictureUrl)
        )
    }
}