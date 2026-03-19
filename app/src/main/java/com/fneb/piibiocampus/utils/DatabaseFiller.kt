package com.fneb.piibiocampus.utils

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

    fun updatePicturesWithRealPictures() {
        val db = FirebaseFirestore.getInstance()

        val imageUrls = listOf(
            "https://cdnfiles2.biolovision.net/www.faune-france.org/pdffiles/news/Calliste_septicolorMichel_Clament-7640.jpg",
            "https://c.pxhere.com/photos/6f/4e/fleurs_canon_europe_midi_prairies_ext_rieur_printemps_plantes-169157.jpg!s2",
            "https://parcsaintecroix.com/wp-content/uploads/2022/09/daim-cp-morgane-bricard-69-scaled-767x767-58ee83f4d76f.jpg",
            "https://img.freepik.com/photos-gratuite/antenne-couleur-verte-abeille-blanche_1172-439.jpg?semt=ais_hybrid&w=740&q=80",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSDxop2VZ9bWpdpl1hpicpbmb_5HgjTu_eTTw&s"
        )

        db.collection("pictures").get().addOnSuccessListener { documents ->
            for (doc in documents) {

                val randomUrl = imageUrls.random()

                doc.reference.update(
                    "imageUrl", randomUrl
                )
            }
        }
    }

    fun updateUsersProfilePictures() {
        val db = FirebaseFirestore.getInstance()

        val profilePictures = listOf(
            "https://img.freepik.com/free-vector/cute-panda-with-bamboo_138676-3053.jpg?semt=ais_rp_progressive&w=740&q=80",
            "https://external-preview.redd.it/8S-3pnTO-r8xHjq3pq41C2cSEG9tFc6A1mmwqVJttJU.jpg?width=640&crop=smart&auto=webp&s=ef1c2501b3cce461bd4412566199dc8fef2686b9",
        )

        db.collection("users").get().addOnSuccessListener { documents ->
            for (doc in documents) {

                val randomUrl = profilePictures.random()

                doc.reference.update(
                    "profilePictureUrl", randomUrl
                )
            }
        }
    }
}