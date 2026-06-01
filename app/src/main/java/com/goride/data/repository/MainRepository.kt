package com.goride.data.repository

import com.goride.data.api.ApiService
import javax.inject.Inject

class MainRepository(private val apiService: ApiService) {
    
    suspend fun login(request: Any) = apiService.login(request)
    
    suspend fun getRides() = apiService.getRides()
}