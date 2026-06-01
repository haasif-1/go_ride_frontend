package com.goride.data.models

import com.google.gson.annotations.SerializedName

data class RideRequest(
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("destination_lat") val destinationLat: Double,
    @SerializedName("destination_lng") val destinationLng: Double,
    @SerializedName("vehicle_type") val vehicleType: String
)

data class RideResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("destination_address") val destinationAddress: String,
    @SerializedName("fare") val fare: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("vehicle_name") val vehicleName: String? = null,
    @SerializedName("vehicle_number") val vehicleNumber: String? = null
)