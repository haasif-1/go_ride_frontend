package com.goride.ui.booking

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.databinding.FragmentDriverTrackingBinding
import com.goride.utils.PolylineDecoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class DriverTrackingFragment : BaseFragment<FragmentDriverTrackingBinding>(), OnMapReadyCallback {

    private val args: DriverTrackingFragmentArgs by navArgs()
    private var mapLibreMap: MapLibreMap? = null
    private var mapView: MapView? = null
    private var carIcon: Icon? = null

    // Persist which segment is currently displayed to avoid duplicate logic.
    // -1: pre-start, 1: after-start
    private var routeSegment: Int = -1

    // State: -1 = waiting for driver, 0 = ready to start, 1 = riding, 2 = destination reached
    private var rideState = -1
    private var movementJob: Job? = null
    private var statusJob: Job? = null
    private var driverMarker: Marker? = null

    private var driverStartLocation: LatLng? = null
    private var spawnToPickupPoints: List<LatLng> = emptyList()
    private var pickupToDestinationPoints: List<LatLng> = emptyList()

    // Keep the route being travelled and current segment polyline index for smooth progress visualization.
    private var activeRoutePoints: List<LatLng> = emptyList()
    private var activeRoutePolylineOutlineIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var activeRoutePolylinePrimaryIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var completedRoutePolylineOutlineIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var completedRoutePolylinePrimaryIds: MutableList<org.maplibre.android.annotations.Polyline>? = null

    private var remainingRoutePolylineOutlineIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var remainingRoutePolylinePrimaryIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var activeRouteStartIndex: Int = 0

    private val routeLineWidth: Float
        get() = 4f

    private val routeOutlineWidth: Float
        get() = 6f

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDriverTrackingBinding =
        FragmentDriverTrackingBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        carIcon = createCarIcon()
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun createCarIcon(): Icon? {
        return try {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car) ?: return null
            val density = resources.displayMetrics.density
            val size = (48 * density).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            IconFactory.getInstance(requireContext()).fromBitmap(bitmap)
        } catch (_: Exception) {
            null
        }
    }

    override fun setupUI() {
        binding.tvDriverName.text = args.driverName
        binding.tvDriverVehicle.text = args.driverVehicle
        binding.tvDriverPlate.text = args.driverPlate

        binding.tvFare.text = "Rs. %.0f".format(Locale.US, args.fare.toDouble())
        binding.tvDistance.text = "%.1f km".format(Locale.US, args.distance.toDouble())
        binding.tvDuration.text = "${args.duration} min"
        binding.tvVehicleType.text = args.vehicleType

        binding.tvEta.text = "Driver Assigned\nETA: 3 min"
        binding.btnCompleteRide.text = "Start Ride"
        binding.btnCompleteRide.isEnabled = false

        binding.btnCompleteRide.setOnClickListener {
            when (rideState) {
                0 -> {
                    rideState = 1
                    binding.btnCompleteRide.isEnabled = false
                    binding.tvEta.text = "Ride In Progress"
                    binding.btnCompleteRide.text = "Finish Ride"
                    startRideSimulation()
                }
                2 -> showRatingBottomSheet()
            }
        }
    }

    private fun showRatingBottomSheet() {
        val bottomSheet = RateDriverBottomSheet.newInstance(
            args.driverName,
            args.driverVehicle
        )

        bottomSheet.onNavigateHome = {
            findNavController().navigate(
                R.id.action_driverTrackingFragment_to_homeFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .build()
            )
        }

        bottomSheet.show(parentFragmentManager, "RateDriverBottomSheet")
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
            val pickup = LatLng(
                args.pickupLat.toDoubleOrNull() ?: 0.0,
                args.pickupLng.toDoubleOrNull() ?: 0.0
            )
            val destination = LatLng(
                args.destinationLat.toDoubleOrNull() ?: 0.0,
                args.destinationLng.toDoubleOrNull() ?: 0.0
            )
            fetchRoute(pickup, destination)
        }
    }

    private fun fetchRoute(pickup: LatLng, destination: LatLng) {
        // Generate random nearby driver start location.
        driverStartLocation = generateRandomNearbyLocation(pickup)

        // Phase 1: Driver Start -> Pickup
        driverStartLocation?.let { start ->
            val coords = "${start.longitude},${start.latitude};${pickup.longitude},${pickup.latitude}"
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.osrmApi.getRoute(coords)
                    if (response.isSuccessful && response.body() != null) {
                        val routeResponse = response.body()!!
                        if (routeResponse.code == "Ok" && routeResponse.routes.isNotEmpty()) {
                            val decodedPoints = PolylineDecoder.decode(routeResponse.routes[0].geometry)
                            spawnToPickupPoints = decodedPoints
                            drawPreStartRouteOnMap(decodedPoints, pickup, destination)
                            return@launch
                        }
                    }
                    drawStraightPreStartRoute(start, pickup, destination)
                } catch (_: Exception) {
                    drawStraightPreStartRoute(start, pickup, destination)
                }
            }
        }
    }


    private fun addDriverMarker(position: LatLng): Marker? {
        val map = mapLibreMap ?: return null
        val options = MarkerOptions()
            .position(position)
            .title("Driver")
        carIcon?.let { options.icon(it) }
        return map.addMarker(options)
    }

    private fun addPickupAndDestinationMarkers(pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        map.addMarker(MarkerOptions().position(destination).title("Destination"))
    }

    private fun addPickupMarker(pickup: LatLng) {
        val map = mapLibreMap ?: return
        map.addMarker(MarkerOptions().position(pickup).title("Pickup"))
    }


    // Draw both route segments (GREY completed, GREEN remaining) without map.clear().
    // NOTE: This replaces earlier single-segment progress logic.
    private fun drawRoutePolylines(points: List<LatLng>) {
        val map = mapLibreMap ?: return

        activeRoutePolylineOutlineIds?.forEach { map.removePolyline(it) }
        activeRoutePolylinePrimaryIds?.forEach { map.removePolyline(it) }

        val outlineColor = ContextCompat.getColor(requireContext(), R.color.route_outline)
        val routeColor = ContextCompat.getColor(requireContext(), R.color.route_primary)

        val outlinePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(outlineColor)
                .width(routeOutlineWidth)
        )

        val primaryPolyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(routeColor)
                .width(routeLineWidth)
        )

        activeRoutePolylineOutlineIds = mutableListOf(outlinePolyline)
        activeRoutePolylinePrimaryIds = mutableListOf(primaryPolyline)
    }



    // Draw split progress: GREY completed (behind), GREEN remaining (ahead)
    // Completed: [0..currentIndex), Remaining: [currentIndex..end)
    private fun drawRouteProgress(currentIndex: Int) {
        val map = mapLibreMap ?: return
        if (activeRoutePoints.isEmpty()) return

        val safeIndex = currentIndex.coerceIn(0, activeRoutePoints.lastIndex)

        val completed = activeRoutePoints.subList(0, safeIndex + 1)
        val remaining = activeRoutePoints.subList(safeIndex, activeRoutePoints.size)

        if (remaining.size < 2 || completed.size < 2) return

        val greyOutlineColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val greyPrimaryColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        val remainingOutlineColor = ContextCompat.getColor(requireContext(), R.color.route_outline)
        val remainingPrimaryColor = ContextCompat.getColor(requireContext(), R.color.route_primary)

        // Remove previously drawn split polylines
        completedRoutePolylineOutlineIds?.forEach { map.removePolyline(it) }
        completedRoutePolylinePrimaryIds?.forEach { map.removePolyline(it) }
        remainingRoutePolylineOutlineIds?.forEach { map.removePolyline(it) }
        remainingRoutePolylinePrimaryIds?.forEach { map.removePolyline(it) }

        completedRoutePolylineOutlineIds = null
        completedRoutePolylinePrimaryIds = null
        remainingRoutePolylineOutlineIds = null
        remainingRoutePolylinePrimaryIds = null

        val completedOutline = map.addPolyline(
            PolylineOptions()
                .addAll(completed)
                .color(greyOutlineColor)
                .width(routeOutlineWidth)
        )
        val completedPrimary = map.addPolyline(
            PolylineOptions()
                .addAll(completed)
                .color(greyPrimaryColor)
                .width(routeLineWidth)
        )

        val remainingOutline = map.addPolyline(
            PolylineOptions()
                .addAll(remaining)
                .color(remainingOutlineColor)
                .width(routeOutlineWidth)
        )
        val remainingPrimary = map.addPolyline(
            PolylineOptions()
                .addAll(remaining)
                .color(remainingPrimaryColor)
                .width(routeLineWidth)
        )

        completedRoutePolylineOutlineIds = mutableListOf(completedOutline)
        completedRoutePolylinePrimaryIds = mutableListOf(completedPrimary)
        remainingRoutePolylineOutlineIds = mutableListOf(remainingOutline)
        remainingRoutePolylinePrimaryIds = mutableListOf(remainingPrimary)
    }



    private fun fitCameraToRoute(points: List<LatLng>, extraPoint: LatLng? = null) {

        val map = mapLibreMap ?: return
        if (points.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        extraPoint?.let { boundsBuilder.include(it) }

        try {
            val bounds = boundsBuilder.build()
            mapView?.post {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            }
        } catch (_: Exception) {
            mapView?.post {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14.0))
            }
        }
    }

    // Draw only the pre-start segment: Driver -> Pickup.
    private fun drawPreStartRouteOnMap(points: List<LatLng>, pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        routeSegment = -1

        // Only markers: pickup (driver marker is separate)
        addPickupMarker(pickup)
        drawRoutePolylines(points)

        val startPosition = points.firstOrNull() ?: pickup
        driverMarker = addDriverMarker(startPosition)

        fitCameraToRoute(points, startPosition)
        startDriverApproachSimulation(points, pickup)
    }

    // Straight-line fallback for pre-start: Spawn -> Pickup.
    private fun drawStraightPreStartRoute(start: LatLng, pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        routeSegment = -1

        val fallbackPoints = listOf(start, pickup)

        addPickupMarker(pickup)
        drawRoutePolylines(fallbackPoints)

        driverMarker = addDriverMarker(start)

        fitCameraToRoute(fallbackPoints, start)
        startDriverApproachSimulation(fallbackPoints, pickup)
    }

    // Draw only the after-start segment: Pickup -> Destination.
    private fun drawPostStartRouteOnMap(points: List<LatLng>) {
        val map = mapLibreMap ?: return
        map.clear()
        routeSegment = 1

        val pickup = driverPickupLatLng()
        val destination = driverDestinationLatLng()

        addPickupAndDestinationMarkers(pickup, destination)
        drawRoutePolylines(points)

        val startPosition = points.firstOrNull() ?: pickup
        driverMarker = addDriverMarker(startPosition)

        fitCameraToRoute(points, startPosition)
    }

    // Straight-line fallback for after-start: Pickup -> Destination.
    private fun drawPostStartStraightRouteOnMap(pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        routeSegment = 1

        val fallbackPoints = listOf(pickup, destination)

        addPickupAndDestinationMarkers(pickup, destination)
        drawRoutePolylines(fallbackPoints)

        driverMarker = addDriverMarker(pickup)
        fitCameraToRoute(fallbackPoints, pickup)
    }

    // Existing helpers kept for compilation/backward compatibility.
    // They are not used by the updated flow but may be referenced elsewhere.
    private fun drawRouteOnMap(points: List<LatLng>, pickup: LatLng, destination: LatLng) {
        drawPreStartRouteOnMap(points, pickup, destination)
    }

    private fun drawStraightRoute(pickup: LatLng, destination: LatLng) {
        val start = if (pickupToDestinationPoints.isNotEmpty()) {
            pickupToDestinationPoints.first()
        } else {
            pickup
        }
        drawStraightPreStartRoute(start, pickup, destination)
    }




    // Random nearby location helper for the pre-start driver spawn.
    // Requirement: spawn within 300–600m radius of pickup.
    private fun generateRandomNearbyLocation(center: LatLng): LatLng {
        val minRadiusMeters = 300.0
        val maxRadiusMeters = 600.0

        // Convert meters -> degrees latitude.
        val radiusDegreesLatMin = minRadiusMeters / 111_320.0
        val radiusDegreesLatMax = maxRadiusMeters / 111_320.0

        val randomAngle = Random.nextDouble() * 2.0 * Math.PI

        // Ensure uniform-ish distribution within annulus: radius^2 proportional.
        val u = Random.nextDouble() // [0,1)
        val r2 = (radiusDegreesLatMin * radiusDegreesLatMin) +
                u * (radiusDegreesLatMax * radiusDegreesLatMax - radiusDegreesLatMin * radiusDegreesLatMin)
        val randomRadius = sqrt(r2)

        val lat = center.latitude + randomRadius * cos(randomAngle)
        val lon = center.longitude +
                (randomRadius * sin(randomAngle)) / maxOf(0.000001, cos(center.latitude * Math.PI / 180.0))

        return LatLng(lat, lon)
    }

    private fun driverPickupLatLng(): LatLng {
        return LatLng(
            args.pickupLat.toDoubleOrNull() ?: 0.0,
            args.pickupLng.toDoubleOrNull() ?: 0.0
        )
    }

    private fun driverDestinationLatLng(): LatLng {
        return LatLng(
            args.destinationLat.toDoubleOrNull() ?: 0.0,
            args.destinationLng.toDoubleOrNull() ?: 0.0
        )
    }



    private fun updateDriverPosition(point: LatLng, followCamera: Boolean) {
        driverMarker?.let { marker ->
            marker.position = point
            mapLibreMap?.updateMarker(marker)
        }

        // Progress visualization: hide route behind the car.


        if (followCamera) {
            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(point, 15.0)
            )
        }
    }


    private fun animateAlongRoute(
        path: List<LatLng>,
        delayMs: Long,
        followCamera: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        movementJob?.cancel()

        movementJob = viewLifecycleOwner.lifecycleScope.launch {

            val step = maxOf(1, path.size / 50)

            var index = 0

            while (index < path.size) {

                activeRouteStartIndex = index

                drawRouteProgress(index)

                updateDriverPosition(
                    path[index],
                    followCamera
                )


                delay(delayMs)

                index += step
            }

            onComplete?.invoke()
        }
    }

    private fun startDriverApproachSimulation(approachPath: List<LatLng>, pickup: LatLng) {

        movementJob?.cancel()
        statusJob?.cancel()

        // Initialize active route for progress visualization (Driver -> Pickup).
        activeRoutePoints = approachPath
        activeRouteStartIndex = 0
        activeRoutePolylineOutlineIds = null
        activeRoutePolylinePrimaryIds = null

        // Draw initial split progress.
        drawRouteProgress(0)



        val stepDelay = if (approachPath.size > 1) {
            15_000L / (approachPath.size - 1)
        } else {
            500L
        }

        animateAlongRoute(approachPath, stepDelay, followCamera = true)


        statusJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.tvEta.text = "Driver Assigned\nETA: 3 min"
            delay(5000)

            if (rideState > -1) return@launch
            binding.tvEta.text = "Driver Arriving\nETA: 2 min"
            delay(5000)

            if (rideState > -1) return@launch
            binding.tvEta.text = "Driver Nearby\nETA: 1 min"
            delay(5000)

            if (rideState > -1) return@launch
            binding.tvEta.text = "Driver Arrived"

            movementJob?.cancel()
            updateDriverPosition(pickup, followCamera = true)

            binding.btnCompleteRide.isEnabled = true
            binding.btnCompleteRide.text = "Start Ride"
            rideState = 0
        }
    }

    private fun startRideSimulation() {
        statusJob?.cancel()
        movementJob?.cancel()

        // Requirement: After Start Ride, clear previous route and show ONLY Pickup->Destination.
        binding.tvEta.text = "Calculating route..."

        val pickup = driverPickupLatLng()
        val destination = driverDestinationLatLng()

        val start = pickup
        val coords = "${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}"

        // Clear any pre-start route immediately.
        mapLibreMap?.clear()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.osrmApi.getRoute(coords)
                if (response.isSuccessful && response.body() != null) {
                    val routeResponse = response.body()!!
                    if (routeResponse.code == "Ok" && routeResponse.routes.isNotEmpty()) {
                        val decodedPoints = PolylineDecoder.decode(routeResponse.routes[0].geometry)
                        pickupToDestinationPoints = decodedPoints
                        // Initialize active route for progress visualization (Pickup -> Destination).
                        activeRoutePoints = decodedPoints
                        activeRouteStartIndex = 0
                        activeRoutePolylineOutlineIds = null
                        activeRoutePolylinePrimaryIds = null

                        drawPostStartRouteOnMap(pickupToDestinationPoints)
                        drawRouteProgress(0)



                        animateAlongRoute(
                            path = pickupToDestinationPoints,
                            delayMs = 120L,
                            followCamera = true,
                            onComplete = { finishRide() }
                        )
                        return@launch
                    }
                }

                // Straight fallback also uses pickup->destination polyline points.
                pickupToDestinationPoints = listOf(pickup, destination)

                // Initialize active route for progress visualization (Pickup -> Destination).
                activeRoutePoints = pickupToDestinationPoints
                activeRouteStartIndex = 0
                activeRoutePolylineOutlineIds = null
                activeRoutePolylinePrimaryIds = null

                drawPostStartStraightRouteOnMap(pickup, destination)
                drawRouteProgress(0)

                animateAlongRoute(


                    path = pickupToDestinationPoints,
                    delayMs = 120L,
                    followCamera = true,
                    onComplete = { finishRide() }
                )
            } catch (_: Exception) {
                pickupToDestinationPoints = listOf(pickup, destination)

                // Initialize active route for progress visualization (Pickup -> Destination).
                activeRoutePoints = pickupToDestinationPoints
                activeRouteStartIndex = 0
                activeRoutePolylineOutlineIds = null
                activeRoutePolylinePrimaryIds = null

                drawPostStartStraightRouteOnMap(pickup, destination)
                drawRouteProgress(0)
                animateAlongRoute(


                    path = pickupToDestinationPoints,

                    delayMs = 120L,
                    followCamera = true,
                    onComplete = { finishRide() }
                )
            }
        }
    }



    private fun finishRide() {
        rideState = 2
        binding.tvEta.text = "Destination Reached"
        binding.btnCompleteRide.text = "Finish Ride"
        binding.btnCompleteRide.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        movementJob?.cancel()
        statusJob?.cancel()
        mapView?.onDestroy()
        mapView = null
        mapLibreMap = null
        carIcon = null
        super.onDestroyView()
    }
}
