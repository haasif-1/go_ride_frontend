package com.goride.data.repository

import com.goride.data.api.ApiService

class RideHistoryRepository(private val apiService: ApiService) {
    suspend fun getRideHistory() = apiService.getRideHistory()
    suspend fun getActiveRide() = apiService.getActiveRide()
}