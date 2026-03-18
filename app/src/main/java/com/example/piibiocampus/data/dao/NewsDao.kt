package com.example.piibiocampus.data.dao

import com.example.piibiocampus.data.model.ItemNews
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldPath.documentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object NewsDao {
    private val storage = FirebaseStorage.getInstance()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance())
    }

    private val newsRef = firestore.collection("news")
    private val storageRef = storage.reference

    fun getAllNews(
        onSuccess: (List<ItemNews>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        newsRef.get()
            .addOnSuccessListener { snapshot ->
                val newsList = snapshot.documents.map { doc ->
                    ItemNews(
                        id = doc.id,
                        titre = doc.getString("titre") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        source = doc.getString("source") ?: ""
                    )
                }
                onSuccess(newsList)
            }
            .addOnFailureListener(onError)
    }

    fun updateNews(
        newsId: String,
        title: String,
        source: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
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
            .addOnFailureListener(onError)
    }
}