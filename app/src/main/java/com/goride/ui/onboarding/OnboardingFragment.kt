package com.goride.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentOnboardingBinding

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {

    private lateinit var adapter: OnboardingAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOnboardingBinding {
        return FragmentOnboardingBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        val items = listOf(
            OnboardingItem(
                "Ride Anytime, Anywhere",
                "Book a ride in seconds and get picked up wherever you are. Fast, reliable transportation at your fingertips.",
                R.drawable.ic_launcher_foreground // Placeholder
            ),
            OnboardingItem(
                "Safe, Smart & Accessible",
                "Choose the ride that fits your needs — including wheelchair-accessible vehicles — with secure payments and transparent pricing.",
                R.drawable.ic_launcher_foreground // Placeholder
            )
        )

        adapter = OnboardingAdapter(items)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val progress = ((position + 1).toFloat() / items.size * 100).toInt()
                binding.progressBar.progress = progress
            }
        })
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < adapter.itemCount) {
                binding.viewPager.currentItem += 1
            } else {
                navigateToLogin()
            }
        }

        binding.btnSkip.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
    }
}