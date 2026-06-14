package com.goride.ui.booking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.goride.data.models.DriverPool
import com.goride.databinding.FragmentDriverSelectionBinding

class DriverSelectionFragment : Fragment() {

    private var _binding: FragmentDriverSelectionBinding? = null
    private val binding get() = _binding!!

    private val args: DriverSelectionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        handleBackPress()
        startSearching()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun startSearching() {
        binding.llSearching.visibility = View.VISIBLE
        binding.rvDrivers.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                showDrivers()
            }
        }, 1500)
    }

    private fun showDrivers() {
        binding.llSearching.visibility = View.GONE
        binding.rvDrivers.visibility = View.VISIBLE

        val driverList = DriverPool.getDrivers(args.vehicleType, args.fare)
        
        binding.rvDrivers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = DriverAdapter(driverList) { selectedDriver ->
                val action = DriverSelectionFragmentDirections
                    .actionDriverSelectionFragmentToBookingSuccessFragment(
                        rideId = args.rideId,
                        status = "CONFIRMED",
                        vehicleType = args.vehicleType,
                        fare = selectedDriver.fare,
                        distance = args.distance,
                        duration = args.duration,
                        driverName = selectedDriver.name,
                        driverVehicle = selectedDriver.vehicle,
                        driverPlate = selectedDriver.plateNumber,
                        pickupLat = args.pickupLat,
                        pickupLng = args.pickupLng,
                        destinationLat = args.destinationLat,
                        destinationLng = args.destinationLng
                    )
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
