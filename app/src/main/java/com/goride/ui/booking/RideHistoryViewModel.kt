package com.goride.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideResponse
import com.goride.data.repository.RideHistoryRepository
import kotlinx.coroutines.launch

class RideHistoryViewModel(private val repository: RideHistoryRepository) : ViewModel() {

    private val _rideHistory = MutableLiveData<Result<List<RideResponse>>>()
    val rideHistory: LiveData<Result<List<RideResponse>>> = _rideHistory

    private val _activeRide = MutableLiveData<Result<RideResponse?>>()
    val activeRide: LiveData<Result<RideResponse?>> = _activeRide

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchRideData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch active ride
                val activeResponse = repository.getActiveRide()
                if (activeResponse.isSuccessful) {
                    _activeRide.value = Result.success(activeResponse.body())
                } else {
                    _activeRide.value = Result.success(null)
                }

                // Fetch history
                val historyResponse = repository.getRideHistory()
                if (historyResponse.isSuccessful) {
                    _rideHistory.value = Result.success(historyResponse.body() ?: emptyList())
                } else {
                    _rideHistory.value = Result.failure(Exception(historyResponse.message()))
                }
            } catch (e: Exception) {
                _rideHistory.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}