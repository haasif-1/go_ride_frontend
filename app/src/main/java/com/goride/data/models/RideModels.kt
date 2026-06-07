package com.goride.data.models

import com.google.gson.annotations.SerializedName

// ── Ride Request ───────────────────────────────────────────────────────────────

data class RideRequest(
    @SerializedName("pickup_lat")      val pickupLat: Double,
    @SerializedName("pickup_lng")      val pickupLng: Double,
    @SerializedName("destination_lat") val destinationLat: Double,
    @SerializedName("destination_lng") val destinationLng: Double,
    @SerializedName("vehicle_type")    val vehicleType: String
)

// ── API Envelope ───────────────────────────────────────────────────────────────

/**
 * Top-level envelope returned by all ride endpoints:
 * { "success": true, "message": "...", "data": { ... } }
 */
data class RideApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data")    val data: RideData?
)

// ── Ride Data ──────────────────────────────────────────────────────────────────

/**
 * The "data" object inside every ride response.
 * Fields are derived from the verified backend payload.
 *
 * Legacy alias kept so that BookingsFragment / RideHistoryAdapter /
 * RideHistoryViewModel continue to compile without churn.
 */
data class RideData(
    @SerializedName("id")               val id: String,
    @SerializedName("status")           val status: String,
    @SerializedName("vehicle_type")     val vehicleType: String,
    @SerializedName("fare")             val fare: Double,
    @SerializedName("distance")         val distance: Double,
    @SerializedName("duration")         val duration: Int,
    @SerializedName("pickup_lat")       val pickupLat: String,
    @SerializedName("pickup_lng")       val pickupLng: String,
    @SerializedName("destination_lat")  val destinationLat: String,
    @SerializedName("destination_lng")  val destinationLng: String,
    @SerializedName("requested_at")     val requestedAt: String
) {
    // ── Compatibility helpers for the Bookings / History screens ───────────────
    // These computed properties map old RideResponse field names to new RideData
    // fields so that BookingsFragment, RideHistoryAdapter, and RideHistoryViewModel
    // require only import changes, not structural rewrites.

    /** Human-readable short Ride ID, e.g. "#A7B5C8D2" */
    val displayId: String get() = "#${id.take(8).uppercase()}"

    /** Formatted fare string, e.g. "Rs. 245" */
    val fareFormatted: String get() = "Rs. %.0f".format(fare)

    /** Pickup coordinates displayed as a fallback address */
    val pickupAddress: String get() = "$pickupLat, $pickupLng"

    /** Destination coordinates displayed as a fallback address */
    val destinationAddress: String get() = "$destinationLat, $destinationLng"

    /** Vehicle type formatted for display */
    val vehicleName: String get() = vehicleType

    /** No driver info in this version of the API */
    val driverName: String? get() = null

    /** Alias for requestedAt so history adapter can use createdAt */
    val createdAt: String get() = requestedAt
}

// ── Backward-compatibility typealias ──────────────────────────────────────────

/**
 * Keeps all existing usages of "RideResponse" compiling without any changes
 * to the files that reference it.
 */
typealias RideResponse = RideData