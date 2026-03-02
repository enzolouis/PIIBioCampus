package com.example.piibiocampus.data.dao

import com.example.piibiocampus.data.model.ItemNews
import com.google.firebase.FirebaseApp
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
        newsRef
            .get()
            .addOnSuccessListener { snapshot ->
                val newsList = snapshot.toObjects(ItemNews::class.java)
                onSuccess(newsList)
            }
            .addOnFailureListener(onError)
    }
}