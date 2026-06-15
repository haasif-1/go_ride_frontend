package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.models.LocalBookingRecord
import com.goride.data.repository.BookingHistoryManager
import com.goride.databinding.FragmentBookingSuccessBinding
import java.text.SimpleDateFormat
import java.util.Date
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

        // ── Driver Information ──────────────────────────────────────────────────
        binding.tvDriverName.text    = args.driverName
        binding.tvDriverVehicle.text = args.driverVehicle
        binding.tvDriverPlate.text   = args.driverPlate

        // ── Ride summary ────────────────────────────────────────────────────────
        binding.tvRideId.text      = "Ride #${args.rideId.take(8).uppercase(Locale.ROOT)}"
        binding.tvStatus.text      = args.status
        binding.tvVehicleType.text = args.vehicleType
        binding.tvFare.text        = "Rs. %.0f".format(Locale.US, args.fare.toDouble())
        binding.tvDistance.text    = "%.1f km".format(Locale.US, args.distance.toDouble())
        binding.tvDuration.text    = "${args.duration} min"

        // ── Save booking locally ────────────────────────────────────────────────
        saveBookingLocally()

        binding.btnTrackDriver.setOnClickListener {
            val action = BookingSuccessFragmentDirections
                .actionBookingSuccessFragmentToDriverTrackingFragment(
                    rideId        = args.rideId,
                    vehicleType   = args.vehicleType,
                    fare          = args.fare,
                    distance      = args.distance,
                    duration      = args.duration,
                    driverName    = args.driverName,
                    driverVehicle = args.driverVehicle,
                    driverPlate   = args.driverPlate,
                    pickupLat     = args.pickupLat,
                    pickupLng     = args.pickupLng,
                    destinationLat = args.destinationLat,
                    destinationLng = args.destinationLng
                )
            findNavController().navigate(action)
        }

        binding.btnCancelRide.setOnClickListener {
            findNavController().navigate(
                BookingSuccessFragmentDirections.actionBookingSuccessFragmentToHomeFragment()
            )
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private fun saveBookingLocally() {
        val dateTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date())

        // Build human-readable location strings from coordinate args.
        // If the backend later provides address strings they can replace these.
        val pickup = formatCoords(args.pickupLat, args.pickupLng)
        val dest   = formatCoords(args.destinationLat, args.destinationLng)

        val record = LocalBookingRecord(
            rideId             = args.rideId,
            pickupAddress      = pickup,
            destinationAddress = dest,
            vehicleType        = args.vehicleType,
            fare               = args.fare,
            driverName         = args.driverName,
            driverVehicle      = args.driverVehicle,
            driverPlate        = args.driverPlate,
            status             = args.status,
            bookedAt           = dateTime
        )

        BookingHistoryManager(requireContext()).saveBooking(record)
    }

    private fun formatCoords(lat: String, lng: String): String {
        val latD = lat.toDoubleOrNull()
        val lngD = lng.toDoubleOrNull()
        return if (latD != null && lngD != null) {
            "%.4f, %.4f".format(Locale.US, latD, lngD)
        } else {
            "$lat, $lng"
        }
    }
}
