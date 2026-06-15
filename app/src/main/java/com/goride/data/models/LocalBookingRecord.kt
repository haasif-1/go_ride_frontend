package com.goride.data.models

data class LocalBookingRecord(
    val rideId: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val vehicleType: String,
    val fare: Float,
    val driverName: String,
    val driverVehicle: String,
    val driverPlate: String,
    val status: String,
    val bookedAt: String          // ISO-8601 formatted date-time string
)
