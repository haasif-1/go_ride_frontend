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

    private var rideState = -1 // -1: waiting, 0: arrived/ready, 1: riding, 2: completed
    private var movementJob: Job? = null
    private var statusJob: Job? = null
    private var driverMarker: Marker? = null

    private var driverStartLocation: LatLng? = null
    private var spawnToPickupPoints: List<LatLng> = emptyList()
    private var pickupToDestinationPoints: List<LatLng> = emptyList()

    private var activeRoutePoints: List<LatLng> = emptyList()
    private var completedRoutePolylineOutlineIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var completedRoutePolylinePrimaryIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var remainingRoutePolylineOutlineIds: MutableList<org.maplibre.android.annotations.Polyline>? = null
    private var remainingRoutePolylinePrimaryIds: MutableList<org.maplibre.android.annotations.Polyline>? = null

    private val routeLineWidth: Float = 4f
    private val routeOutlineWidth: Float = 6f

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

        // ── Navigation Buttons ──────────────────────────────────────────────────
        
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCancelRide.setOnClickListener {
            movementJob?.cancel()
            statusJob?.cancel()
            // Navigate to Home and clear back stack as defined in nav_graph.xml action
            findNavController().navigate(
                DriverTrackingFragmentDirections.actionDriverTrackingFragmentToHomeFragment()
            )
        }

        binding.btnCompleteRide.setOnClickListener {
            when (rideState) {
                0 -> {
                    rideState = 1
                    binding.btnCompleteRide.isEnabled = false
                    binding.tvEta.text = "Ride In Progress"
                    binding.btnCompleteRide.text = "Finish Ride"
                    binding.btnCancelRide.visibility = View.GONE
                    startRideSimulation()
                }
                2 -> showRatingBottomSheet()
            }
        }
    }

    private fun showRatingBottomSheet() {
        val bottomSheet = RateDriverBottomSheet.newInstance(args.driverName, args.driverVehicle)
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
            val pickup = LatLng(args.pickupLat.toDoubleOrNull() ?: 0.0, args.pickupLng.toDoubleOrNull() ?: 0.0)
            val destination = LatLng(args.destinationLat.toDoubleOrNull() ?: 0.0, args.destinationLng.toDoubleOrNull() ?: 0.0)
            fetchRoute(pickup, destination)
        }
    }

    private fun fetchRoute(pickup: LatLng, destination: LatLng) {
        driverStartLocation = generateRandomNearbyLocation(pickup)
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
                            drawPreStartRouteOnMap(decodedPoints, pickup)
                            return@launch
                        }
                    }
                    drawPreStartRouteOnMap(listOf(start, pickup), pickup)
                } catch (_: Exception) {
                    drawPreStartRouteOnMap(listOf(start, pickup), pickup)
                }
            }
        }
    }

    private fun drawPreStartRouteOnMap(points: List<LatLng>, pickup: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        addPickupMarker(pickup)
        activeRoutePoints = points
        drawRouteProgress(0)
        driverMarker = addDriverMarker(points.firstOrNull() ?: pickup)
        fitCameraToRoute(points)
        startDriverApproachSimulation(points, pickup)
    }

    private fun addDriverMarker(position: LatLng): Marker? {
        val options = MarkerOptions().position(position).title("Driver")
        carIcon?.let { options.icon(it) }
        return mapLibreMap?.addMarker(options)
    }

    private fun addPickupMarker(pickup: LatLng) {
        mapLibreMap?.addMarker(MarkerOptions().position(pickup).title("Pickup"))
    }

    private fun addPickupAndDestinationMarkers(pickup: LatLng, destination: LatLng) {
        mapLibreMap?.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        mapLibreMap?.addMarker(MarkerOptions().position(destination).title("Destination"))
    }

    private fun drawRouteProgress(currentIndex: Int) {
        val map = mapLibreMap ?: return
        if (activeRoutePoints.isEmpty()) return

        val safeIndex = currentIndex.coerceIn(0, activeRoutePoints.lastIndex)
        val completed = activeRoutePoints.subList(0, safeIndex + 1)
        val remaining = activeRoutePoints.subList(safeIndex, activeRoutePoints.size)

        val greyColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.route_outline)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.route_primary)

        // Add new polylines first to reduce flicker
        val newCO = if (completed.size >= 2) map.addPolyline(PolylineOptions().addAll(completed).color(greyColor).width(routeOutlineWidth)) else null
        val newCP = if (completed.size >= 2) map.addPolyline(PolylineOptions().addAll(completed).color(greyColor).width(routeLineWidth)) else null
        val newRO = if (remaining.size >= 2) map.addPolyline(PolylineOptions().addAll(remaining).color(outlineColor).width(routeOutlineWidth)) else null
        val newRP = if (remaining.size >= 2) map.addPolyline(PolylineOptions().addAll(remaining).color(primaryColor).width(routeLineWidth)) else null

        // Remove old ones
        completedRoutePolylineOutlineIds?.forEach { map.removePolyline(it) }
        completedRoutePolylinePrimaryIds?.forEach { map.removePolyline(it) }
        remainingRoutePolylineOutlineIds?.forEach { map.removePolyline(it) }
        remainingRoutePolylinePrimaryIds?.forEach { map.removePolyline(it) }

        // Update references
        completedRoutePolylineOutlineIds = newCO?.let { mutableListOf(it) }
        completedRoutePolylinePrimaryIds = newCP?.let { mutableListOf(it) }
        remainingRoutePolylineOutlineIds = newRO?.let { mutableListOf(it) }
        remainingRoutePolylinePrimaryIds = newRP?.let { mutableListOf(it) }
    }

    private fun animateAlongRoute(
        path: List<LatLng>,
        totalDurationMs: Long,
        followCamera: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        movementJob?.cancel()
        movementJob = viewLifecycleOwner.lifecycleScope.launch {
            if (path.isEmpty()) return@launch
            val fps = 20
            val totalFrames = (totalDurationMs / (1000 / fps)).toInt().coerceAtLeast(1)
            val frameDelay = totalDurationMs / totalFrames

            for (frame in 0..totalFrames) {
                val progress = frame.toDouble() / totalFrames
                val currentLatLng = interpolateLatLng(path, progress)
                val pathIndex = (progress * (path.size - 1)).toInt()

                // Throttle route redraw to every 5 frames or the last frame to reduce blinking
                if (frame % 5 == 0 || frame == totalFrames) {
                    drawRouteProgress(pathIndex)
                }
                updateDriverPosition(currentLatLng, followCamera)
                delay(frameDelay)
            }
            onComplete?.invoke()
        }
    }

    private fun interpolateLatLng(path: List<LatLng>, progress: Double): LatLng {
        if (path.isEmpty()) return LatLng(0.0, 0.0)
        if (path.size == 1 || progress <= 0.0) return path.first()
        if (progress >= 1.0) return path.last()

        val segmentFloat = progress * (path.size - 1)
        val index = segmentFloat.toInt().coerceAtMost(path.size - 2)
        val segmentProgress = segmentFloat - index

        val start = path[index]
        val end = path[index + 1]

        val lat = start.latitude + (end.latitude - start.latitude) * segmentProgress
        val lng = start.longitude + (end.longitude - start.longitude) * segmentProgress
        return LatLng(lat, lng)
    }

    private fun updateDriverPosition(point: LatLng, followCamera: Boolean) {
        driverMarker?.let { marker ->
            marker.position = point
            mapLibreMap?.updateMarker(marker)
        }
        if (followCamera) {
            mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.0))
        }
    }

    private fun startDriverApproachSimulation(approachPath: List<LatLng>, pickup: LatLng) {
        statusJob?.cancel()
        animateAlongRoute(
            path = approachPath,
            totalDurationMs = 25000L,
            followCamera = true,
            onComplete = {
                binding.tvEta.text = "Driver Arrived"
                binding.btnCompleteRide.isEnabled = true
                binding.btnCompleteRide.text = "Start Ride"
                rideState = 0
            }
        )

        statusJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.tvEta.text = "Driver Assigned\nETA: 3 min"
            delay(8000)
            if (rideState > -1) return@launch
            binding.tvEta.text = "Driver Arriving\nETA: 2 min"
            delay(8000)
            if (rideState > -1) return@launch
            binding.tvEta.text = "Driver Nearby\nETA: 1 min"
        }
    }

    private fun startRideSimulation() {
        statusJob?.cancel()
        val pickup = LatLng(args.pickupLat.toDoubleOrNull() ?: 0.0, args.pickupLng.toDoubleOrNull() ?: 0.0)
        val destination = LatLng(args.destinationLat.toDoubleOrNull() ?: 0.0, args.destinationLng.toDoubleOrNull() ?: 0.0)
        val coords = "${pickup.longitude},${pickup.latitude};${destination.longitude},${destination.latitude}"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.osrmApi.getRoute(coords)
                if (response.isSuccessful && response.body() != null) {
                    val routeResponse = response.body()!!
                    if (routeResponse.code == "Ok" && routeResponse.routes.isNotEmpty()) {
                        val decodedPoints = PolylineDecoder.decode(routeResponse.routes[0].geometry)
                        pickupToDestinationPoints = decodedPoints
                        beginRideAnimation(decodedPoints, pickup, destination)
                        return@launch
                    }
                }
                beginRideAnimation(listOf(pickup, destination), pickup, destination)
            } catch (_: Exception) {
                beginRideAnimation(listOf(pickup, destination), pickup, destination)
            }
        }
    }

    private fun beginRideAnimation(points: List<LatLng>, pickup: LatLng, destination: LatLng) {
        mapLibreMap?.clear()
        addPickupAndDestinationMarkers(pickup, destination)
        activeRoutePoints = points
        drawRouteProgress(0)
        driverMarker = addDriverMarker(points.first())
        fitCameraToRoute(points)
        animateAlongRoute(
            path = points,
            totalDurationMs = 45000L,
            followCamera = true,
            onComplete = { finishRide() }
        )
    }

    private fun finishRide() {
        rideState = 2
        binding.tvEta.text = "Destination Reached"
        binding.btnCompleteRide.text = "Finish Ride"
        binding.btnCompleteRide.isEnabled = true
    }

    private fun fitCameraToRoute(points: List<LatLng>) {
        if (points.isEmpty()) return
        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(it) }
        try {
            val bounds = builder.build()
            mapView?.post { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)) }
        } catch (_: Exception) {}
    }

    private fun generateRandomNearbyLocation(center: LatLng): LatLng {
        val minR = 300.0 / 111320.0
        val maxR = 600.0 / 111320.0
        val angle = Random.nextDouble() * 2.0 * Math.PI
        val r = sqrt(Random.nextDouble() * (maxR * maxR - minR * minR) + minR * minR)
        return LatLng(center.latitude + r * cos(angle), center.longitude + (r * sin(angle)) / cos(center.latitude * Math.PI / 180.0))
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onStop() { super.onStop(); mapView?.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView?.onSaveInstanceState(outState) }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
    override fun onDestroyView() {
        movementJob?.cancel(); statusJob?.cancel()
        mapView?.onDestroy(); mapView = null; mapLibreMap = null; carIcon = null
        super.onDestroyView()
    }
}
