package com.goride.data.api

import com.goride.data.models.NominatimPlace
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {

    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "en",
        @Query("limit") limit: Int = 5
    ): List<NominatimPlace>
}