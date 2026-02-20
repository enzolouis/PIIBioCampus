package com.example.piibiocampus.ui.photo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Représente le contexte depuis lequel PhotoViewerActivity est ouverte.
 * Cela permet de contrôler dynamiquement la visibilité des boutons et la taille de la vue.
 */
enum class PicturesViewerCaller {
    MAP,            // Appelé depuis la carte → FAB + BottomNav visibles → taille réduite
    MY_PROFILE,     // Appelé depuis le profil perso → pas de FAB → taille intermédiaire
    ADMIN,          // Appelé depuis la vue admin → plein écran
    CENSUS_TREE     // Appelé depuis la gestion de recensement
}

/**
 * Toutes les données nécessaires pour afficher PhotoViewerActivity.
 * Passé via Intent en tant que Parcelable.
 */

@Parcelize
data class PhotoViewerState(
    // --- Données de la photo ---
    val imageUrl: String,
    val family: String?,
    val genre: String?,
    val specie: String?,
    val timestamp: String,          // déjà formaté en String avant de passer
    val adminValidated: Boolean,
    val pictureId: String,          // ID du document Firestore pour actions (delete, validate)
    val authorId: String,           // UID de l'auteur (pour charger sa photo de profil)
    val authorProfilePictureUrl: String?,

    // --- Données pour "Reprendre le recensement" ---
    val censusRef: String?,         // ID du nœud CensusTree sélectionné
    val imageBytes: ByteArray?,     // image originale pour re-ouvrir CensusTreeActivity
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,

    // --- Contexte d'appel ---
    val caller: PicturesViewerCaller

) : Parcelable {

    // ---------------------------------------------------------------
    // Règles de visibilité des éléments UI selon le caller
    // ---------------------------------------------------------------

    /** Photo de profil de l'auteur : visible sur MAP et CENSUS_TREE (pas sur MY_PROFILE) */
    val showAuthorProfile: Boolean
        get() = caller == PicturesViewerCaller.MAP || caller == PicturesViewerCaller.CENSUS_TREE

    /**
     * Bouton "Reprendre le recensement" :
     * - MY_PROFILE → visible seulement si adminValidated == false
     * - CENSUS_TREE → toujours visible
     * - MAP / ADMIN → jamais visible
     */
    val showResumeCensus: Boolean
        get() = when (caller) {
            PicturesViewerCaller.MY_PROFILE   -> !adminValidated
            PicturesViewerCaller.CENSUS_TREE  -> true
            else                           -> false
        }

    /** Bouton "Supprimer" : visible sur MY_PROFILE, ADMIN et CENSUS_TREE */
    val showDelete: Boolean
        get() = caller == PicturesViewerCaller.MY_PROFILE
                || caller == PicturesViewerCaller.ADMIN
                || caller == PicturesViewerCaller.CENSUS_TREE

    /** Bouton "Valider / Invalider" : visible uniquement pour ADMIN */
    val showValidate: Boolean
        get() = caller == PicturesViewerCaller.ADMIN

    /** Icône de validation (adminValidated) : toujours visible si true, quel que soit le caller */
    val showValidatedBadge: Boolean
        get() = adminValidated

    /**
     * Titre adapté au niveau taxonomique connu.
     * Priorité : espèce > genre > famille > "Non identifié"
     */
    val displayTitle: String
        get() = when {
            !specie.isNullOrBlank() -> specie
            !genre.isNullOrBlank()  -> genre
            !family.isNullOrBlank() -> family
            else                    -> "Non identifié"
        }
}
