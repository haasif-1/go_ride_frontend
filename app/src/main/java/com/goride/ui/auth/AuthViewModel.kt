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

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Result<RegisterResponse>>()
    val registerResult: LiveData<Result<RegisterResponse>> = _registerResult

    private val _userProfile = MutableLiveData<Result<UserResponse>>()
    val userProfile: LiveData<Result<UserResponse>> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val TAG = "AuthViewModel"

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.login(LoginRequest(email, password))
                Log.d(TAG, "LOGIN_HTTP_CODE: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "LOGIN_RESPONSE: $body")
                    
                    // Corrected field mapping to match backend JSON: body.data.access
                    val accessToken = body.data?.access
                    val userEmail = body.data?.user?.email
                    
                    Log.d(TAG, "LOGIN_TOKEN: $accessToken")
                    Log.d(TAG, "LOGIN_USER: ${body.data?.user}")

                    if (!accessToken.isNullOrEmpty() && !userEmail.isNullOrEmpty()) {
                        dataStoreManager.saveSession(accessToken, userEmail)
                        _loginResult.value = Result.success(body)
                    } else {
                        Log.e(TAG, "LOGIN_ERROR: Missing session data. Access: $accessToken, Email: $userEmail")
                        _loginResult.value = Result.failure(Exception("Invalid session data received"))
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e(TAG, "LOGIN_ERROR_BODY: $errorStr")
                    val errorMsg = parseError(errorStr) ?: response.message()
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN_EXCEPTION: ${e.message}", e)
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.register(RegisterRequest(email, password, confirmPassword))
                Log.d(TAG, "REGISTER_HTTP_CODE: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "REGISTER_RESPONSE: ${response.body()}")
                    _registerResult.value = Result.success(response.body()!!)
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e(TAG, "REGISTER_ERROR_BODY: $errorStr")
                    val errorMsg = parseError(errorStr) ?: response.message()
                    _registerResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "REGISTER_EXCEPTION: ${e.message}", e)
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseError(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            val jsonObject = JSONObject(errorBody)
            when {
                jsonObject.has("message") -> jsonObject.getString("message")
                jsonObject.has("detail") -> jsonObject.getString("detail")
                jsonObject.has("email") -> {
                    val emailError = jsonObject.optJSONArray("email")
                    if (emailError != null) "Email: " + emailError.getString(0) else "Email error"
                }
                jsonObject.has("non_field_errors") -> {
                    val nfError = jsonObject.optJSONArray("non_field_errors")
                    if (nfError != null) nfError.getString(0) else "Authentication error"
                }
                else -> errorBody
            }
        } catch (e: Exception) {
            errorBody
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    _userProfile.value = Result.success(response.body()!!)
                } else {
                    _userProfile.value = Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                _userProfile.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
