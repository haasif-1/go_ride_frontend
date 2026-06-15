package com.goride.data.models

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String? = null,
    val role: String = "Rider"
)

data class RideStats(
    val totalBookings: Int = 0,
    val completedBookings: Int = 0,
    val cancelledBookings: Int = 0,
    val lastBookingDate: String = "--",
    val preferredVehicle: String = "--"
)
