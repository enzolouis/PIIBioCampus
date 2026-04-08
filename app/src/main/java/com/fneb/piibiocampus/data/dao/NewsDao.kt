package com.fneb.piibiocampus.data.dao

import android.content.Context
import android.net.Uri
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.ItemNews
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object NewsDao {
    private val storage = FirebaseStorage.getInstance()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    private val newsRef = firestore.collection("news")

    fun getDynamicNews(
        onSuccess: (List<ItemNews>) -> Unit,
        onError: (AppException) -> Unit
    ) {
        newsRef
            .whereEqualTo("behavior", "dynamic")
            .orderBy("order")
            .get()
            .addOnSuccessListener { snapshot ->
                val newsList = snapshot.documents.map { doc ->
                    ItemNews(
                        id = doc.id,
                        titre = doc.getString("titre") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        source = doc.getString("source") ?: "",
                        order = doc.getLong("order"),
                        behavior = doc.getString("behavior") ?: "",
                    )
                }
                onSuccess(newsList)
            }
            .addOnFailureListener(onError)
    }

    fun getStaticNews(
        onSuccess: (List<ItemNews>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        newsRef
            .whereEqualTo("behavior", "static")
            .orderBy("order")
            .get()
            .addOnSuccessListener { snapshot ->
                val newsList = snapshot.documents.map { doc ->
                    ItemNews(
                        id = doc.id,
                        titre = doc.getString("titre") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        source = doc.getString("source") ?: "",
                        order = doc.getLong("order"),
                        behavior = doc.getString("behavior") ?: "",
                    )
                }
                onSuccess(newsList)
            }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun updateNews(
        newsId: String,
        title: String,
        source: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (AppException) -> Unit
    ) {
        newsRef.document(newsId)
            .update(
                mapOf(
                    "titre" to title,
                    "source" to source,
                    "imageUrl" to imageUrl
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(FirebaseExceptionMapper.map(e)) }
    }

    fun uploadNewsImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (AppException) -> Unit
    ) {
        try {
            // convertir en bytes
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Impossible d'ouvrir l'image")

            val bytes = inputStream.readBytes()
            inputStream.close()

            // réutilise ta compression existante
            val file = PictureDao.bytesToWebpFile(context, bytes)

            val fileName = "news_${System.currentTimeMillis()}.webp"
            val ref = FirebaseStorage.getInstance().reference.child("news/$fileName")

            ref.putFile(Uri.fromFile(file))
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { url -> file.delete(); onSuccess(url.toString()) }
                        .addOnFailureListener { e -> file.delete(); onError(FirebaseExceptionMapper.map(e)) }
                }
                .addOnFailureListener { e -> file.delete(); onError(FirebaseExceptionMapper.map(e))}
        } catch (e: Exception) {
            onError(FirebaseExceptionMapper.map(e))
        }
    }

    fun createNews(
        titre: String,
        imageUrl: String,
        source: String,
        behavior: String?,
        order: Int,
        onSuccess: () -> Unit,
        onError: (AppException) -> Unit
    ) {
        val itemNews = mapOf(
            "titre" to titre,
            "imageUrl" to imageUrl,
            "source" to source,
            "behavior" to behavior,
            "order" to order
        )

        newsRef
            .add(itemNews)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(FirebaseExceptionMapper.map(e))
            }
    }

    fun deleteNews(
        newsId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        newsRef.document(newsId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(FirebaseExceptionMapper.map(e))
            }
    }
}