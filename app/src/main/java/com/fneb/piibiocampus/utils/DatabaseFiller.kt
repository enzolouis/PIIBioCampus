package com.fneb.piibiocampus.utils

import com.fneb.piibiocampus.data.model.Campus
import com.google.firebase.firestore.CollectionReference
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

    fun fillCampus(clear: Boolean = false) {
        val db = FirebaseFirestore.getInstance()
        val collection = db.collection("campus")

        val campuses = listOf(
            // Paris
            Campus(generateId("Paris Saclay"), "Paris-Saclay", 4000.0, 2.1700, 48.7090),
            Campus(generateId("Sorbonne Universite"), "Sorbonne Université", 2000.0, 2.3440, 48.8462),
            Campus(generateId("Paris Cite"), "Université Paris Cité", 2000.0, 2.3550, 48.8400),
            Campus(generateId("PSL"), "PSL (Quartier Latin)", 1500.0, 2.3360, 48.8460),
            Campus(generateId("Sciences Po"), "Sciences Po Paris", 1200.0, 2.3280, 48.8530),
            Campus(generateId("Paris Nanterre"), "Paris Nanterre", 2500.0, 2.2090, 48.9010),
            Campus(generateId("Creteil UPEC"), "Créteil (UPEC)", 2000.0, 2.4600, 48.7900),
            Campus(generateId("Paris 8 Saint Denis"), "Paris 8 Saint-Denis", 2000.0, 2.3570, 48.9360),

            // Toulouse
            Campus(generateId("Université de Toulouse UT3"), "Université de Toulouse (UT3)", 2500.0, 1.466220712666369, 43.56463539561721),
            Campus(generateId("Université Toulouse Capitole UT1"), "Université Toulouse Capitole (UT1)", 1500.0, 1.4371570942869203, 43.60720817811082),
            Campus(generateId("Université Toulouse Jean Jaurès UT2"), "Université Toulouse - Jean Jaurès (U2J)", 2000.0, 1.4020874433889605, 43.57683327090197),

            // Bordeaux
            Campus(generateId("Bordeaux Carreire"), "Bordeaux-Carreire", 1000.0, -0.6051290299378996, 44.82582965077974),
            Campus(generateId("Bordeaux Victoire Bastide"), "Bordeaux-Victoire et Bastide", 1500.0, -0.5708716577644373, 44.831917163988464),
            Campus(generateId("Pessac Talence Gradignan"), "Pessac Talence Gradignan", 1000.0, -0.5962678104864305, 44.8005876650241),

            // Rennes
            Campus(generateId("Rennes Villejean"), "Rennes - Villejean", 2000.0, -1.7016, 48.1180),
            Campus(generateId("Rennes Beaulieu"), "Rennes - Beaulieu", 2000.0, -1.6383, 48.1158),
            Campus(generateId("Rennes Centre"), "Rennes - Centre", 1200.0, -1.6750, 48.1110),

            // Lille
            Campus(generateId("Villeneuve d Ascq"), "Villeneuve-d’Ascq", 3000.0, 3.1410, 50.6110),
            Campus(generateId("Lille Centre"), "Lille Centre", 1500.0, 3.0573, 50.6292),
            Campus(generateId("Lille Sante"), "Lille Santé", 1500.0, 3.0200, 50.6320),

            // Amiens
            Campus(generateId("Amiens Centre"), "Amiens Centre", 1500.0, 2.3010, 49.8738),
            Campus(generateId("Amiens Sud"), "Amiens Sud", 2000.0, 2.3450, 49.8465),

            // Marseille
            Campus(generateId("Luminy"), "Luminy", 2500.0, 5.4390, 43.2320),
            Campus(generateId("Marseille Centre"), "Marseille Centre", 1500.0, 5.3698, 43.2965),
            Campus(generateId("Timone"), "Timone", 1500.0, 5.4010, 43.2880),
            Campus(generateId("Saint Jerome"), "Saint-Jérôme", 2000.0, 5.4420, 43.3440),

            // Lyon
            Campus(generateId("La Doua"), "La Doua", 2500.0, 4.8780, 45.7820),
            Campus(generateId("Lyon Centre"), "Lyon Centre", 2000.0, 4.8370, 45.7485),
            Campus(generateId("Bron Grange Blanche"), "Bron / Grange Blanche", 2000.0, 4.8140, 45.7190),

            // Limoges
            Campus(generateId("Limoges Centre"), "Limoges Centre", 1200.0, 1.2611, 45.8336),
            Campus(generateId("Limoges Sante"), "Limoges Santé", 1200.0, 1.2700, 45.8150),

            // Montpellier
            Campus(generateId("Triolet"), "Triolet", 2000.0, 3.8640, 43.6320),
            Campus(generateId("Montpellier Centre"), "Montpellier Centre", 1500.0, 3.8767, 43.6108),
            Campus(generateId("Richter"), "Richter", 1500.0, 3.8400, 43.6290),

            // Dijeon
            Campus(generateId("Montmuzard"), "Montmuzard", 2000.0, 5.0680, 47.3110),
            Campus(generateId("Dijon Centre"), "Dijon Centre", 1200.0, 5.0410, 47.3220),

            // Reims
            Campus(generateId("Croix Rouge"), "Croix-Rouge", 2500.0, 4.0170, 49.2320),
            Campus(generateId("Reims Centre"), "Reims Centre", 1200.0, 4.0310, 49.2580),

            // Savoie
            Campus(generateId("Annecy"), "Annecy", 2000.0, 6.1294, 45.9197),
            Campus(generateId("Le Bourget du Lac"), "Le Bourget-du-Lac", 2500.0, 5.8670, 45.6415),
            Campus(generateId("Chambery Jacob"), "Chambéry - Jacob-Bellecombette", 2000.0, 5.9178, 45.5620),
        )

        if (clear) {
            collection.get().addOnSuccessListener { documents ->
                val batch = db.batch()

                for (doc in documents) {
                    batch.delete(doc.reference)
                }

                batch.commit().addOnSuccessListener {
                    insertCampuses(collection, campuses)
                }
            }
        } else {
            insertCampuses(collection, campuses)
        }
    }

    private fun insertCampuses(
        collection: CollectionReference,
        campuses: List<Campus>
    ) {
        for (campus in campuses) {
            collection.document(campus.id).set(campus)
        }
    }

    private fun generateId(name: String): String {
        return name
            .lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("(", "")
            .replace(")", "")
            .replace("’", "")
    }
}