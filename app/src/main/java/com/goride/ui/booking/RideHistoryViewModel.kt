package com.goride.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideData
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
                // ── Active ride ────────────────────────────────────────────────
                val activeResponse = repository.getActiveRide()
                if (activeResponse.isSuccessful && activeResponse.body() != null) {
                    val body = activeResponse.body()!!
                    // Unwrap envelope: data is nullable (null = no active ride)
                    _activeRide.value = Result.success(if (body.success) body.data else null)
                } else {
                    // 404 or similar means no active ride — not an error for the UI
                    _activeRide.value = Result.success(null)
                }

                // ── Ride history ───────────────────────────────────────────────
                val historyResponse = repository.getRideHistory()
                if (historyResponse.isSuccessful && historyResponse.body() != null) {
                    // History endpoint returns List<RideApiResponse>; extract each data object
                    val rideList: List<RideData> = historyResponse.body()!!
                        .mapNotNull { envelope -> if (envelope.success) envelope.data else null }
                    _rideHistory.value = Result.success(rideList)
                } else {
                    _rideHistory.value = Result.failure(
                        Exception("Failed to load history (${historyResponse.code()})")
                    )
                }
            } catch (e: Exception) {
                _activeRide.value = Result.success(null)
                _rideHistory.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}