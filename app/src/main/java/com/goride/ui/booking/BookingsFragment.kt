package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.repository.RideHistoryRepository
import com.goride.databinding.FragmentBookingsBinding

class BookingsFragment : BaseFragment<FragmentBookingsBinding>() {

    private val viewModel: RideHistoryViewModel by viewModels {
        RideHistoryViewModelFactory(RideHistoryRepository(RetrofitClient.apiService))
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBookingsBinding {
        return FragmentBookingsBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.fetchRideData()
    }

    private fun setupRecyclerView() {
        binding.rvRideHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.root.findViewById<View>(R.id.btnRetry)?.setOnClickListener {
            viewModel.fetchRideData()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.activeRide.observe(viewLifecycleOwner) { result ->
            result.onSuccess { ride ->
                if (ride != null) {
                    binding.cardActiveRide.visibility = View.VISIBLE
                    binding.tvActiveDriver.text = "Driver: ${ride.driverName ?: "Searching..."}"
                    binding.tvActiveStatus.text = ride.status
                    binding.tvActivePickup.text = "Pickup: ${ride.pickupAddress}"
                    binding.tvActiveDest.text = "Drop: ${ride.destinationAddress}"
                } else {
                    binding.cardActiveRide.visibility = View.GONE
                }
            }
        }

        viewModel.rideHistory.observe(viewLifecycleOwner) { result ->
            result.onSuccess { history ->
                binding.root.findViewById<View>(R.id.errorLayout)?.visibility = View.GONE
                if (history.isEmpty()) {
                    binding.root.findViewById<View>(R.id.emptyLayout)?.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                } else {
                    binding.root.findViewById<View>(R.id.emptyLayout)?.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                    binding.rvRideHistory.adapter = RideHistoryAdapter(history)
                }
            }.onFailure {
                binding.root.findViewById<View>(R.id.errorLayout)?.visibility = View.VISIBLE
                binding.root.findViewById<View>(R.id.emptyLayout)?.visibility = View.GONE
                binding.scrollView.visibility = View.GONE
            }
        }
    }
}