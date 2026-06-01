package com.goride.data.api

import com.goride.data.models.RideRequest
import com.goride.data.models.RideResponse
import com.goride.data.models.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/users/profile/")
    suspend fun getProfile(): Response<UserResponse>

    @POST("api/rides/request/")
    suspend fun requestRide(@Body request: RideRequest): Response<RideResponse>

    @GET("api/rides/history/")
    suspend fun getRideHistory(): Response<List<RideResponse>>

    @GET("api/rides/active/")
    suspend fun getActiveRide(): Response<RideResponse>
}