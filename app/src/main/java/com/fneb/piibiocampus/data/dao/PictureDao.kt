package com.fneb.piibiocampus.data.dao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.LocationMeta
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Date
import java.util.UUID
import kotlin.math.*
import kotlinx.coroutines.tasks.await

object PictureDao {

    private val storage = FirebaseStorage.getInstance()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    private val picturesRef = firestore.collection("pictures")
    private val storageRef  = storage.reference

    /**
     * Télécharge une image depuis son URL Firebase Storage et retourne ses bytes.
     */
    fun downloadImageBytes(
        imageUrl: String,
        onSuccess: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Thread {
            try {
                val bytes = URL(imageUrl).readBytes()
                onSuccess(bytes)
            } catch (e: Exception) {
                onError(FirebaseExceptionMapper.map(e))
            }
        }.start()
    }

    fun listenToPicturesByUserEnrichedSortedByDate(
        userId: String,
        onUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (AppException) -> Unit
    ): ListenerRegistration {
        return firestore.collection("pictures")
            .whereEqualTo("userRef", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(FirebaseExceptionMapper.map(error)); return@addSnapshotListener
                }
                val pictures = snapshot?.documents?.mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    val map  = data.toMutableMap()
                    map["id"] = document.id
                    map
                } ?: emptyList()
                enrichPicturesWithCensusData(
                    pictures,
                    onSuccess = { onUpdate(it) },
                    onError   = { e -> onError(FirebaseExceptionMapper.map(e)) }
                )
            }
    }

    fun getAllPicturesEnriched(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef.get()
            .addOnSuccessListener { snapshot ->
                val pictures = snapshot.documents.mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    val map  = data.toMutableMap()
                    map["id"] = document.id
                    map
                }
                enrichPicturesWithCensusData(pictures, onSuccess, onError)
            }
            .addOnFailureListener(onError)
    }

    fun getAllPicturesEnriched(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        picturesRef.get()
            .addOnSuccessListener { snapshot ->
                val pictures = snapshot.documents.mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    val map  = data.toMutableMap()
                    map["id"] = document.id
                    map
                }
                enrichPicturesWithCensusData(pictures, onSuccess, onError)
            }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun getPicturesNearLocation(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        picturesRef.get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { document ->
                    val data     = document.data ?: return@mapNotNull null
                    val location = data["location"] as? Map<*, *> ?: return@mapNotNull null
                    val lat      = location["latitude"]  as? Double ?: return@mapNotNull null
                    val lon      = location["longitude"] as? Double ?: return@mapNotNull null
                    if (distanceInMeters(centerLat, centerLon, lat, lon) <= radiusMeters) {
                        val map = data.toMutableMap()
                        map["id"] = document.id
                        map
                    } else null
                }
                onSuccess(result)
            }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun getPicturesNearLocationEnriched(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        getPicturesNearLocation(centerLat, centerLon, radiusMeters,
            onSuccess = { enrichPicturesWithCensusData(it, onSuccess, onError) },
            onError   = onError
        )
    }

    fun exportPictureFromBytes(
        context: Context,
        imageBytes: ByteArray,
        location: LocationMeta,
        censusRef: String?,
        recordingStatus: Boolean,
        adminValidated: Boolean = false,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: run { onError(IllegalStateException("Utilisateur non connecté.")); return }

            val uid       = currentUser.uid
            val webpFile  = bytesToWebpFile(context, imageBytes)
            val pictureId = picturesRef.document().id
            val picRef    = storageRef.child("$pictureId.webp")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .setCustomMetadata("userRef", uid)
                .setCustomMetadata("uploadDate", Date().toString())
                .build()

            picRef.putFile(Uri.fromFile(webpFile), metadata)
                .addOnSuccessListener {
                    picRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        val data = hashMapOf(
                            "imageUrl"        to downloadUrl.toString(),
                            "timestamp"       to Date(),
                            "location"        to mapOf(
                                "latitude"  to location.latitude,
                                "longitude" to location.longitude,
                                "altitude"  to location.altitude
                            ),
                            "censusRef"       to (censusRef ?: ""),
                            "userRef"         to uid,
                            "recordingStatus" to recordingStatus,
                            "adminValidated"  to adminValidated
                        )
                        picturesRef.document(pictureId)
                            .set(data)
                            .addOnSuccessListener { webpFile.delete(); onSuccess() }
                            .addOnFailureListener(onError)
                    }.addOnFailureListener(onError)
                }
                .addOnFailureListener { e -> webpFile.delete(); onError(FirebaseExceptionMapper.map(e)) }

        } catch (e: Exception) {
            onError(FirebaseExceptionMapper.map(e))
        }
    }

    fun updatePictureCensus(
        pictureId: String,
        censusRef: String?,
        recordingStatus: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "censusRef"       to (censusRef ?: ""),
            "recordingStatus" to recordingStatus
        )
        picturesRef.document(pictureId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun setAdminValidated(
        pictureId: String,
        validated: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef.document(pictureId)
            .update("adminValidated", validated)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun deletePicture(
        pictureId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef.document(pictureId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    // ── Upload image pour l'arbre de recensement ──────────────────────────────

    /**
     * Convertit [imageBytes] en WebP, l'upload dans Storage sous
     * `census_images/{uuid}.webp` et retourne l'URL de téléchargement.
     * Aucun document Firestore n'est créé (usage exclusif pour les nœuds de recensement).
     */
    fun uploadImageForCensus(
        context: Context,
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (AppException) -> Unit
    ) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: run { onError(AppException.NotAuthenticated()); return }

            val webpFile = try {
                bytesToWebpFile(context, imageBytes)
            } catch (e: Exception) {
                onError(AppException.ImageProcessingError(e)); return
            }

            val imageId  = UUID.randomUUID().toString()
            val ref      = storageRef.child("$imageId.webp")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .setCustomMetadata("userRef",    currentUser.uid)
                .setCustomMetadata("uploadDate", Date().toString())
                .build()

            ref.putFile(Uri.fromFile(webpFile), metadata)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            webpFile.delete()
                            onSuccess(uri.toString())
                        }
                        .addOnFailureListener { e ->
                            webpFile.delete()
                            onError(FirebaseExceptionMapper.map(e))
                        }
                }
                .addOnFailureListener { e ->
                    webpFile.delete()
                    onError(FirebaseExceptionMapper.map(e))
                }
        } catch (e: AppException) {
            onError(e)
        } catch (e: Exception) {
            onError(FirebaseExceptionMapper.map(e))
        }
    }

    // ── Enrichissement ────────────────────────────────────────────────────────

    private data class TaxonomyInfo(
        val ordre: String   = "Non identifié",
        val famille: String = "Non identifié",
        val genre: String   = "Non identifié",
        val espece: String  = "Non identifié",
        val type: String    = ""
    )

    private fun enrichPicturesWithCensusData(
        pictures: List<Map<String, Any>>,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        if (pictures.isEmpty()) { onSuccess(emptyList()); return }

        CensusDao.fetchCensusTree(
            onComplete = { censusNodes ->
                val taxonomyMap = mutableMapOf<String, TaxonomyInfo>()
                censusNodes.forEach { ordre ->
                    taxonomyMap[ordre.id] = TaxonomyInfo(ordre = ordre.name, type = "ORDER")
                    ordre.children.forEach { famille ->
                        taxonomyMap[famille.id] = TaxonomyInfo(
                            ordre = ordre.name, famille = famille.name, type = "FAMILY")
                        famille.children.forEach { genre ->
                            taxonomyMap[genre.id] = TaxonomyInfo(
                                ordre = ordre.name, famille = famille.name,
                                genre = genre.name, type = "GENUS")
                            genre.children.forEach { espece ->
                                taxonomyMap[espece.id] = TaxonomyInfo(
                                    ordre = ordre.name, famille = famille.name,
                                    genre = genre.name, espece = espece.name, type = "SPECIES")
                            }
                        }
                    }
                }

                val userIds = pictures.mapNotNull { it["userRef"] as? String }.distinct()
                if (userIds.isEmpty()) { onSuccess(pictures); return@fetchCensusTree }

                val userProfileMap = mutableMapOf<String, String>()
                val chunks   = userIds.chunked(10)
                var completed = 0

                chunks.forEach { chunk ->
                    firestore.collection("users")
                        .whereIn("__name__", chunk)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            snapshot.documents.forEach { doc ->
                                userProfileMap[doc.id] = doc.getString("profilePictureUrl") ?: ""
                            }
                            completed++
                            if (completed == chunks.size) {
                                val enriched = pictures.map { picture ->
                                    val map = picture.toMutableMap()
                                    if (!map.containsKey("adminValidated"))  map["adminValidated"]  = false
                                    if (!map.containsKey("recordingStatus")) map["recordingStatus"] = false
                                    val uid        = map["userRef"] as? String ?: ""
                                    map["profilePictureUrl"] = userProfileMap[uid] ?: ""
                                    val censusRef  = picture["censusRef"] as? String
                                    val taxonomy   = if (!censusRef.isNullOrEmpty() && censusRef != "null")
                                        taxonomyMap[censusRef] else null
                                    map["ordre"]  = taxonomy?.ordre   ?: "Non identifié"
                                    map["family"] = taxonomy?.famille ?: "Non identifié"
                                    map["genre"]  = taxonomy?.genre   ?: "Non identifié"
                                    map["specie"] = taxonomy?.espece  ?: "Non identifié"
                                    map
                                }
                                onSuccess(enriched)
                            }
                        }
                        .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
                }
            },
            onError = onError
        )
    }

    fun getPictureEnrichedById(
        pictureId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        picturesRef.document(pictureId).get()
            .addOnSuccessListener { document ->
                val data = document.data
                if (data == null) { onError(AppException.DocumentNotFound()); return@addOnSuccessListener }
                val map  = data.toMutableMap()
                map["id"] = document.id
                enrichPicturesWithCensusData(
                    pictures  = listOf(map),
                    onSuccess = { enriched -> onSuccess(enriched.first()) },
                    onError   = { e -> onError(FirebaseExceptionMapper.map(e)) }
                )
            }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    suspend fun getPicturesByUserEnrichedSortedByDate(userId: String): List<Map<String, Any>> {
        // Les exceptions Firebase remontent naturellement grâce au try/catch externe
        return try {
            val snapshot = firestore.collection("pictures")
                .whereEqualTo("userRef", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val pictures = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                val map  = data.toMutableMap()
                map["id"] = document.id
                map
            }

            val deferred = kotlinx.coroutines.CompletableDeferred<List<Map<String, Any>>>()

            enrichPicturesWithCensusData(
                pictures  = pictures,
                onSuccess = { deferred.complete(it) },
                onError   = { e ->
                    // L'enrichissement est non-critique : on retourne les photos brutes
                    // mais on logue l'erreur mappée pour le débogage
                    android.util.Log.w("PictureDao", FirebaseExceptionMapper.map(e).userMessage)
                    deferred.complete(pictures)
                }
            )

            deferred.await()

        } catch (e: AppException) {
            throw e                              // déjà mappée, on laisse passer
        } catch (e: Exception) {
            throw FirebaseExceptionMapper.map(e) // erreur Firebase → AppException
        }
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    fun bytesToWebpFile(context: Context, imageBytes: ByteArray, quality: Int = 90): File {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val file   = File.createTempFile("picture_", ".webp", context.cacheDir)
        FileOutputStream(file).use { out ->
            val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY
            else Bitmap.CompressFormat.WEBP
            bitmap.compress(format, quality, out)
        }
        return file
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }

    fun getAllRecordedPictures(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError:   (AppException) -> Unit
    ) {
        picturesRef
            .whereEqualTo("recordingStatus", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val pictures = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val map  = data.toMutableMap()
                    map["id"] = doc.id
                    map
                }
                onSuccess(pictures)
            }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }
}