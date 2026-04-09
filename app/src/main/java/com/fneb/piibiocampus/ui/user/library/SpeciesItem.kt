package com.fneb.piibiocampus.ui.user.library

/**
 * Représente une espèce dans la bibliothèque.
 *
 * @param id                   ID du nœud CensusNode (SPECIES)
 * @param name                 Nom de l'espèce
 * @param taxonomyImageUrl     Image issue de l'arbre taxonomique (toujours affichée)
 * @param description          Description issue de l'arbre
 * @param orderName            Nom de l'ordre parent
 * @param familyName           Nom de la famille parente
 * @param genusName            Nom du genre parent
 * @param isRecordedByUser     L'utilisateur·ice a-t-il/elle recensé cette espèce ?
 * @param totalRecordingsCount Nombre total de recensements (tous utilisateur·ices)
 * @param pictureLocations     Localisations GPS des recensements validés (pour filtres)
 */
data class SpeciesItem(
    val id: String,
    val name: String,
    val taxonomyImageUrl: String,
    val description: List<String>,
    val orderName: String,
    val familyName: String,
    val genusName: String,
    val isRecordedByUser: Boolean = false,
    val totalRecordingsCount: Int = 0,
    val pictureLocations: List<Pair<Double, Double>> = emptyList()
)