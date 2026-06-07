package com.goride.ui.booking

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentLocationConfirmationBinding
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import android.graphics.Color
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import java.util.Locale

class LocationConfirmationFragment : BaseFragment<FragmentLocationConfirmationBinding>(), OnMapReadyCallback {

    private val args: LocationConfirmationFragmentArgs by navArgs()
    private var mapLibreMap: MapLibreMap? = null
    private var mapView: MapView? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> {
                handleLocationUnavailable()
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationConfirmationBinding {
        return FragmentLocationConfirmationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView = binding.root.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        setupDestinationData()
        checkLocationPermission()
    }

    override fun setupUI() {
        setupListeners()
    }

    private fun setupDestinationData() {
        binding.tvDestLabel.text = args.destinationName
        binding.tvDestAddress.text = args.destinationFullAddress
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun fetchCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    updatePickupUI(location.latitude, location.longitude)
                    updateMapCamera()
                } else {
                    handleLocationUnavailable()
                }
            }.addOnFailureListener {
                handleLocationUnavailable()
            }
        } catch (e: SecurityException) {
            handleLocationUnavailable()
        }
    }

    private fun handleLocationUnavailable() {
        binding.tvPickupLabel.text = "Current Location"
        binding.tvPickupAddress.text = "Location unavailable"
        updateMapCamera()
    }

    private fun updatePickupUI(lat: Double, lon: Double) {
        binding.tvPickupLabel.text = "Current Location"
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                binding.tvPickupAddress.text = addresses[0].getAddressLine(0)
            } else {
                binding.tvPickupAddress.text = "$lat, $lon"
            }
        } catch (e: Exception) {
            binding.tvPickupAddress.text = "$lat, $lon"
        }

        // Calculate and display distance
        val destLat = args.destinationLat.toDoubleOrNull()
        val destLon = args.destinationLon.toDoubleOrNull()

        if (destLat != null && destLon != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lat, lon, destLat, destLon, results)
            val distanceInMeters = results[0]

            val distanceText = if (distanceInMeters < 1000) {
                "${distanceInMeters.toInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000f)
            }

            binding.tvDistance.text = distanceText
            binding.tvDistance.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.action_locationConfirmationFragment_to_vehicleSelectionFragment)
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
            updateMapCamera()
        }
    }

    private fun updateMapCamera() {
        val map = mapLibreMap ?: return

        // Remove old markers before adding new ones
        map.clear()

        val destLat = args.destinationLat.toDoubleOrNull() ?: 0.0
        val destLon = args.destinationLon.toDoubleOrNull() ?: 0.0
        val destination = LatLng(destLat, destLon)

        map.addMarker(MarkerOptions().position(destination).title("Destination"))

        if (currentLocation != null) {
            map.addMarker(MarkerOptions().position(currentLocation!!).title("Pickup"))

            map.addPolyline(
                PolylineOptions()
                    .add(currentLocation)
                    .add(destination)
                    .color(Color.BLUE)
                    .width(4f)
            )

            val bounds = LatLngBounds.Builder()
                .include(currentLocation!!)
                .include(destination)
                .build()

            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 150)
            )
        } else {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(destination, 14.0)
            )
        }
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
        mapView?.onDestroy()
        mapView = null
        mapLibreMap = null
        super.onDestroyView()
    }
}