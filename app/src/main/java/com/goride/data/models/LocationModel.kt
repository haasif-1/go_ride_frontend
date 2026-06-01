package com.goride.data.models

data class LocationModel(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: String? = null
)