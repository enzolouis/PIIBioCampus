package com.example.piibiocampus.ui.map

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MapViewModel : ViewModel() {

    private val _pictures = MutableLiveData<List<Map<String, Any>>>()
    val pictures: LiveData<List<Map<String, Any>>> = _pictures

    private val TAG = "MapViewModel"

    fun loadAllPictures() {
        PictureDao.getAllPictures(
            onSuccess = { list ->
                _pictures.postValue(list)
            },
            onError = { e ->
                Log.e(TAG, "Erreur récupération pictures", e)
                _pictures.postValue(emptyList())
            }
        )
    }

    fun loadPicturesNear(lat: Double, lon: Double, radiusMeters: Double) {
        PictureDao.getPicturesNearLocation(
            lat, lon, radiusMeters,
            onSuccess = { list -> _pictures.postValue(list) },
            onError = { e ->
                Log.e(TAG, "Erreur récupération nearby pictures", e)
                _pictures.postValue(emptyList())
            }
        )
    }
}
