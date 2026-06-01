package com.goride.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideRequest
import com.goride.data.models.RideResponse
import com.goride.data.repository.BookingRepository
import kotlinx.coroutines.launch

class BookingViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _rideBookingResult = MutableLiveData<Result<RideResponse>>()
    val rideBookingResult: LiveData<Result<RideResponse>> = _rideBookingResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun requestRide(pickupLat: Double, pickupLng: Double, destLat: Double, destLng: Double, vehicleType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = RideRequest(pickupLat, pickupLng, destLat, destLng, vehicleType)
                val response = repository.requestRide(request)
                if (response.isSuccessful && response.body() != null) {
                    _rideBookingResult.value = Result.success(response.body()!!)
                } else {
                    _rideBookingResult.value = Result.failure(Exception(response.message().ifEmpty { "Failed to book ride" }))
                }
            } catch (e: Exception) {
                _rideBookingResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}