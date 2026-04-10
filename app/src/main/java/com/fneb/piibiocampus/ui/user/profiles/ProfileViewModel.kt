package com.fneb.piibiocampus.ui.user.profiles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel partagé par [MyProfileFragment] (userId = null → utilisateur courant)
 * et [UserProfileFragment] (userId fourni → profil d'un autre utilisateur).
 */
class ProfileViewModel(private val userId: String?) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── États UI ──────────────────────────────────────────────────────────────

    private val _profileState = MutableLiveData<UiState<UserProfile>>(UiState.Idle)
    val profileState: LiveData<UiState<UserProfile>> = _profileState

    private val _photosState = MutableLiveData<UiState<List<Map<String, Any>>>>(UiState.Idle)
    val photosState: LiveData<UiState<List<Map<String, Any>>>> = _photosState

    // ── ID résolu ─────────────────────────────────────────────────────────────

    /** UID effectif une fois résolu (null tant que non chargé). */
    private var resolvedUserId: String? = null

    private var photosListener: ListenerRegistration? = null

    // ── Chargement ────────────────────────────────────────────────────────────

    init {
        loadProfile()
    }

    /**
     * Charge le profil utilisateur.
     * Si [userId] est null, récupère l'utilisateur courant.
     * Lance aussi l'écoute des photos une fois l'UID résolu.
     */
    fun loadProfile() {
        _profileState.value = UiState.Loading
        resolvedUserId = null // ✅ Reset avant chaque rechargement
        scope.launch {
            try {
                val uid: String
                val profile: UserProfile?

                if (userId == null) {
                    // Profil personnel
                    val currentUser = UserDao.getCurrentUser()
                        ?: throw AppException.NotAuthenticated()
                    uid     = currentUser.uid
                    profile = UserDao.getCurrentUserProfile()
                } else {
                    // Profil d'un autre utilisateur
                    uid     = userId
                    profile = UserDao.getUserProfileById(userId)
                }

                resolvedUserId = uid

                if (profile != null) {
                    _profileState.postValue(UiState.Success(profile))
                } else {
                    _profileState.postValue(UiState.Error(AppException.DocumentNotFound()))
                }

                // Lance l'écoute des photos seulement pour le profil personnel,
                // et un chargement one-shot pour les autres profils.
                if (userId == null) {
                    startPhotosListener(uid)
                } else {
                    loadPhotosOneShot(uid)
                }

            } catch (e: AppException) {
                _profileState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _profileState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    /**
     * Écoute temps-réel des photos (profil personnel uniquement).
     * Remplace le listener existant si appelé plusieurs fois.
     */
    fun startPhotosListener(uid: String) {
        photosListener?.remove()
        _photosState.value = UiState.Loading
        photosListener = PictureDao.listenToPicturesByUserEnrichedSortedByDate(
            userId   = uid,
            onUpdate = { photos ->
                _photosState.postValue(UiState.Success(photos))
            },
            onError  = { e ->
                _photosState.postValue(UiState.Error(e))
            }
        )
    }

    /**
     * Chargement one-shot des photos (profil d'un autre utilisateur).
     */
    private fun loadPhotosOneShot(uid: String) {
        _photosState.value = UiState.Loading
        scope.launch {
            try {
                val photos = PictureDao.getPicturesByUserEnrichedSortedByDate(uid)
                _photosState.postValue(UiState.Success(photos))
            } catch (e: AppException) {
                _photosState.postValue(UiState.Error(e))
            } catch (e: Exception) {
                _photosState.postValue(UiState.Error(AppException.Unknown(e)))
            }
        }
    }

    /** Recharge les photos après une modification (ex. retour du PicturesViewerFragment). */
    fun reloadPhotos() {
        val uid = resolvedUserId ?: return
        if (userId == null) startPhotosListener(uid) else loadPhotosOneShot(uid)
    }

    override fun onCleared() {
        super.onCleared()
        photosListener?.remove()
        scope.cancel()
    }
}

// ── Factories ─────────────────────────────────────────────────────────────────

/** Factory pour MyProfileFragment — pas d'userId, on prend l'utilisateur courant. */
class MyProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(userId = null) as T
    }
}

/** Factory pour UserProfileFragment — userId obligatoire. */
class UserProfileViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(userId = userId) as T
    }
}