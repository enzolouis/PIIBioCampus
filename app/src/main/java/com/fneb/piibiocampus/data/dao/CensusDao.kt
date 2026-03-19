package com.fneb.piibiocampus.data.dao

import com.fneb.piibiocampus.ui.census.CensusNode
import com.fneb.piibiocampus.ui.census.CensusType
import com.google.firebase.firestore.FirebaseFirestore

object CensusDao {

    private val db = FirebaseFirestore.getInstance()

    // ── Lecture ───────────────────────────────────────────────────────────────

    /**
     * Récupère la hiérarchie complète depuis la collection "census".
     * Appelle onComplete(rootNodes) sur succès.
     */
    fun fetchCensusTree(onComplete: (List<CensusNode>) -> Unit, onError: (Exception) -> Unit) {
        fetchCensusTreeFull(
            onComplete = { _, roots -> onComplete(roots) },
            onError    = onError
        )
    }

    /**
     * Comme [fetchCensusTree] mais retourne aussi l'ID du premier document Firestore.
     * Utilisé par CensusEditorViewModel pour pouvoir sauvegarder le document.
     */
    fun fetchCensusTreeFull(
        onComplete: (docId: String?, roots: List<CensusNode>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("census").get()
            .addOnSuccessListener { query ->
                val allRoots  = mutableListOf<CensusNode>()
                var firstDocId: String? = null

                for (doc in query.documents) {
                    if (firstDocId == null) firstDocId = doc.id
                    val data = doc.data ?: continue
                    val ordreAny = data["ordre"]
                    if (ordreAny is List<*>) {
                        ordreAny.forEach { ordObj ->
                            val ordMap = ordObj as? Map<*, *> ?: return@forEach
                            parseOrder(ordMap)?.let { allRoots.add(it) }
                        }
                    } else if (ordreAny is Map<*, *>) {
                        parseOrder(ordreAny)?.let { allRoots.add(it) }
                    }
                }
                onComplete(firstDocId, allRoots)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    /**
     * Sérialise [roots] et les sauvegarde dans le document [docId].
     * Si [docId] est null, crée un nouveau document.
     * [onSuccess] reçoit l'ID du document effectivement utilisé.
     */
    fun saveCensusDocument(
        docId:     String?,
        roots:     List<CensusNode>,
        onSuccess: (String) -> Unit,
        onError:   (Exception) -> Unit
    ) {
        val data = mapOf("ordre" to roots.map { it.toFirestoreMap() })
        val ref  = if (docId != null) db.collection("census").document(docId)
                   else               db.collection("census").document()

        ref.set(data)
            .addOnSuccessListener { onSuccess(ref.id) }
            .addOnFailureListener { e -> onError(e) }
    }

    // ── Sérialisation CensusNode → Map ────────────────────────────────────────

    private fun CensusNode.toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id"          to id,
            "name"        to name,
            "urlImage"    to imageUrl,
            "description" to description
        )
        when (type) {
            CensusType.ORDER   -> map["famille"] = children.map { it.toFirestoreMap() }
            CensusType.FAMILY  -> map["genre"]   = children.map { it.toFirestoreMap() }
            CensusType.GENUS   -> map["espece"]  = children.map { it.toFirestoreMap() }
            CensusType.SPECIES -> { /* feuille, pas de liste enfants */ }
        }
        return map
    }

    // ── Parsers (lecture) ─────────────────────────────────────────────────────

    private fun parseOrder(map: Map<*, *>): CensusNode? {
        val id          = map["id"] as? String ?: map["name"] as? String ?: return null
        val name        = map["name"] as? String ?: "ordre"
        val imageUrl    = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val families    = mutableListOf<CensusNode>()

        val familleAny = map["famille"]
        if (familleAny is List<*>) {
            familleAny.forEach { f ->
                (f as? Map<*, *>)?.let { fm -> parseFamily(fm)?.let { families.add(it) } }
            }
        } else if (familleAny is Map<*, *>) {
            parseFamily(familleAny)?.let { families.add(it) }
        }
        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description,
            type = CensusType.ORDER, children = families)
    }

    private fun parseFamily(map: Map<*, *>): CensusNode? {
        val id          = map["id"] as? String ?: map["name"] as? String ?: return null
        val name        = map["name"] as? String ?: "famille"
        val imageUrl    = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val genres      = mutableListOf<CensusNode>()

        val genreAny = map["genre"]
        if (genreAny is List<*>) {
            genreAny.forEach { g ->
                (g as? Map<*, *>)?.let { gm -> parseGenus(gm)?.let { genres.add(it) } }
            }
        } else if (genreAny is Map<*, *>) {
            parseGenus(genreAny)?.let { genres.add(it) }
        }
        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description,
            type = CensusType.FAMILY, children = genres)
    }

    private fun parseGenus(map: Map<*, *>): CensusNode? {
        val id          = map["id"] as? String ?: map["name"] as? String ?: return null
        val name        = map["name"] as? String ?: "genre"
        val imageUrl    = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val species     = mutableListOf<CensusNode>()

        val especeAny = map["espece"]
        if (especeAny is List<*>) {
            especeAny.forEach { s ->
                (s as? Map<*, *>)?.let { sm -> parseSpecies(sm)?.let { species.add(it) } }
            }
        } else if (especeAny is Map<*, *>) {
            parseSpecies(especeAny)?.let { species.add(it) }
        }
        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description,
            type = CensusType.GENUS, children = species)
    }

    private fun parseSpecies(map: Map<*, *>): CensusNode? {
        val id          = map["id"] as? String ?: map["name"] as? String ?: return null
        val name        = map["name"] as? String ?: "espèce"
        val imageUrl    = map["urlImage"] as? String ?: ""
        val description = (map["description"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return CensusNode(id = id, name = name, imageUrl = imageUrl, description = description,
            type = CensusType.SPECIES, children = emptyList())
    }
}
