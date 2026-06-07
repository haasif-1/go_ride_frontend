package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentBookingSuccessBinding
import java.util.Locale

class BookingSuccessFragment : BaseFragment<FragmentBookingSuccessBinding>() {

    private val args: BookingSuccessFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBookingSuccessBinding =
        FragmentBookingSuccessBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // Pop-in animation on the success icon
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.ivSuccess.startAnimation(animation)

        // ── Ride summary ────────────────────────────────────────────────────────
        binding.tvRideId.text      = "Ride #${args.rideId.take(8).uppercase(Locale.ROOT)}"
        binding.tvStatus.text      = args.status
        binding.tvVehicleType.text = args.vehicleType
        binding.tvFare.text        = "Rs. %.0f".format(Locale.US, args.fare.toDouble())
        binding.tvDistance.text    = "%.1f km".format(Locale.US, args.distance.toDouble())
        binding.tvDuration.text    = "${args.duration} min"

        binding.btnBackHome.setOnClickListener {
            findNavController().navigate(R.id.action_bookingSuccessFragment_to_homeFragment)
        }
    }
}