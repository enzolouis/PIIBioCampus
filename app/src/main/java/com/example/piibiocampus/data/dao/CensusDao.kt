package com.example.piibiocampus.data.dao

import com.example.piibiocampus.ui.census.CensusNode
import com.example.piibiocampus.ui.census.CensusType
import com.google.firebase.firestore.FirebaseFirestore

object CensusDao {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Récupère la hiérarchie complète depuis la collection "census".
     * Appelle onComplete(rootNodes) sur succès.
     */
    fun fetchCensusTree(onComplete: (List<CensusNode>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("census").get()
            .addOnSuccessListener { query ->
                val allRoots = mutableListOf<CensusNode>()
                for (doc in query.documents) {
                    val data = doc.data ?: continue
                    val ordreAny = data["ordre"]
                    if (ordreAny is List<*>) {
                        ordreAny.forEach { ordObj ->
                            val ordMap = ordObj as? Map<*, *> ?: return@forEach
                            parseOrder(ordMap)?.let { allRoots.add(it) }
                        }
                    } else if (ordreAny is Map<*, *>) {
                        // cas où ordre était stocké comme single map (compat)
                        parseOrder(ordreAny)?.let { allRoots.add(it) }
                    }
                }
                onComplete(allRoots)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // helpers de parsing (sécurisés)
    private fun parseOrder(map: Map<*, *>): CensusNode? {
        val id = map["id"] as? String ?: map["name"] as? String ?: return null
        val name = map["name"] as? String ?: "ordre"
        val imageUrl = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val families = mutableListOf<CensusNode>()

        val familleAny = map["famille"]
        if (familleAny is List<*>) {
            familleAny.forEach { f ->
                (f as? Map<*, *>)?.let { fm -> parseFamily(fm)?.let { families.add(it) } }
            }
        } else if (familleAny is Map<*, *>) {
            parseFamily(familleAny)?.let { families.add(it) }
        }

        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description, type = CensusType.ORDER, children = families)
    }

    private fun parseFamily(map: Map<*, *>): CensusNode? {
        val id = map["id"] as? String ?: map["name"] as? String ?: return null
        val name = map["name"] as? String ?: "famille"
        val imageUrl = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val genres = mutableListOf<CensusNode>()

        val genreAny = map["genre"]
        if (genreAny is List<*>) {
            genreAny.forEach { g ->
                (g as? Map<*, *>)?.let { gm -> parseGenus(gm)?.let { genres.add(it) } }
            }
        } else if (genreAny is Map<*, *>) {
            parseGenus(genreAny)?.let { genres.add(it) }
        }

        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description, type = CensusType.FAMILY, children = genres)
    }

    private fun parseGenus(map: Map<*, *>): CensusNode? {
        val id = map["id"] as? String ?: map["name"] as? String ?: return null
        val name = map["name"] as? String ?: "genre"
        val imageUrl = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val species = mutableListOf<CensusNode>()

        val especeAny = map["espece"]
        if (especeAny is List<*>) {
            especeAny.forEach { s ->
                (s as? Map<*, *>)?.let { sm -> parseSpecies(sm)?.let { species.add(it) } }
            }
        } else if (especeAny is Map<*, *>) {
            parseSpecies(especeAny)?.let { species.add(it) }
        }

        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description, type = CensusType.GENUS, children = species)
    }

    private fun parseSpecies(map: Map<*, *>): CensusNode? {
        val id = map["id"] as? String ?: map["name"] as? String ?: return null
        val name = map["name"] as? String ?: "espèce"
        val imageUrl = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description, type = CensusType.SPECIES, children = emptyList())
    }
}