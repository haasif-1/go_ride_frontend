package com.goride.data.api

import android.content.Context
import com.goride.data.repository.DataStoreManager
import com.goride.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.goride.data.api.NominatimApiService
import android.util.Log

object RetrofitClient {

    private lateinit var dataStoreManager: DataStoreManager

    fun init(context: Context) {
        dataStoreManager = DataStoreManager(context)
    }

    private val authInterceptor = Interceptor { chain ->
        chain.proceed(chain.request())
    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val nominatimClient = OkHttpClient.Builder()
        .addInterceptor { chain ->

            val request = chain.request()
                .newBuilder()
                .header(
                    "User-Agent",
                    "GoRide Android App"
                )
                .build()

            chain.proceed(request)
        }
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val nominatimRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(nominatimClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    val nominatimApi: NominatimApiService by lazy {
        nominatimRetrofit.create(NominatimApiService::class.java)
    }
}