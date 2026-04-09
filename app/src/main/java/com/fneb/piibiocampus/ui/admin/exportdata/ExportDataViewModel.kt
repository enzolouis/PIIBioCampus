package com.fneb.piibiocampus.ui.admin.exportdata

import androidx.lifecycle.*
import com.fneb.piibiocampus.data.dao.CampusDao
import com.fneb.piibiocampus.data.dao.CensusDao
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.model.Campus
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.ui.user.census.CensusNode
import com.fneb.piibiocampus.ui.user.census.CensusType
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.get
import kotlin.math.*

enum class ValidationFilter { ONLY_VALIDATED, VALIDATED_AND_NOT }

data class ExportRow(
    val adminValidated: Boolean,
    val altitude:       Double,
    val longitude:      Double,
    val latitude:       Double,
    val date:           String,
    val maxLevel:       String,
    val ordre:          String,
    val famille:        String,
    val genre:          String,
    val espece:         String
)

class ExportDataViewModel : ViewModel() {

    // ── État de chargement (Loading / Success / Error) ────────────────────────

    private val _loadState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val loadState: LiveData<UiState<Unit>> = _loadState

    // ── Données ───────────────────────────────────────────────────────────────

    private val _allPictures = MutableLiveData<List<Map<String, Any>>>(emptyList())

    private val _campusList = MutableLiveData<List<Campus>>(emptyList())
    val campusList: LiveData<List<Campus>> = _campusList

    private var censusRoots: List<CensusNode> = emptyList()

    // ── Filtres ───────────────────────────────────────────────────────────────

    var filterCampusId:   String?           = null
    var filterDateFrom:   Calendar?         = null
    var filterDateTo:     Calendar?         = null
    var filterValidation: ValidationFilter  = ValidationFilter.ONLY_VALIDATED

    // ── Résultats filtrés ─────────────────────────────────────────────────────

    private val _filteredCount = MutableLiveData(0)
    val filteredCount: LiveData<Int> = _filteredCount

    private val _previewRows = MutableLiveData<List<ExportRow>>(emptyList())
    val previewRows: LiveData<List<ExportRow>> = _previewRows

    // ── Chargement ────────────────────────────────────────────────────────────

    fun loadAll() {
        _loadState.value = UiState.Loading

        // Campus — non bloquant : erreur loguée via UiState mais on continue
        CampusDao.getAll(
            onComplete = { _campusList.postValue(it) },
            onError    = { e ->
                // On notifie l'erreur campus mais on ne bloque pas le reste
                _campusList.postValue(emptyList())
                _loadState.postValue(UiState.Error(e))
            }
        )

        // Arbre de recensement — non bloquant : colonnes taxonomiques seront vides si échec
        CensusDao.fetchCensusTreeFull(
            onComplete = { _, roots -> censusRoots = roots },
            onError    = { /* non bloquant, taxonomie vide */ }
        )

        // Photos — bloquant : sans elles, rien à exporter
        PictureDao.getAllPicturesEnriched(
            onSuccess = { list ->
                _allPictures.postValue(list)
                _loadState.postValue(UiState.Success(Unit))
                applyFilters()
            },
            onError = { e ->
                // AppException directement (getAllPicturesEnriched → onError: (AppException))
                _loadState.postValue(UiState.Error(e))
            }
        )
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    fun applyFilters() {
        val all = _allPictures.value
        if (all.isNullOrEmpty() && _loadState.value is UiState.Loading) return

        val campus = _campusList.value ?: emptyList()
        var result = all ?: emptyList()

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

        _filteredCount.postValue(result.size)

        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        _previewRows.postValue(
            result.take(4).map { photo -> toExportRow(photo, fmt) }
        )
    }

    fun resetFilters() {
        filterCampusId   = null
        filterDateFrom   = null
        filterDateTo     = null
        filterValidation = ValidationFilter.ONLY_VALIDATED
        applyFilters()
    }

    // ── Construction du CSV ───────────────────────────────────────────────────

    fun buildCsvContent(): String {
        val campus = _campusList.value ?: emptyList()
        var result = _allPictures.value ?: emptyList()

        // Re-applique les filtres pour garantir la cohérence avec l'aperçu
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

        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        return buildString {
            appendLine("adminValidated;altitude;longitude;latitude;date;maxLevel;ordre;famille;genre;espece")
            for (photo in result) {
                val row = toExportRow(photo, fmt)
                appendLine("${row.adminValidated};${row.altitude};${row.longitude};${row.latitude};" +
                        "${row.date};${row.maxLevel};${row.ordre};${row.famille};${row.genre};${row.espece}")
            }
        }
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private fun toExportRow(photo: Map<String, Any>, fmt: SimpleDateFormat): ExportRow {
        val loc            = photo["location"] as? Map<*, *>
        val adminValidated = (photo["adminValidated"] as? Boolean) ?: false
        val altitude       = (loc?.get("altitude")  as? Double) ?: 0.0
        val longitude      = (loc?.get("longitude") as? Double) ?: 0.0
        val latitude       = (loc?.get("latitude")  as? Double) ?: 0.0
        val date           = timestampToDate(photo["timestamp"])?.let { fmt.format(it) } ?: ""
        val tax            = resolveTaxonomy(photo["censusRef"] as? String)
        return ExportRow(adminValidated, altitude, longitude, latitude, date,
            tax.maxLevel, tax.ordre, tax.famille, tax.genre, tax.espece)
    }

    private fun findPathToNode(
        nodes: List<CensusNode>, targetId: String,
        currentPath: List<CensusNode> = emptyList()
    ): List<CensusNode>? {
        for (node in nodes) {
            val path = currentPath + node
            if (node.id == targetId) return path
            findPathToNode(node.children, targetId, path)?.let { return it }
        }
        return null
    }

    private fun resolveTaxonomy(censusRef: String?): TaxonomyResult {
        if (censusRef.isNullOrEmpty()) return TaxonomyResult()
        val path = findPathToNode(censusRoots, censusRef) ?: return TaxonomyResult()
        var ordre = ""; var famille = ""; var genre = ""; var espece = ""
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

    private fun timestampToDate(ts: Any?): Date? = when (ts) {
        is Timestamp -> ts.toDate()
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
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ExportDataViewModel() as T
}