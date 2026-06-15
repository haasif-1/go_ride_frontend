package com.goride.ui.profile

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.repository.BookingHistoryManager
import com.goride.data.repository.DataStoreManager
import com.goride.databinding.FragmentEditProfileBinding

class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            DataStoreManager(requireContext()),
            BookingHistoryManager(requireContext())
        )
    }

    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.ivProfileImage)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditProfileBinding {
        return FragmentEditProfileBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupListeners() {
        binding.btnChangePhoto.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnSaveChanges.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.etName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            viewModel.updateProfile(name, phone, selectedImageUri)
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.etName.setText(it.name)
                binding.etPhone.setText(it.phone)
                binding.etEmail.setText(it.email)
                
                // Only load from profile if user hasn't selected a new one in this session
                if (selectedImageUri == null && !it.profileImage.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.profileImage)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(binding.ivProfileImage)
                }
            }
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveChanges.isEnabled = !isLoading
            // You could show a progress bar here if one exists in the layout
        }
    }
}
