import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.piibiocampus.data.model.LocationMeta
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.*

object PictureDao {

    private val storage = FirebaseStorage.getInstance()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    private val picturesRef = firestore.collection("pictures")
    private val storageRef = storage.reference

    private fun uriToWebpFile(
        context: Context,
        uri: Uri,
        quality: Int = 90
    ): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Impossible d'ouvrir l'URI")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val file = File.createTempFile("picture_", ".webp", context.cacheDir)
        val outputStream = FileOutputStream(file)

        val format = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

        bitmap.compress(format, quality, outputStream)

        outputStream.flush()
        outputStream.close()

        return file
    }

    private fun bytesToWebpFile(
        context: Context,
        imageBytes: ByteArray,
        quality: Int = 90
    ): File {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val file = File.createTempFile("picture_", ".webp", context.cacheDir)
        val outputStream = FileOutputStream(file)

        val format = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

        bitmap.compress(format, quality, outputStream)

        outputStream.flush()
        outputStream.close()

        return file
    }

    fun exportPictureFromBytes(
        context: Context,
        imageBytes: ByteArray,
        location: LocationMeta,
        censusRef: String?,
        userRef: String? = null, // si null, on prend l'UID FirebaseAuth
        speciesRef: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // récupère l'UID actuel si userRef pas fourni
            val currentUserUid = userRef ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utilisateur non connecté")

            // conversion ByteArray -> fichier WebP temporaire
            val webpFile = bytesToWebpFile(context, imageBytes)

            // ID unique pour la photo
            val pictureId = picturesRef.document().id
            val pictureStorageRef = storageRef.child("$pictureId.webp")

            // metadata obligatoire pour passer les rules
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setCustomMetadata("userRef", currentUserUid)
                .build()

            // upload vers Firebase Storage
            pictureStorageRef.putFile(Uri.fromFile(webpFile), metadata)
                .addOnSuccessListener {
                    pictureStorageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                        // stockage Firestore
                        val data = hashMapOf(
                            "imageUrl" to downloadUrl.toString(),
                            "timestamp" to Date(),
                            "location" to mapOf(
                                "latitude" to location.latitude,
                                "longitude" to location.longitude,
                                "altitude" to location.altitude
                            ),
                            "censusRef" to censusRef,
                            "userRef" to currentUserUid,
                            "speciesRef" to speciesRef
                        )

                        picturesRef.document(pictureId)
                            .set(data)
                            .addOnSuccessListener {
                                // 🧹 nettoyage
                                webpFile.delete()
                                onSuccess()
                            }
                            .addOnFailureListener(onError)
                    }.addOnFailureListener(onError)
                }
                .addOnFailureListener(onError)

        } catch (e: Exception) {
            onError(e)
        }
    }


    /**
     * Exporter une photo (upload + Firestore)
     */
    fun exportPicture(
        context: Context,
        imageUri: Uri,
        location: LocationMeta,
        censusRef: String,
        userRef: String,
        speciesRef: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
// 🔹 Conversion WEBP
            val webpFile = uriToWebpFile(context, imageUri)
            val pictureId = picturesRef.document().id

            val pictureStorageRef =
                storageRef.child("$pictureId.webp")

            pictureStorageRef.putFile(Uri.fromFile(webpFile))
                .addOnSuccessListener {
                    pictureStorageRef.downloadUrl
                        .addOnSuccessListener { downloadUrl ->

                            val data = hashMapOf(
                                "imageUrl" to downloadUrl.toString(),
                                "timestamp" to Date(),
                                "location" to mapOf(
                                    "latitude" to location.latitude,
                                    "longitude" to location.longitude,
                                    "altitude" to location.altitude
                                ),
                                "censusRef" to censusRef,
                                "userRef" to userRef,
                                "speciesRef" to speciesRef
                            )

                            picturesRef.document(pictureId)
                                .set(data)
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener(onError)
                        }
                        .addOnFailureListener(onError)
                }
                .addOnFailureListener(onError)

        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Télécharger une photo depuis Storage
     */
    fun downloadPicture(
        pictureId: String,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Récupération du document Firestore
        picturesRef.document(pictureId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val url = document.getString("imageUrl")
                    if (!url.isNullOrEmpty()) {
                        // Convertir en Uri pour Picasso/Glide
                        onSuccess(Uri.parse(url))
                    } else {
                        onError(Exception("L'URL de l'image est vide"))
                    }
                } else {
                    onError(Exception("Document inexistant"))
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getPicturesByUser(
        userRef: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef
            .whereEqualTo("userRef", userRef)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener(onError)
    }

    fun getAllPictures(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener(onError)
    }

    fun getPicturesByCensus(
        censusRef: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef
            .whereEqualTo("censusRef", censusRef)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener(onError)
    }

    fun getPicturesBySpecies(
        speciesRef: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef
            .whereEqualTo("speciesRef", speciesRef)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener(onError)
    }

    fun getPicturesByDateRange(
        start: Date,
        end: Date,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(start))
            .whereLessThanOrEqualTo("timestamp", Timestamp(end))
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener(onError)
    }



    private fun distanceInMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0 // rayon Terre (m)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }

    fun getPicturesNearLocation(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        picturesRef.get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    val location = doc.get("location") as? Map<*, *> ?: return@mapNotNull null

                    val lat = location["latitude"] as? Double ?: return@mapNotNull null
                    val lon = location["longitude"] as? Double ?: return@mapNotNull null

                    val distance = distanceInMeters(
                        centerLat, centerLon,
                        lat, lon
                    )

                    if (distance <= radiusMeters) doc.data else null
                }

                onSuccess(result)
            }
            .addOnFailureListener(onError)
    }
}
