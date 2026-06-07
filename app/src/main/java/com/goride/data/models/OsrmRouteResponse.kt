package com.goride.data.models

import com.google.gson.annotations.SerializedName

data class OsrmRouteResponse(
    @SerializedName("code") val code: String,
    @SerializedName("routes") val routes: List<OsrmRoute>,
    @SerializedName("waypoints") val waypoints: List<OsrmWaypoint>? = null
)

data class OsrmRoute(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("distance") val distance: Double,
    @SerializedName("weight_name") val weightName: String? = null,
    @SerializedName("weight") val weight: Double? = null
)

data class OsrmWaypoint(
    @SerializedName("name") val name: String? = null,
    @SerializedName("location") val location: List<Double>? = null
)
