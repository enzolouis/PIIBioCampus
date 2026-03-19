package com.example.piibiocampus.ui.admin

import androidx.lifecycle.*
import com.example.piibiocampus.data.dao.CampusDao
import com.example.piibiocampus.data.dao.CensusDao
import com.example.piibiocampus.data.dao.PictureDao
import com.example.piibiocampus.data.model.Campus
import com.example.piibiocampus.ui.census.CensusNode
import com.example.piibiocampus.ui.census.CensusType
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

/**
 * Filtre de validation (2 états) :
 * - ONLY_VALIDATED    : uniquement adminValidated == true
 * - VALIDATED_AND_NOT : validées ET non validées
 */
enum class ValidationFilter { ONLY_VALIDATED, VALIDATED_AND_NOT }

/**
 * Représente une ligne du CSV
 */
data class ExportRow(
    val adminValidated: Boolean,
    val altitude: Double,
    val longitude: Double,
    val latitude: Double,
    val date: String,           // formaté dd/MM/yyyy HH:mm
    val maxLevel: String,       // "ordre" | "famille" | "genre" | "espèce"
    val ordre: String,
    val famille: String,
    val genre: String,
    val espece: String
)

class ExportDataViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _allPictures = MutableLiveData<List<Map<String, Any>>>(emptyList())

    private val _campusList = MutableLiveData<List<Campus>>(emptyList())
    val campusList: LiveData<List<Campus>> = _campusList

    // Racines de l'arbre de recensement, chargées une seule fois.
    private var censusRoots: List<CensusNode> = emptyList()

    var filterCampusId: String?   = null
    var filterDateFrom: Calendar? = null
    var filterDateTo:   Calendar? = null
    // Par défaut : "Validé uniquement"
    var filterValidation: ValidationFilter = ValidationFilter.ONLY_VALIDATED

    // résultats filtrés
    private val _filteredCount = MutableLiveData(0)
    val filteredCount: LiveData<Int> = _filteredCount

    // Les 4 premières lignes, pour l'aperçu du tableau dans l'UI
    private val _previewRows = MutableLiveData<List<ExportRow>>(emptyList())
    val previewRows: LiveData<List<ExportRow>> = _previewRows

    fun loadAll() {
        _isLoading.value = true

        // Campus
        CampusDao.getAll(
            onComplete = { _campusList.postValue(it) },
            onError    = { _campusList.postValue(emptyList()) }
        )

        CensusDao.fetchCensusTreeFull(
            onComplete = { _, roots ->
                censusRoots = roots
            },
            onError = { /* on continue sans l'arbre ; les colonnes seront vides */ }
        )

        // Photos enrichies (contiennent family/genre/specie déjà dans certains projets,
        // mais ici on les résout nous-mêmes depuis censusRef pour être sûr)
        PictureDao.getAllPicturesEnriched(
            onSuccess = { list ->
                _allPictures.postValue(list)
                _isLoading.postValue(false)
                applyFilters()
            },
            onError = {
                _error.postValue(it.message)
                _isLoading.postValue(false)
            }
        )
    }

    fun applyFilters() {
        // Ne pas émettre de résultat tant que les photos ne sont pas encore chargées
        val all = _allPictures.value
        if (all.isNullOrEmpty() && _isLoading.value == true) return

        val campus = _campusList.value ?: emptyList()
        var result = all ?: emptyList()

        // Filtre campus / hors campus
        filterCampusId?.let { campusFilter ->
            result = result.filter { photo ->
                val loc = photo["location"] as? Map<*, *> ?: return@filter false
                val lat = loc["latitude"]  as? Double    ?: return@filter false
                val lon = loc["longitude"] as? Double    ?: return@filter false
                val inAnyCampus = campus.any { c ->
                    distanceMeters(lat, lon, c.latitudeCenter, c.longitudeCenter) <= c.radius
                }
                if (campusFilter == "HORS_CAMPUS") !inAnyCampus
                else {
                    val target = campus.find { it.name == campusFilter }
                    target != null &&
                            distanceMeters(lat, lon, target.latitudeCenter, target.longitudeCenter) <= target.radius
                }
            }
        }

        // Filtre plage de dates
        if (filterDateFrom != null && filterDateTo != null) {
            result = result.filter { photo ->
                val date = timestampToDate(photo["timestamp"])
                date != null && date >= filterDateFrom!!.time && date <= filterDateTo!!.time
            }
        }

        // Filtre validation (2 états, pas de "tout")
        result = when (filterValidation) {
            ValidationFilter.ONLY_VALIDATED    ->
                result.filter { (it["adminValidated"] as? Boolean) == true }
            ValidationFilter.VALIDATED_AND_NOT ->
                result.filter { it.containsKey("adminValidated") }
        }

        _filteredCount.postValue(result.size)

        // Aperçu : résout les 4 premières lignes pour l'UI
        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        _previewRows.postValue(
            result.take(4).map { photo ->
                val loc            = photo["location"] as? Map<*, *>
                val adminValidated = (photo["adminValidated"] as? Boolean) ?: false
                val altitude       = (loc?.get("altitude")  as? Double) ?: 0.0
                val longitude      = (loc?.get("longitude") as? Double) ?: 0.0
                val latitude       = (loc?.get("latitude")  as? Double) ?: 0.0
                val date           = timestampToDate(photo["timestamp"])
                    ?.let { fmt.format(it) } ?: ""
                val tax = resolveTaxonomy(photo["censusRef"] as? String)
                ExportRow(adminValidated, altitude, longitude, latitude, date,
                    tax.maxLevel, tax.ordre, tax.famille, tax.genre, tax.espece)
            }
        )
    }

    fun resetFilters() {
        filterCampusId   = null
        filterDateFrom   = null
        filterDateTo     = null
        filterValidation = ValidationFilter.ONLY_VALIDATED  // retour au défaut
        applyFilters()
    }

    // codes simillaires à CensusViewModel.findPathToNode() :
    // étant donné un censusRef (ID d'un CensusNode), on parcourt l'arbre
    // récursivement pour retrouver le chemin complet depuis la racine,
    // puis on mappe chaque niveau ORDER/FAMILY/GENUS/SPECIES sur les colonnes CSV.

    private fun findPathToNode(
        nodes: List<CensusNode>,
        targetId: String,
        currentPath: List<CensusNode> = emptyList()
    ): List<CensusNode>? {
        for (node in nodes) {
            val path = currentPath + node
            if (node.id == targetId) return path
            val found = findPathToNode(node.children, targetId, path)
            if (found != null) return found
        }
        return null
    }

    /**
     * Résout un censusRef en ordre, famille, genre, espèce, maxLevel.
     * Retourne des chaînes vides si le ref est null ou introuvable.
     */
    private fun resolveTaxonomy(censusRef: String?): TaxonomyResult {
        if (censusRef.isNullOrEmpty()) return TaxonomyResult()
        val path = findPathToNode(censusRoots, censusRef) ?: return TaxonomyResult()

        var ordre   = ""
        var famille = ""
        var genre   = ""
        var espece  = ""

        for (node in path) {
            when (node.type) {
                CensusType.ORDER   -> ordre   = node.name
                CensusType.FAMILY  -> famille = node.name
                CensusType.GENUS   -> genre   = node.name
                CensusType.SPECIES -> espece  = node.name
            }
        }

        val maxLevel = when (path.last().type) {
            CensusType.ORDER   -> "ordre"
            CensusType.FAMILY  -> "famille"
            CensusType.GENUS   -> "genre"
            CensusType.SPECIES -> "espèce"
        }

        return TaxonomyResult(ordre, famille, genre, espece, maxLevel)
    }

    data class TaxonomyResult(
        val ordre:    String = "",
        val famille:  String = "",
        val genre:    String = "",
        val espece:   String = "",
        val maxLevel: String = ""
    )

    /**
     * Construit le contenu CSV à partir des photos filtrées.
     * Appelé depuis l'Activity après confirmation de l'utilisateur.
     */
    fun buildCsvContent(): String {
        val campus = _campusList.value ?: emptyList()
        var result = _allPictures.value ?: emptyList()

        // Re-applique les filtres pour être sûr d'avoir les bonnes données
        filterCampusId?.let { campusFilter ->
            result = result.filter { photo ->
                val loc = photo["location"] as? Map<*, *> ?: return@filter false
                val lat = loc["latitude"]  as? Double    ?: return@filter false
                val lon = loc["longitude"] as? Double    ?: return@filter false
                val inAnyCampus = campus.any { c ->
                    distanceMeters(lat, lon, c.latitudeCenter, c.longitudeCenter) <= c.radius
                }
                if (campusFilter == "HORS_CAMPUS") !inAnyCampus
                else {
                    val target = campus.find { it.name == campusFilter }
                    target != null &&
                            distanceMeters(lat, lon, target.latitudeCenter, target.longitudeCenter) <= target.radius
                }
            }
        }
        if (filterDateFrom != null && filterDateTo != null) {
            result = result.filter { photo ->
                val date = timestampToDate(photo["timestamp"])
                date != null && date >= filterDateFrom!!.time && date <= filterDateTo!!.time
            }
        }
        result = when (filterValidation) {
            ValidationFilter.ONLY_VALIDATED    ->
                result.filter { (it["adminValidated"] as? Boolean) == true }
            ValidationFilter.VALIDATED_AND_NOT ->
                result.filter { it.containsKey("adminValidated") }
        }

        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        val sb  = StringBuilder()

        // En-tête
        sb.appendLine("adminValidated;altitude;longitude;latitude;date;maxLevel;ordre;famille;genre;espece")

        for (photo in result) {
            val loc            = photo["location"] as? Map<*, *>
            val adminValidated = (photo["adminValidated"] as? Boolean) ?: false
            val altitude       = (loc?.get("altitude")  as? Double) ?: 0.0
            val longitude      = (loc?.get("longitude") as? Double) ?: 0.0
            val latitude       = (loc?.get("latitude")  as? Double) ?: 0.0
            val date           = timestampToDate(photo["timestamp"])?.let { fmt.format(it) } ?: ""
            val censusRef      = photo["censusRef"] as? String
            val tax            = resolveTaxonomy(censusRef)

            sb.appendLine(
                "${adminValidated};${altitude};${longitude};${latitude};${date};" +
                        "${tax.maxLevel};${tax.ordre};${tax.famille};${tax.genre};${tax.espece}"
            )
        }

        return sb.toString()
    }

    private fun timestampToDate(ts: Any?): Date? = when (ts) {
        is com.google.firebase.Timestamp -> ts.toDate()
        is Date                          -> ts
        else                             -> null
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

class ExportDataViewModelFactory : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        ExportDataViewModel() as T
}