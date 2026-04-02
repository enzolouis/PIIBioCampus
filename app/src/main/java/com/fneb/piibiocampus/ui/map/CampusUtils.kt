package com.fneb.piibiocampus.utils

import com.fneb.piibiocampus.data.model.CampusPolygons
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.util.GeoPoint

// ─────────────────────────────────────────────────────────────────────────────
// Import des polygones campus dans Firebase
// ─────────────────────────────────────────────────────────────────────────────

/**
 * À appeler UNE SEULE FOIS (ex: depuis un bouton admin ou un DataLoader).
 * Importe tous les campus de CampusPolygons.all dans Firestore.
 *
 * Structure dans Firebase :
 *   campus/{name} : {
 *     name, latitudeCenter, longitudeCenter, radius,
 *     polygon: [ {lat, lng}, ... ]
 *   }
 */
fun importCampusPolygonsToFirebase(
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    for (campus in CampusPolygons.all) {
        val polygonData = campus.polygon.map { (lat, lng) ->
            mapOf("lat" to lat, "lng" to lng)
        }
        val docRef = db.collection("campus").document(campus.name)
        batch.set(docRef, mapOf(
            "name"            to campus.name,
            "latitudeCenter"  to campus.latitudeCenter,
            "longitudeCenter" to campus.longitudeCenter,
            "radius"          to campus.radius,
            "polygon"         to polygonData
        ))
    }

    batch.commit()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Détection : est-ce qu'un point est dans un polygone ?
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Algorithme Ray Casting.
 * @param point  Position GPS à tester
 * @param polygon Liste de points (lat, lng) formant le polygone (sens quelconque)
 * @return true si le point est à l'intérieur du polygone
 */
fun isPointInPolygon(
    point: GeoPoint,
    polygon: List<Pair<Double, Double>>
): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val (latI, lngI) = polygon[i]
        val (latJ, lngJ) = polygon[j]
        val intersect = (lngI > point.longitude) != (lngJ > point.longitude) &&
            point.latitude < (latJ - latI) * (point.longitude - lngI) / (lngJ - lngI) + latI
        if (intersect) inside = !inside
        j = i
    }
    return inside
}

/**
 * Retourne le nom du campus dans lequel se trouve le point,
 * ou null si hors de tout campus.
 *
 * Utilise d'abord le polygone précis s'il est disponible,
 * sinon replie sur le test centre+rayon.
 */
fun findCampusForPoint(
    point: GeoPoint
): String? {
    for (campus in CampusPolygons.all) {
        if (campus.polygon.isNotEmpty()) {
            if (isPointInPolygon(point, campus.polygon)) return campus.name
        } else {
            // Fallback : cercle
            val center = GeoPoint(campus.latitudeCenter, campus.longitudeCenter)
            if (center.distanceToAsDouble(point) <= campus.radius) return campus.name
        }
    }
    return null
}
