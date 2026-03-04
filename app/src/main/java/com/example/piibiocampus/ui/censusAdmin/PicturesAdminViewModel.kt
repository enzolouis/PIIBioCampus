package com.example.piibiocampus.ui.admin

import androidx.lifecycle.*
import com.example.piibiocampus.data.dao.CampusDao
import com.example.piibiocampus.data.dao.PictureDao
import com.example.piibiocampus.data.dao.UserDao
import com.example.piibiocampus.data.model.Campus
import com.example.piibiocampus.data.model.UserProfile
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

enum class SortOrder { DESC, ASC }

class PicturesAdminViewModel : ViewModel() {

    // ── États ─────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // ── Données ───────────────────────────────────────────
    private val _allPictures = MutableLiveData<List<Map<String, Any>>>(emptyList())

    private val _allUsers = MutableLiveData<List<UserProfile>>(emptyList())
    val allUsers: LiveData<List<UserProfile>> = _allUsers

    private val _campusList = MutableLiveData<List<Campus>>(emptyList())
    val campusList: LiveData<List<Campus>> = _campusList

    // ── Filtres ───────────────────────────────────────────
    var filterUserId:    String?   = null
    var filterUserName:  String?   = null
    var filterCampusId:  String?   = null
    var filterValidated: Boolean?  = null
    var filterDateFrom:  Calendar? = null
    var filterDateTo:    Calendar? = null

    // ── Tri ───────────────────────────────────────────────
    // DESC = plus récent en premier (défaut), ASC = plus ancien en premier
    private val _sortOrder = MutableLiveData(SortOrder.DESC)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    // ── Résultat ──────────────────────────────────────────
    private val _filteredPictures = MutableLiveData<List<Map<String, Any>>>(emptyList())
    val filteredPictures: LiveData<List<Map<String, Any>>> = _filteredPictures

    // ─────────────────────────────────────────────────────

    fun loadAll() {
        _isLoading.value = true

        CampusDao.getAll(
            onComplete = { _campusList.postValue(it) },
            onError    = { _campusList.postValue(emptyList()) }
        )

        viewModelScope.launch {
            try {
                val users = UserDao.getAllUsersWithUid()
                _allUsers.postValue(users)
            } catch (e: Exception) {
                _allUsers.postValue(emptyList())
            }
        }

        PictureDao.getAllPicturesEnriched(
            onSuccess = { list ->
                _allPictures.postValue(list)
                _isLoading.postValue(false)
                // Affichage immédiat trié sans attendre campus (async)
                // Si des filtres sont déjà actifs on les réapplique,
                // sinon on poste directement la liste triée
                val hasActiveFilters = filterUserId != null || filterCampusId != null ||
                        filterValidated != null || filterDateFrom != null
                if (hasActiveFilters) {
                    applyFilters()
                } else {
                    val sorted = when (_sortOrder.value ?: SortOrder.DESC) {
                        SortOrder.DESC -> list.sortedByDescending { timestampToDate(it["timestamp"]) }
                        SortOrder.ASC  -> list.sortedBy          { timestampToDate(it["timestamp"]) }
                    }
                    _filteredPictures.postValue(sorted)
                }
            },
            onError = {
                _error.postValue(it.message)
                _isLoading.postValue(false)
            }
        )
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
        applyFilters()
    }

    fun applyFilters() {
        val campus = _campusList.value ?: emptyList()
        var result = _allPictures.value ?: emptyList()

        // Filtre utilisateur
        filterUserId?.let { uid ->
            result = result.filter { it["userRef"] == uid }
        }

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

        // Filtre validation
        filterValidated?.let { validated ->
            result = result.filter { (it["adminValidated"] as? Boolean ?: false) == validated }
        }

        // Filtre plage de dates
        if (filterDateFrom != null && filterDateTo != null) {
            result = result.filter { photo ->
                val date = timestampToDate(photo["timestamp"])
                date != null && date >= filterDateFrom!!.time && date <= filterDateTo!!.time
            }
        }

        // Tri par date
        result = when (_sortOrder.value ?: SortOrder.DESC) {
            SortOrder.DESC -> result.sortedByDescending { timestampToDate(it["timestamp"]) }
            SortOrder.ASC  -> result.sortedBy          { timestampToDate(it["timestamp"]) }
        }

        _filteredPictures.postValue(result)
    }

    fun resetFilters() {
        filterUserId    = null
        filterUserName  = null
        filterCampusId  = null
        filterValidated = null
        filterDateFrom  = null
        filterDateTo    = null
        applyFilters()
    }

    // ── Helpers ───────────────────────────────────────────

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

class PicturesAdminViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PicturesAdminViewModel() as T
}