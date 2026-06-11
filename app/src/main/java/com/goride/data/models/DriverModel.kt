package com.goride.data.models

data class DriverModel(
    val name: String,
    val rating: Float,
    val vehicle: String,
    val plateNumber: String,
    val etaMinutes: Int,
    val fare: Float
)
