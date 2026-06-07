package com.goride.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment<FragmentHomeBinding>(), OnMapReadyCallback {

    private val viewModel: HomeViewModel by viewModels()
    private var mapLibreMap: MapLibreMap? = null
    private var mapView: MapView? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.root.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun setupUI() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        viewModel.recentLocations.observe(viewLifecycleOwner) { locations ->
            binding.rvRecentLocations.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentLocations.adapter = RecentLocationAdapter(locations) {
                findNavController().navigate(R.id.action_homeFragment_to_searchDestinationFragment)
            }
        }
    }

    private fun setupListeners() {
        binding.searchBar.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchDestinationFragment)
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        val styleUrl = "https://tiles.openfreemap.org/styles/liberty"
        map.setStyle(styleUrl) { style ->
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationComponent = map.locationComponent
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(requireContext(), style).build()
                )
                locationComponent.isLocationComponentEnabled = true

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0))
                    }
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            }
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