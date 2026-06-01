package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentBookingSuccessBinding

class BookingSuccessFragment : BaseFragment<FragmentBookingSuccessBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBookingSuccessBinding {
        return FragmentBookingSuccessBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Apply pop-in animation to the success icon
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.ivSuccess.startAnimation(animation)

        binding.btnBackHome.setOnClickListener {
            // Navigate back to home and clear history
            findNavController().navigate(R.id.action_bookingSuccessFragment_to_homeFragment)
        }
    }
}