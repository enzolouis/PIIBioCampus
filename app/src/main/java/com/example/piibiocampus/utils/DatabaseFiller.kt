package com.example.piibiocampus.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

object DatabaseFiller {
    fun fillDatabase() {
        val db = FirebaseFirestore.getInstance()
        for (i in 100..140) {

            val data = hashMapOf(
                "censusRef" to "",
                "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/piibiocampus-c8f50.firebasestorage.app/o/pangolin.jpg?alt=media&token=69825aeb-e137-480b-ad34-cda35ac8dba7",
                "location" to hashMapOf(
                    "altitude" to Random.nextDouble(0.0, 3000.0),
                    "latitude" to Random.nextDouble(-90.0, 90.0),
                    "longitude" to Random.nextDouble(-180.0, 180.0)
                ),
                "speciesRef" to "",
                "timestamp" to System.currentTimeMillis(),
                "userRef" to "nUEdMnubWyR9z7BL86Tc88C52Ri1"
            )

            db.collection("pictures").add(data)
        }
    }

    fun fillUsers() {
        val db = FirebaseFirestore.getInstance()
        for (i in 20..40) {
            val data = hashMapOf(
                "description" to "Je déteste les abeilles",
                "email" to "user$i@gmail.com",
                "name" to "User$i",
                "profilePictureUrl" to "https://firebasestorage.googleapis.com/v0/b/piibiocampus-c8f50.firebasestorage.app/o/hippo.jpg?alt=media&token=b7cb1564-951d-4e3c-b5b9-80034165c0ea",
                "role" to if (i % 2 == 0) "ADMIN" else "USER"
            )

            db.collection("users").add(data)
        }
    }
}