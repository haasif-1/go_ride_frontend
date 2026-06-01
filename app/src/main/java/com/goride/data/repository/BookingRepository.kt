package com.goride.data.repository

import com.goride.data.api.ApiService
import com.goride.data.models.RideRequest

class BookingRepository(private val apiService: ApiService) {
    suspend fun requestRide(request: RideRequest) = apiService.requestRide(request)
    suspend fun getRideHistory() = apiService.getRideHistory()
    suspend fun getActiveRide() = apiService.getActiveRide()
}