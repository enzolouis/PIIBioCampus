package com.example.piibiocampus.data.dao

import com.example.piibiocampus.data.model.Campus
import com.google.firebase.firestore.FirebaseFirestore

object CampusDao {

    private val db = FirebaseFirestore.getInstance()

    fun getAll(onComplete: (List<Campus>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("campus").get()
            .addOnSuccessListener { query ->
                val campusList = mutableListOf<Campus>()
                for (doc in query.documents) {
                    val data = doc.data ?: continue
                    val campus = Campus(
                        id        = doc.id,
                        name      = data["name"] as? String ?: "",
                        radius    = (data["radius"] as? Number)?.toDouble() ?: 0.0,
                        longitudeCenter = (data["longitudeCenter"] as? Number)?.toDouble() ?: 0.0,
                        latitudeCenter  = (data["latitudeCenter"] as? Number)?.toDouble() ?: 0.0
                    )
                    campusList.add(campus)
                }
                onComplete(campusList)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}