package com.goride.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideData
import com.goride.data.models.RideRequest
import com.goride.data.repository.BookingRepository
import kotlinx.coroutines.launch

class BookingViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _rideBookingResult = MutableLiveData<Result<RideData>>()
    val rideBookingResult: LiveData<Result<RideData>> = _rideBookingResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun requestRide(
        pickupLat: Double,
        pickupLng: Double,
        destLat: Double,
        destLng: Double,
        vehicleType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = RideRequest(pickupLat, pickupLng, destLat, destLng, vehicleType)
                val response = repository.requestRide(request)

                when {
                    response.isSuccessful && response.body() != null -> {
                        val body = response.body()!!
                        if (body.success && body.data != null) {
                            _rideBookingResult.value = Result.success(body.data)
                        } else {
                            _rideBookingResult.value = Result.failure(
                                Exception(body.message.ifEmpty { "Booking failed" })
                            )
                        }
                    }
                    response.code() == 401 -> {
                        _rideBookingResult.value = Result.failure(
                            Exception("Session expired. Please log in again.")
                        )
                    }
                    response.code() == 400 -> {
                        _rideBookingResult.value = Result.failure(
                            Exception("Invalid request. Please check your inputs.")
                        )
                    }
                    else -> {
                        _rideBookingResult.value = Result.failure(
                            Exception("Server error (${response.code()}). Please try again.")
                        )
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _rideBookingResult.value = Result.failure(
                    Exception("No internet connection. Please check your network.")
                )
            } catch (e: java.net.SocketTimeoutException) {
                _rideBookingResult.value = Result.failure(
                    Exception("Request timed out. Please try again.")
                )
            } catch (e: Exception) {
                _rideBookingResult.value = Result.failure(
                    Exception(e.message ?: "Unexpected error occurred.")
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}