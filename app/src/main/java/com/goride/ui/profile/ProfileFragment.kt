package com.goride.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.models.UserProfile
import com.goride.data.repository.BookingHistoryManager
import com.goride.data.repository.DataStoreManager
import com.goride.databinding.FragmentProfileBinding

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            DataStoreManager(requireContext()),
            BookingHistoryManager(requireContext())
        )
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRealRideStats()
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let { updateProfileUI(it) } ?: run {
                binding.tvUserName.text = "Set up your profile"
                binding.tvUserEmail.text = "Add email"
                binding.tvUserPhone.text = ""
            }
        }

        viewModel.rideStats.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalBookings.text = stats.totalBookings.toString()
            binding.tvCompletedBookings.text = stats.completedBookings.toString()
            binding.tvCancelledBookings.text = stats.cancelledBookings.toString()
            binding.tvLastBookingDate.text = stats.lastBookingDate
            binding.tvPreferredVehicle.text = stats.preferredVehicle
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateProfileUI(profile: UserProfile) {
        binding.tvUserName.text = profile.name.ifEmpty { "User" }
        binding.tvUserEmail.text = profile.email
        binding.tvUserPhone.text = profile.phone.ifEmpty { "No phone number added" }
        binding.chipUserRole.text = profile.role

        if (!profile.profileImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.profileImage)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfileImage)
        } else {
            binding.ivProfileImage.setImageResource(R.drawable.ic_person)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
                findNavController().navigate(R.id.action_global_loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
