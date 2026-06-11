package com.goride.ui.booking

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.models.RideData
import com.goride.data.repository.BookingRepository
import com.goride.databinding.FragmentRideTrackingBinding

class RideTrackingFragment : BaseFragment<FragmentRideTrackingBinding>() {

    private val args: RideTrackingFragmentArgs by navArgs()

    private val viewModel: RideTrackingViewModel by viewModels {
        BookingViewModelFactory(BookingRepository(RetrofitClient.apiService))
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRideTrackingBinding =
        FragmentRideTrackingBinding.inflate(inflater, container, false)

    override fun setupUI() {
        observeViewModel()
        viewModel.startTracking()
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.activeRide.observe(viewLifecycleOwner) { result ->
            result.onSuccess { ride ->
                showRideDetails(ride)
                binding.tvError.visibility = View.GONE
            }.onFailure { error ->
                binding.tvError.text = error.message ?: "Unable to fetch ride details"
                binding.tvError.visibility = View.VISIBLE
                // Keep button visible so the user can exit on repeated failures
                binding.btnBackHome.visibility = View.VISIBLE
            }
        }
    }

    // ── UI Binding ────────────────────────────────────────────────────────────

    private fun showRideDetails(ride: RideData) {
        // ── Core fields ──────────────────────────────────────────────────────
        binding.tvRideId.text    = ride.displayId
        binding.tvVehicleType.text = ride.vehicleType.lowercase().replaceFirstChar { it.uppercase() }
        binding.tvFare.text      = "Rs. %.2f".format(ride.fare)
        binding.tvDistance.text  = "%.2f km".format(ride.distance)
        binding.tvDuration.text  = "${ride.duration} min"

        // ── Status badge ─────────────────────────────────────────────────────
        val status = ride.status.uppercase()
        applyStatusBadge(status)

        // ── Driver card (only when a driver is assigned) ──────────────────
        val hasDriver = !ride.driverName.isNullOrBlank()
        binding.cardDriverInfo.visibility = if (hasDriver) View.VISIBLE else View.GONE
        if (hasDriver) {
            binding.tvDriverName.text   = ride.driverName
            binding.tvVehiclePlate.text = ride.vehiclePlate ?: "--"
        }

        // ── Terminal status: show "Back to Home" and hide loading ─────────
        if (status == "COMPLETED" || status == "CANCELLED") {
            binding.loadingLayout.visibility = View.GONE
            binding.btnBackHome.visibility   = View.VISIBLE
            binding.btnBackHome.setOnClickListener {
                findNavController().navigate(
                    R.id.action_rideTrackingFragment_to_homeFragment
                )
            }
        }
    }

    // ── Status badge colour helper ────────────────────────────────────────────

    private fun applyStatusBadge(status: String) {
        val color = when (status) {
            "REQUESTED"   -> Color.parseColor("#FF9800") // Orange
            "ACCEPTED"    -> Color.parseColor("#2196F3") // Blue
            "ARRIVING"    -> Color.parseColor("#9C27B0") // Purple
            "IN_PROGRESS" -> Color.parseColor("#4CAF50") // Green
            "COMPLETED"   -> Color.parseColor("#1B5E20") // Dark Green
            "CANCELLED"   -> Color.parseColor("#F44336") // Red
            else           -> Color.parseColor("#9E9E9E") // Grey fallback
        }

        val label = status.replace("_", " ")
        binding.tvStatusBadge.text = label

        // Mutate the drawable so each update is independent
        val bg = (binding.tvStatusBadge.background.mutate() as? GradientDrawable)
        bg?.setColor(color)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        viewModel.stopTracking()
        super.onDestroyView()
    }
}
