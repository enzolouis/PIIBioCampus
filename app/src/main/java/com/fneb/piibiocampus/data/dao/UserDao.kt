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

    suspend fun login(email: String, password: String): FirebaseUser {
        return try {
            if (isEmailBanned(email)) throw AppException.AccountBanned()

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

    suspend fun createUser(email: String, password: String, name: String): FirebaseUser {
        return try {
            if (isEmailBanned(email)) throw AppException.AccountBanned()

            val user = auth.createUserWithEmailAndPassword(email, password)
                .await()
                .user
                ?: throw AppException.NotAuthenticated()

            db.collection("users")
                .document(user.uid)
                .set(mapOf(
                    "name"              to name,
                    "description"       to "Je débute ici !",
                    "profilePictureUrl" to "https://firebasestorage.googleapis.com/v0/b/piibiocampus-c8f50.firebasestorage.app/o/defaultProfilePicture%2FdefaultProfilePicture.png?alt=media&token=9ec98653-3c8d-4795-854f-f5024d51e145",
                    "email"             to email,
                    "role"              to "USER",
                    "currentBadge"      to ""
                ))
                .await()

            user
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String) {
        try {
            val user  = auth.currentUser ?: throw AppException.NotAuthenticated()
            val email = user.email       ?: throw AppException.NotAuthenticated()
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, oldPassword)).await()
            } catch (e: Exception) {
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

    // ── Liste noire ───────────────────────────────────────────────────────────

    /**
     * Convertit l'email en clé Firestore valide.
     * Ex : "user@example.com" → "user_at_example_com"
     */
    private fun emailToKey(email: String): String =
        email.lowercase().replace("@", "_at_").replace(".", "_")

    /**
     * Vérifie si un email est dans la liste noire.
     */
    suspend fun isEmailBanned(email: String): Boolean {
        return try {
            val doc = db.collection("banned_emails")
                .document(emailToKey(email))
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            // En cas d'erreur réseau, on laisse passer (fail open)
            // pour ne pas bloquer les utilisateurs légitimes
            false
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

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

    fun updateUserRole(
        userId: String,
        newRole: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .update("role", newRole)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

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

    suspend fun getAllUsersAndAdmins(): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereIn("role", listOf("USER", "ADMIN"))
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        }.sortedWith(
            compareBy(
                { if (it.role == "ADMIN") 0 else 1 },
                { it.name }
            )
        )
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

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

    suspend fun uploadProfilePicture(context: Context, imageBytes: ByteArray): String {
        return try {
            val user = auth.currentUser ?: throw AppException.NotAuthenticated()
            val webpFile = try {
                PictureDao.bytesToWebpFile(context, imageBytes)
            } catch (e: Exception) {
                throw AppException.ImageProcessingError(e)
            }
            val ref = FirebaseStorage.getInstance()
                .reference.child("profile_pictures/${user.uid}/avatar.webp")
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

    // ── Suppression / Ban ─────────────────────────────────────────────────────

    suspend fun banUser(uid: String) {
        try {
            val userDoc = db.collection("users").document(uid).get().await()
            val email   = userDoc.getString("email") ?: ""

            // ✅ Liste noire par email (empêche la recréation de compte)
            if (email.isNotEmpty()) {
                db.collection("banned_emails")
                    .document(emailToKey(email))
                    .set(mapOf("email" to email, "bannedAt" to com.google.firebase.Timestamp.now()))
                    .await()
            }

            // ✅ Liste noire par UID (bloque les opérations Firestore en temps réel)
            db.collection("banned_users")
                .document(uid)
                .set(mapOf("bannedAt" to com.google.firebase.Timestamp.now()))
                .await()

            val pictures = db.collection("pictures").whereEqualTo("userRef", uid).get().await()
            for (doc in pictures.documents) doc.reference.delete().await()
            db.collection("users").document(uid).delete().await()

        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e)
        }
    }

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

    suspend fun getCurrentUserDataForExport(): Map<String, String> {
        val user    = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email"             to (user.email ?: ""),
            "name"              to (profile.name ?: ""),
            "description"       to (profile.description ?: ""),
            "profilePictureUrl" to (profile.profilePictureUrl ?: "")
        )
    }

    suspend fun updateUserProfileName(name: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update(mapOf("name" to name)).await()
    }

    suspend fun getCurrentUserData(): Map<String, String> {
        val user    = auth.currentUser ?: return emptyMap()
        val profile = getCurrentUserProfile() ?: return emptyMap()
        return mapOf(
            "email"             to (user.email ?: ""),
            "name"              to profile.name,
            "description"       to profile.description,
            "profilePictureUrl" to profile.profilePictureUrl
        )
    }
}