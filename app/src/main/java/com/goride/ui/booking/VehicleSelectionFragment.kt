package com.goride.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.models.VehicleModel
import com.goride.data.repository.BookingRepository
import com.goride.databinding.FragmentVehicleSelectionBinding

class VehicleSelectionFragment : BaseFragment<FragmentVehicleSelectionBinding>(), OnMapReadyCallback {

    private var mapLibreMap: MapLibreMap? = null
    private var mapView: MapView? = null
    private var selectedVehicle: VehicleModel? = null

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(BookingRepository(RetrofitClient.apiService))
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentVehicleSelectionBinding {
        return FragmentVehicleSelectionBinding.inflate(inflater, container, false)
    }

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
    }

    private fun setupRecyclerView() {
        val vehicles = listOf(
            VehicleModel(1, "Normal", "$1.99", "2 min", "4 Seats", R.drawable.ic_car_placeholder, isPopular = true),
            VehicleModel(2, "Economy", "$5.99", "2 min", "5 Seats", R.drawable.ic_car_placeholder),
            VehicleModel(3, "Comfort", "$8.00", "2 min", "6 Seats", R.drawable.ic_car_placeholder)
        )
        selectedVehicle = vehicles[0]

        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = VehicleAdapter(vehicles) { vehicle ->
            selectedVehicle = vehicle
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnSelectRide.setOnClickListener {
            // Real API call
            viewModel.requestRide(
                pickupLat = -6.2088, // Mock coordinates for demo
                pickupLng = 106.8456,
                destLat = -6.2146,
                destLng = 106.8451,
                vehicleType = selectedVehicle?.name ?: "Normal"
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.rideBookingResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                findNavController().navigate(R.id.action_vehicleSelectionFragment_to_bookingSuccessFragment)
            }.onFailure {
                Toast.makeText(requireContext(), "Booking failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
            val pickup = LatLng(-6.2088, 106.8456)
            // Use Double (15.0) for zoom instead of Float (15f)
            mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 15.0))
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