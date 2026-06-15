package com.goride.ui.booking

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.models.VehicleModel
import com.goride.data.repository.BookingRepository
import com.goride.databinding.FragmentVehicleSelectionBinding
import com.goride.utils.PolylineDecoder
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import java.util.Locale

class VehicleSelectionFragment : BaseFragment<FragmentVehicleSelectionBinding>(), OnMapReadyCallback {

    private val args: VehicleSelectionFragmentArgs by navArgs()
    private var mapLibreMap: MapLibreMap? = null
    private var mapView: MapView? = null
    private var selectedVehicle: VehicleModel? = null

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(BookingRepository(RetrofitClient.apiService))
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVehicleSelectionBinding =
        FragmentVehicleSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.root.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        binding.tvDestinationName.text = args.destinationName
    }

    private fun setupRecyclerView() {
        val initialVehicles = listOf(
            VehicleModel(1, "Normal",  "Rs. 100", "-- min", "4 Seats", R.drawable.ic_car_placeholder, isPopular = true),
            VehicleModel(2, "Economy", "Rs. 150", "-- min", "5 Seats", R.drawable.ic_car_placeholder),
            VehicleModel(3, "Comfort", "Rs. 250", "-- min", "6 Seats", R.drawable.ic_car_placeholder)
        )
        selectedVehicle = initialVehicles[0]

        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = VehicleAdapter(initialVehicles) { vehicle ->
            selectedVehicle = vehicle
            binding.tvFare.text = vehicle.price
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSelectRide.setOnClickListener {
            val rawPickupLat = args.pickupLat.toDoubleOrNull() ?: 0.0
            val rawPickupLng = args.pickupLng.toDoubleOrNull() ?: 0.0
            val rawDestLat   = args.destinationLat.toDoubleOrNull() ?: 0.0
            val rawDestLng   = args.destinationLng.toDoubleOrNull() ?: 0.0

            val pickupLat = roundCoordinate(rawPickupLat)
            val pickupLng = roundCoordinate(rawPickupLng)
            val destLat   = roundCoordinate(rawDestLat)
            val destLng   = roundCoordinate(rawDestLng)

            val vehicleType = selectedVehicle?.name?.uppercase(Locale.ROOT) ?: "NORMAL"

            Log.d("VehicleSelection", "Original Pickup: $rawPickupLat, $rawPickupLng")
            Log.d("VehicleSelection", "Rounded Pickup: $pickupLat, $pickupLng")
            Log.d("VehicleSelection", "Original Dest: $rawDestLat, $rawDestLng")
            Log.d("VehicleSelection", "Rounded Dest: $destLat, $destLng")
            Log.d("VehicleSelection", "vehicleType: $vehicleType")

            binding.btnSelectRide.isEnabled = false
            binding.btnSelectRide.text = "Requesting…"

            viewModel.requestRide(
                pickupLat   = pickupLat,
                pickupLng   = pickupLng,
                destLat     = destLat,
                destLng     = destLng,
                vehicleType = vehicleType
            )
        }
    }

    private fun roundCoordinate(value: Double): Double {
        return try {
            String.format(Locale.US, "%.6f", value).toDouble()
        } catch (e: Exception) {
            value
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.rideBookingResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { rideData ->
                // Use the fare the user actually sees on the vehicle card,
                // not the raw API fare (which may be a backend base amount
                // unrelated to the distance-adjusted price shown on screen).
                val displayedFare = selectedVehicle?.price
                    ?.replace("Rs.", "", ignoreCase = true)
                    ?.trim()
                    ?.toFloatOrNull()
                    ?: rideData.fare.toFloat()

                val action = VehicleSelectionFragmentDirections
                    .actionVehicleSelectionFragmentToDriverSelectionFragment(
                        rideId      = rideData.id,
                        vehicleType = rideData.vehicleType,
                        fare        = displayedFare,
                        distance    = rideData.distance.toFloat(),
                        duration    = rideData.duration,
                        pickupLat   = args.pickupLat,
                        pickupLng   = args.pickupLng,
                        destinationLat = args.destinationLat,
                        destinationLng = args.destinationLng
                    )
                findNavController().navigate(action)
            }.onFailure { error ->
                binding.btnSelectRide.isEnabled = true
                binding.btnSelectRide.text = "Select Ride"
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
            val pickupLat = args.pickupLat.toDoubleOrNull() ?: 0.0
            val pickupLng = args.pickupLng.toDoubleOrNull() ?: 0.0
            val destLat   = args.destinationLat.toDoubleOrNull() ?: 0.0
            val destLng   = args.destinationLng.toDoubleOrNull() ?: 0.0
            fetchRoute(LatLng(pickupLat, pickupLng), LatLng(destLat, destLng))
        }
    }

    private fun fetchRoute(pickup: LatLng, destination: LatLng) {
        val coords = "${pickup.longitude},${pickup.latitude};${destination.longitude},${destination.latitude}"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.osrmApi.getRoute(coords)
                if (response.isSuccessful && response.body() != null) {
                    val routeResponse = response.body()!!
                    if (routeResponse.code == "Ok" && routeResponse.routes.isNotEmpty()) {
                        val route = routeResponse.routes[0]
                        updateTripDetails(route.distance, route.duration)
                        drawRouteOnMap(PolylineDecoder.decode(route.geometry), pickup, destination)
                        return@launch
                    }
                }
                handleRouteCalculationFallback(pickup, destination)
            } catch (e: Exception) {
                handleRouteCalculationFallback(pickup, destination)
            }
        }
    }

    private fun handleRouteCalculationFallback(pickup: LatLng, destination: LatLng) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            pickup.latitude, pickup.longitude,
            destination.latitude, destination.longitude,
            results
        )
        val distanceMeters = results[0].toDouble()
        val durationSeconds = distanceMeters / 11.1
        updateTripDetails(distanceMeters, durationSeconds)
        drawStraightRoute(pickup, destination)
    }

    private fun updateTripDetails(distanceMeters: Double, durationSeconds: Double) {
        val distanceKm  = distanceMeters / 1000.0
        val durationMin = (durationSeconds / 60.0).toLong().toInt().coerceAtLeast(1)

        binding.tvDistance.text = String.format(Locale.getDefault(), "%.1f km", distanceKm)
        binding.tvDuration.text = String.format(Locale.getDefault(), "%d min", durationMin)

        val updatedVehicles = listOf(
            VehicleModel(1, "Normal",  "Rs. %.0f".format(Locale.US, 100.0 + distanceKm * 30.0), "${durationMin} min",     "4 Seats", R.drawable.ic_car_placeholder, isPopular = true),
            VehicleModel(2, "Economy", "Rs. %.0f".format(Locale.US, 150.0 + distanceKm * 40.0), "${durationMin + 2} min", "5 Seats", R.drawable.ic_car_placeholder),
            VehicleModel(3, "Comfort", "Rs. %.0f".format(Locale.US, 250.0 + distanceKm * 55.0), "${durationMin + 5} min", "6 Seats", R.drawable.ic_car_placeholder)
        )

        selectedVehicle = updatedVehicles[0]
        binding.tvFare.text = selectedVehicle?.price
        (binding.rvVehicles.adapter as? VehicleAdapter)?.updateVehicles(updatedVehicles)
    }

    private fun drawRouteOnMap(points: List<LatLng>, pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        map.addMarker(MarkerOptions().position(destination).title("Destination"))

        if (points.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions().addAll(points).color(Color.BLUE).width(4f)
            )
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
            } catch (e: Exception) {
                drawStraightRoute(pickup, destination)
            }
        } else {
            drawStraightRoute(pickup, destination)
        }
    }

    private fun drawStraightRoute(pickup: LatLng, destination: LatLng) {
        val map = mapLibreMap ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        map.addMarker(MarkerOptions().position(destination).title("Destination"))
        map.addPolyline(
            PolylineOptions().add(pickup).add(destination).color(Color.BLUE).width(4f)
        )
        try {
            val bounds = LatLngBounds.Builder().include(pickup).include(destination).build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        } catch (e: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 14.0))
        }
    }

    override fun onStart()  { super.onStart();  mapView?.onStart()  }
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause()  { super.onPause();  mapView?.onPause()  }
    override fun onStop()   { super.onStop();   mapView?.onStop()   }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        mapView?.onDestroy()
        mapView = null
        mapLibreMap = null
        super.onDestroyView()
    }
}
