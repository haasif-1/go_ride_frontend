package com.goride.data.repository

import com.goride.data.api.ApiService

class MainRepository(
    private val apiService: ApiService
) {

    suspend fun getProfile() =
        apiService.getProfile()

    suspend fun getRideHistory() =
        apiService.getRideHistory()

    suspend fun getActiveRide() =
        apiService.getActiveRide()
}