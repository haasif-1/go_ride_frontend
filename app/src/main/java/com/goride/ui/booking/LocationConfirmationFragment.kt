package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentLocationConfirmationBinding

class LocationConfirmationFragment : BaseFragment<FragmentLocationConfirmationBinding>(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationConfirmationBinding {
        return FragmentLocationConfirmationBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupMap()
        setupListeners()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.action_locationConfirmationFragment_to_vehicleSelectionFragment)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Mocking pickup and destination markers
        val pickup = LatLng(-6.2088, 106.8456)
        val destination = LatLng(-6.2146, 106.8451)

        googleMap?.addMarker(MarkerOptions().position(pickup).title("Pickup"))
        googleMap?.addMarker(MarkerOptions().position(destination).title("Destination"))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pickup, 14f))
    }
}