package com.goride.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.data.api.RetrofitClient
import com.goride.data.repository.AuthRepository
import com.goride.data.repository.DataStoreManager
import com.goride.databinding.FragmentSignupBinding

class SignupFragment : BaseFragment<FragmentSignupBinding>() {

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            AuthRepository(RetrofitClient.authApiService, RetrofitClient.apiService),
            DataStoreManager(requireContext())
        )
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSignupBinding {
        return FragmentSignupBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(fullName, email, password, confirmPassword)) {
                viewModel.register(fullName, email, password)
            }
        }

        binding.btnLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInput(fullName: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email is required"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.findViewById<View>(R.id.loadingLayout)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignUp.isEnabled = !isLoading
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Registration Successful. Please Login.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }.onFailure {
                Toast.makeText(requireContext(), "Registration Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}