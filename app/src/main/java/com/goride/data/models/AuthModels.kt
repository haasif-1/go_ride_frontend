package com.goride.data.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: LoginData?
)

data class LoginData(
    @SerializedName("access") val access: String?,
    @SerializedName("refresh") val refresh: String?,
    @SerializedName("user") val user: UserResponse?
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("confirm_password")
    val confirmPassword: String,

    @SerializedName("role")
    val role: String = "USER"
)

data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserResponse?
)

data class UserResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String
)