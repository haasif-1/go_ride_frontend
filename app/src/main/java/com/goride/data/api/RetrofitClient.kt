package com.goride.data.api

import android.content.Context
import android.util.Log
import com.goride.data.repository.DataStoreManager
import com.goride.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"

    // dataStoreManager is initialized lazily from init() — guaranteed before
    // any API call because GoRideApp.onCreate() calls RetrofitClient.init() first.
    private var dataStoreManager: DataStoreManager? = null

    fun init(context: Context) {
        // Always use applicationContext to avoid leaking Activity context
        dataStoreManager = DataStoreManager(context.applicationContext)
        Log.d(TAG, "RetrofitClient initialized with context: ${context.applicationContext}")
    }

    // ── Logging interceptor ────────────────────────────────────────────────────

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OkHttp", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ── Auth interceptor ───────────────────────────────────────────────────────
    // Reads the access token from DataStore on every request and attaches it.
    // Uses a lambda interceptor so it evaluates dataStoreManager at call-time,
    // not at object-initialisation time — this is the critical fix.

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val isBackendRequest = originalRequest.url.toString().startsWith(Constants.BASE_URL)
                val isPublicEndpoint = originalRequest.url.encodedPath.contains("/api/auth/")

                val dsm = dataStoreManager
                val token: String? = if (dsm != null) {
                    runBlocking {
                        try {
                            val t = dsm.authToken.first()
                            Log.e("AUTH_AUDIT", "DATASTORE_TOKEN=${if (t.isNullOrBlank()) "EMPTY" else t}")
                            t
                        } catch (e: Exception) {
                            Log.e("AUTH_AUDIT", "DATASTORE_TOKEN=EXCEPTION: ${e.message}")
                            null
                        }
                    }
                } else {
                    Log.e("AUTH_AUDIT", "DATASTORE_TOKEN=NULL (DataStoreManager not initialised)")
                    null
                }

                if (isBackendRequest && !isPublicEndpoint) {
                    Log.e("AUTH_AUDIT", "AUTH_REQUEST_START")
                }

                val request = if (isBackendRequest && !isPublicEndpoint && !token.isNullOrBlank()) {
                    Log.e("AUTH_AUDIT", "HEADER_TOKEN=$token")
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }

                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── Nominatim client (no auth, custom User-Agent) ─────────────────────────

    private val nominatimClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GoRide Android App")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── Retrofit instances ─────────────────────────────────────────────────────

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

    private val osrmRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── Service accessors ──────────────────────────────────────────────────────

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val nominatimApi: NominatimApiService by lazy {
        nominatimRetrofit.create(NominatimApiService::class.java)
    }

    val osrmApi: OsrmApiService by lazy {
        osrmRetrofit.create(OsrmApiService::class.java)
    }
}