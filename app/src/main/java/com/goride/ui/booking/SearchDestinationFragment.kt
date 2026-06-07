package com.goride.ui.booking

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.models.NominatimPlace
import com.goride.databinding.FragmentSearchDestinationBinding
import com.goride.ui.home.HomeViewModel
import com.goride.ui.home.RecentLocationAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SearchDestinationFragment : BaseFragment<FragmentSearchDestinationBinding>() {
    private var searchResults = mutableListOf<NominatimPlace>()

    private val viewModel: HomeViewModel by viewModels()

    private var selectedLat: String? = null
    private var selectedLon: String? = null
    private var selectedName: String? = null
    private var selectedFullAddress: String? = null
    private var isSelectingFromList = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var searchJob: Job? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> {
                binding.etPickup.setText("Current Location")
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSearchDestinationBinding {
        return FragmentSearchDestinationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkLocationPermission()
    }

    override fun setupUI() {
        binding.btnContinue.isEnabled = false
        binding.etPickup.setText("Fetching location...")
        setupRecyclerView()
        setupListeners()
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
                    reverseGeocodeLocation(location.latitude, location.longitude)
                } else {
                    binding.etPickup.setText("Current Location")
                }
            }.addOnFailureListener {
                binding.etPickup.setText("Current Location")
            }
        } catch (e: SecurityException) {
            binding.etPickup.setText("Current Location")
        }
    }

    private fun reverseGeocodeLocation(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0].getAddressLine(0)
                binding.etPickup.setText(address)
            } else {
                binding.etPickup.setText("$lat, $lon")
            }
        } catch (e: Exception) {
            binding.etPickup.setText("$lat, $lon")
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        viewModel.recentLocations.observe(viewLifecycleOwner) { locations ->
            binding.rvRecentPlaces.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentPlaces.adapter = RecentLocationAdapter(locations) { location ->
                val action = SearchDestinationFragmentDirections
                    .actionSearchDestinationFragmentToLocationConfirmationFragment(
                        location.name,
                        location.latitude.toString(),
                        location.longitude.toString(),
                        location.address
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun showRecentPlaces() {
        binding.cardSearchResults.visibility = View.GONE
        binding.tvRecentHeader.visibility = View.VISIBLE
        binding.btnClearAll.visibility = View.VISIBLE
        binding.rvRecentPlaces.visibility = View.VISIBLE
    }

    private fun showSearchResults() {
        binding.cardSearchResults.visibility = View.VISIBLE
        binding.tvRecentHeader.visibility = View.GONE
        binding.btnClearAll.visibility = View.GONE
        binding.rvRecentPlaces.visibility = View.GONE
    }

    private fun searchPlaces(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300)
            try {
                val results = RetrofitClient.nominatimApi.searchPlaces(query)
                searchResults.clear()
                searchResults.addAll(results)

                binding.rvSearchResults.adapter = PlaceSearchAdapter(searchResults) { place ->
                    Log.d("PLACE_CLICK", place.displayName)
                    val parts = place.displayName.split(",", limit = 2)
                    selectedName = parts.getOrNull(0)?.trim() ?: place.displayName
                    selectedFullAddress = parts.getOrNull(1)?.trim() ?: ""
                    selectedLat = place.lat
                    selectedLon = place.lon

                    isSelectingFromList = true
                    binding.etDestination.setText(place.displayName)
                    isSelectingFromList = false

                    binding.etDestination.clearFocus()

                    Log.d("CONTINUE_ENABLED", "true")
                    binding.btnContinue.isEnabled = true

                    Log.d("DEST_SELECTED", "$selectedName $selectedLat $selectedLon")
                    binding.cardSearchResults.visibility = View.GONE

                    viewModel.addRecentLocation(
                        com.goride.data.models.LocationModel(
                            name = selectedName!!,
                            address = selectedFullAddress!!,
                            latitude = selectedLat?.toDoubleOrNull() ?: 0.0,
                            longitude = selectedLon?.toDoubleOrNull() ?: 0.0
                        )
                    )
                }

                if (searchResults.isNotEmpty()) {
                    showSearchResults()
                } else {
                    binding.cardSearchResults.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("SEARCH_ERROR", e.stackTraceToString())
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnClearAll.setOnClickListener {
            viewModel.clearRecentLocations()
        }

        binding.etDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isSelectingFromList) return

                binding.btnContinue.isEnabled = false
                selectedLat = null
                selectedLon = null
                selectedName = null
                selectedFullAddress = null

                if (s.isNullOrEmpty()) {
                    showRecentPlaces()
                } else if (s.toString().length >= 3) {
                    searchPlaces(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnContinue.setOnClickListener {
            val name = selectedName ?: return@setOnClickListener
            val lat = selectedLat ?: return@setOnClickListener
            val lon = selectedLon ?: return@setOnClickListener
            val address = selectedFullAddress ?: return@setOnClickListener

            val action = SearchDestinationFragmentDirections
                .actionSearchDestinationFragmentToLocationConfirmationFragment(name, lat, lon, address)
            findNavController().navigate(action)
        }

        binding.etDestination.setOnEditorActionListener { _, _, _ ->
            if (selectedName != null && selectedLat != null && selectedLon != null && selectedFullAddress != null) {
                val action = SearchDestinationFragmentDirections
                    .actionSearchDestinationFragmentToLocationConfirmationFragment(
                        selectedName!!, selectedLat!!, selectedLon!!, selectedFullAddress!!
                    )
                findNavController().navigate(action)
            }
            true
        }
    }
}