package com.goride.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.goride.data.models.LocationModel
import com.goride.data.repository.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    private val _recentLocations = MutableLiveData<List<LocationModel>>()
    val recentLocations: LiveData<List<LocationModel>> = _recentLocations

    init {
        loadRecentLocations()
    }

    private fun loadRecentLocations() {
        viewModelScope.launch {
            dataStoreManager.recentLocations.collect { locations ->
                _recentLocations.postValue(locations)
            }
        }
    }

    fun addRecentLocation(location: LocationModel) {
        viewModelScope.launch {
            val currentLocations = dataStoreManager.recentLocations.first().toMutableList()

            // Remove duplicate if exists to move it to the top
            currentLocations.removeAll { it.name == location.name && it.latitude == location.latitude && it.longitude == location.longitude }

            // Add to top
            currentLocations.add(0, location)

            // Keep max 10
            if (currentLocations.size > 10) {
                currentLocations.removeAt(currentLocations.size - 1)
            }

            dataStoreManager.saveRecentLocations(currentLocations)
        }
    }

    fun clearRecentLocations() {
        viewModelScope.launch {
            dataStoreManager.saveRecentLocations(emptyList())
        }
    }
}