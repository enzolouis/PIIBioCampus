package com.fneb.piibiocampus.ui.library

import androidx.lifecycle.*
import com.fneb.piibiocampus.data.dao.CampusDao
import com.fneb.piibiocampus.data.dao.CensusDao
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.model.Campus
import com.fneb.piibiocampus.ui.census.CensusNode
import com.fneb.piibiocampus.ui.census.CensusType
import kotlin.math.*

class LibraryViewModel : ViewModel() {

    // ── État ──────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // ── Données ───────────────────────────────────────────
    private val _allSpecies = MutableLiveData<List<SpeciesItem>>(emptyList())

    private val _campusList = MutableLiveData<List<Campus>>(emptyList())
    val campusList: LiveData<List<Campus>> = _campusList

    // ── Filtres ───────────────────────────────────────────
    var searchQuery:    String? = null
    var filterCampusId: String? = null

    // ── Résultat filtré ───────────────────────────────────
    private val _displayedSpecies = MutableLiveData<List<SpeciesItem>>(emptyList())
    val displayedSpecies: LiveData<List<SpeciesItem>> = _displayedSpecies

    // ─────────────────────────────────────────────────────

    fun loadAll() {
        _isLoading.value = true

        CampusDao.getAll(
            onComplete = { _campusList.postValue(it) },
            onError    = { _campusList.postValue(emptyList()) }
        )

        val currentUid = UserDao.getCurrentUser()?.uid

        CensusDao.fetchCensusTree(
            onComplete = { roots ->
                val hierarchy = extractSpeciesHierarchy(roots)

                PictureDao.getAllRecordedPictures(
                    onSuccess = { allPics ->
                        val picsBySpecies  = allPics.groupBy { it["censusRef"] as? String ?: "" }
                        val userPicsBySpec = if (currentUid != null)
                            allPics.filter { it["userRef"] == currentUid }
                                .groupBy { it["censusRef"] as? String ?: "" }
                        else emptyMap()

                        val items = hierarchy.map { (node, order, family, genus) ->
                            val allForSpecies  = picsBySpecies[node.id]  ?: emptyList()
                            val userForSpecies = userPicsBySpec[node.id] ?: emptyList()

                            SpeciesItem(
                                id                   = node.id,
                                name                 = node.name,
                                taxonomyImageUrl     = node.imageUrl,
                                description          = node.description,
                                orderName            = order,
                                familyName           = family,
                                genusName            = genus,
                                isRecordedByUser     = userForSpecies.isNotEmpty(),
                                totalRecordingsCount = allForSpecies.size,
                                pictureLocations     = allForSpecies.mapNotNull { pic ->
                                    val loc = pic["location"] as? Map<*, *> ?: return@mapNotNull null
                                    val lat = loc["latitude"]  as? Double    ?: return@mapNotNull null
                                    val lon = loc["longitude"] as? Double    ?: return@mapNotNull null
                                    Pair(lat, lon)
                                }
                            )
                        }

                        // .value synchrone pour que applyFilters() lise immédiatement la liste
                        _allSpecies.value = items
                        _isLoading.value  = false
                        applyFilters()
                    },
                    onError = { e ->
                        _error.postValue(e.message)
                        _isLoading.postValue(false)
                    }
                )
            },
            onError = { e ->
                _error.postValue(e.message)
                _isLoading.postValue(false)
            }
        )
    }

    // ── Filtres ───────────────────────────────────────────

    fun applyFilters() {
        val campus = _campusList.value ?: emptyList()
        var result = _allSpecies.value ?: emptyList()

        // Recherche textuelle
        searchQuery?.takeIf { it.isNotBlank() }?.let { q ->
            val lower = q.lowercase()
            result = result.filter {
                it.name.lowercase().contains(lower)       ||
                        it.familyName.lowercase().contains(lower) ||
                        it.genusName.lowercase().contains(lower)  ||
                        it.orderName.lowercase().contains(lower)
            }
        }

        // Filtre campus
        filterCampusId?.let { campusId ->
            val target = campus.find { it.id == campusId || it.name == campusId }
            if (target != null) {
                result = result.filter { species ->
                    species.pictureLocations.any { (lat, lon) ->
                        distanceMeters(lat, lon, target.latitudeCenter, target.longitudeCenter) <= target.radius
                    }
                }
            }
        }

        // Tri : recensées par l'utilisateur·ice en premier, puis alphabétique
        result = result.sortedWith(
            compareByDescending<SpeciesItem> { it.isRecordedByUser }.thenBy { it.name }
        )

        _displayedSpecies.value = result
    }

    fun resetFilters() {
        searchQuery    = null
        filterCampusId = null
        applyFilters()
    }

    // ── Helpers ───────────────────────────────────────────

    private data class SpeciesHierarchy(
        val node:   CensusNode,
        val order:  String,
        val family: String,
        val genus:  String
    )

    private fun extractSpeciesHierarchy(roots: List<CensusNode>): List<SpeciesHierarchy> {
        val result = mutableListOf<SpeciesHierarchy>()
        for (order in roots) {
            for (family in order.children) {
                for (genus in family.children) {
                    for (species in genus.children) {
                        if (species.type == CensusType.SPECIES) {
                            result.add(SpeciesHierarchy(species, order.name, family.name, genus.name))
                        }
                    }
                }
            }
        }
        return result
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}