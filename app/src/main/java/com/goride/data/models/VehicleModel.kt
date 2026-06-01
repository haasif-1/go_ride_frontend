package com.goride.data.models

data class VehicleModel(
    val id: Int,
    val name: String,
    val price: String,
    val eta: String,
    val seats: String,
    val imageResId: Int,
    val isPopular: Boolean = false,
    var isSelected: Boolean = false
)