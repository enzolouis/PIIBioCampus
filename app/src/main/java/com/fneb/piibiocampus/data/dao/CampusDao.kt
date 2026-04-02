package com.fneb.piibiocampus.data.dao

import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.Campus
import com.google.firebase.firestore.FirebaseFirestore

object CampusDao {

    private val db = FirebaseFirestore.getInstance()

    fun getAll(onComplete: (List<Campus>) -> Unit, onError: (AppException) -> Unit) {
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
                onError(FirebaseExceptionMapper.map(e))
            }
    }
}