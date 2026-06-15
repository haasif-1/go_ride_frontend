package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.repository.BookingHistoryManager
import com.goride.databinding.FragmentBookingsBinding

/**
 * Displays booking history loaded from local SharedPreferences storage.
 * No network calls are made — data is written by [BookingSuccessFragment]
 * each time a user completes a ride booking.
 */
class BookingsFragment : BaseFragment<FragmentBookingsBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBookingsBinding =
        FragmentBookingsBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // Active-ride card is driven by API — hide it for local-only mode
        binding.cardActiveRide.visibility = View.GONE

        loadBookingHistory()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list every time the screen is re-entered so a newly
        // completed booking appears immediately without restarting the app.
        loadBookingHistory()
    }

    // ────────────────────────────────────────────────────────────────────────────

    private fun loadBookingHistory() {
        val bookings = BookingHistoryManager(requireContext()).getBookings()

        if (bookings.isEmpty()) {
            showEmptyState()
        } else {
            showBookings(bookings)
        }
    }

    private fun showEmptyState() {
        binding.scrollView.visibility                           = View.GONE
        binding.root.findViewById<View>(R.id.emptyLayout)?.visibility  = View.VISIBLE
        binding.root.findViewById<View>(R.id.errorLayout)?.visibility  = View.GONE
        binding.root.findViewById<View>(R.id.loadingLayout)?.visibility = View.GONE
    }

    private fun showBookings(bookings: List<com.goride.data.models.LocalBookingRecord>) {
        binding.root.findViewById<View>(R.id.emptyLayout)?.visibility  = View.GONE
        binding.root.findViewById<View>(R.id.errorLayout)?.visibility  = View.GONE
        binding.root.findViewById<View>(R.id.loadingLayout)?.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE

        binding.rvRideHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRideHistory.adapter       = LocalBookingAdapter(bookings)
    }
}