package com.goride.data.repository

import com.goride.data.api.AuthApiService
import com.goride.data.api.ApiService
import com.goride.data.models.LoginRequest
import com.goride.data.models.RegisterRequest

class AuthRepository(
    private val authApiService: AuthApiService,
    private val apiService: ApiService
) {
    suspend fun login(request: LoginRequest) = authApiService.login(request)
    suspend fun register(request: RegisterRequest) = authApiService.register(request)
    suspend fun getProfile() = apiService.getProfile()
}