package com.goride.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideData
import com.goride.data.repository.BookingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RideTrackingViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _activeRide = MutableLiveData<Result<RideData>>()
    val activeRide: LiveData<Result<RideData>> = _activeRide

    private var pollingJob: Job? = null

    fun startTracking() {
        if (pollingJob != null && pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val response = repository.getActiveRide()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success && body.data != null) {
                            val ride = body.data
                            _activeRide.value = Result.success(ride)

                            val status = ride.status.uppercase()
                            if (status == "COMPLETED" || status == "CANCELLED") {
                                stopTracking()
                                break
                            }
                        } else {
                            _activeRide.value = Result.failure(
                                Exception(body.message.ifEmpty { "Failed to load active ride details" })
                            )
                        }
                    } else {
                        _activeRide.value = Result.failure(
                            Exception("Server error (${response.code()})")
                        )
                    }
                } catch (e: Exception) {
                    _activeRide.value = Result.failure(e)
                }
                delay(5000)
            }
        }
    }

    fun stopTracking() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
