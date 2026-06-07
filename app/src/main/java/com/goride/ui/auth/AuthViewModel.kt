package com.goride.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.LoginRequest
import com.goride.data.models.LoginResponse
import com.goride.data.models.RegisterRequest
import com.goride.data.models.RegisterResponse
import com.goride.data.models.UserResponse
import com.goride.data.repository.AuthRepository
import com.goride.data.repository.DataStoreManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel(
    private val repository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val TAG = "AuthViewModel"

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Result<RegisterResponse>>()
    val registerResult: LiveData<Result<RegisterResponse>> = _registerResult

    private val _userProfile = MutableLiveData<Result<UserResponse>>()
    val userProfile: LiveData<Result<UserResponse>> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // ── Login ──────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.login(LoginRequest(email, password))
                Log.d(TAG, "LOGIN — HTTP ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "LOGIN — body.success=${body.success} | message=${body.message}")
                    Log.d(TAG, "LOGIN — data=${body.data}")

                    // Extract ONLY the access token — never the refresh token
                    val accessToken = body.data?.access
                    val userEmail   = body.data?.user?.email

                    Log.e("AUTH_AUDIT", "LOGIN_ACCESS=$accessToken")
                    Log.d(TAG, "LOGIN — accessToken=${
                        if (accessToken.isNullOrBlank()) "MISSING" else "${accessToken.take(30)}…"
                    }")
                    Log.d(TAG, "LOGIN — userEmail=${userEmail ?: "MISSING"}")

                    when {
                        !body.success -> {
                            // Backend returned 2xx but reported failure in the body
                            val msg = body.message ?: "Login failed"
                            Log.e(TAG, "LOGIN — backend reported failure: $msg")
                            _loginResult.value = Result.failure(Exception(msg))
                        }
                        accessToken.isNullOrBlank() -> {
                            Log.e(TAG, "LOGIN — access token is missing from response")
                            _loginResult.value = Result.failure(
                                Exception("Server did not return an access token")
                            )
                        }
                        userEmail.isNullOrBlank() -> {
                            Log.e(TAG, "LOGIN — user email is missing from response")
                            _loginResult.value = Result.failure(
                                Exception("Server did not return user information")
                            )
                        }
                        else -> {
                            Log.e("AUTH_AUDIT", "SAVING_TOKEN=$accessToken")
                            // ✅ Save ONLY the access token to DataStore
                            dataStoreManager.saveSession(accessToken, userEmail)
                            Log.d(TAG, "LOGIN — session saved successfully")

                            // Verify what was actually persisted
                            Log.d(TAG, "LOGIN — verifying persisted token…")
                            _loginResult.value = Result.success(body)
                        }
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e(TAG, "LOGIN — error body: $errorStr")
                    val errorMsg = parseError(errorStr) ?: response.message().ifEmpty { "Login failed" }
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN — exception: ${e.message}", e)
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Register ───────────────────────────────────────────────────────────────

    fun register(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.register(
                    RegisterRequest(email, password, confirmPassword)
                )
                Log.d(TAG, "REGISTER — HTTP ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "REGISTER — success: ${response.body()}")
                    _registerResult.value = Result.success(response.body()!!)
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e(TAG, "REGISTER — error body: $errorStr")
                    val errorMsg = parseError(errorStr) ?: response.message().ifEmpty { "Registration failed" }
                    _registerResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "REGISTER — exception: ${e.message}", e)
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Profile ────────────────────────────────────────────────────────────────

    fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _userProfile.value = Result.success(response.body()!!)
                } else {
                    Log.e(TAG, "PROFILE — HTTP ${response.code()}: ${response.message()}")
                    _userProfile.value = Result.failure(
                        Exception("Failed to load profile (${response.code()})")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "PROFILE — exception: ${e.message}", e)
                _userProfile.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Error parser ───────────────────────────────────────────────────────────

    private fun parseError(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            val json = JSONObject(errorBody)
            when {
                json.has("message")          -> json.getString("message")
                json.has("detail")           -> json.getString("detail")
                json.has("non_field_errors") -> json.optJSONArray("non_field_errors")
                    ?.getString(0) ?: "Authentication error"
                json.has("email")            -> "Email: " + (json.optJSONArray("email")
                    ?.getString(0) ?: "unknown error")
                else                         -> errorBody
            }
        } catch (e: Exception) {
            errorBody
        }
    }
}
