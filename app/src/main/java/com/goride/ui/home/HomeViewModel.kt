package com.goride.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.goride.data.models.LocationModel

class HomeViewModel : ViewModel() {

    private val _recentLocations = MutableLiveData<List<LocationModel>>()
    val recentLocations: LiveData<List<LocationModel>> = _recentLocations

    init {
        loadRecentLocations()
    }

    private fun loadRecentLocations() {
        _recentLocations.value = listOf(
            LocationModel("Home", "2972 Westheimer Rd. Santa Ana, Illinois 85486", 0.0, 0.0),
            LocationModel("Office", "1901 Thornridge Cir. Shiloh, Hawaii 81063", 0.0, 0.0),
            LocationModel("Shopping Center", "4140 Parker Rd. Allentown, New Mexico 31134", 0.0, 0.0)
        )
    }
}