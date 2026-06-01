package com.goride.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.repository.AuthRepository
import com.goride.data.repository.DataStoreManager
import com.goride.databinding.FragmentProfileBinding
import com.goride.ui.auth.AuthViewModel
import com.goride.ui.auth.AuthViewModelFactory
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            AuthRepository(RetrofitClient.authApiService, RetrofitClient.apiService),
            DataStoreManager(requireContext())
        )
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupListeners()
        observeViewModel()
        viewModel.getProfile()
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility = 
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvUserName.text = user.fullName
                binding.tvUserEmail.text = user.email
            }.onFailure {
                Toast.makeText(requireContext(), "Error fetching profile: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        val dataStoreManager = DataStoreManager(requireContext())
        lifecycleScope.launch {
            dataStoreManager.clearSession()
            findNavController().navigate(R.id.action_global_loginFragment)
        }
    }
}