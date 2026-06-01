package com.goride.ui.auth

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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    dataStoreManager.saveSession(body.accessToken, body.user.email)
                    _loginResult.value = Result.success(body)
                } else {
                    _loginResult.value = Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.register(RegisterRequest(fullName, email, password))
                if (response.isSuccessful && response.body() != null) {
                    _registerResult.value = Result.success(response.body()!!)
                } else {
                    _registerResult.value = Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
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