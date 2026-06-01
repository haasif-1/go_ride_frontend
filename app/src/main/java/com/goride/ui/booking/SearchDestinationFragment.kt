package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.goride.R
import com.goride.base.BaseFragment
import com.goride.databinding.FragmentSearchDestinationBinding
import com.goride.ui.home.HomeViewModel
import com.goride.ui.home.RecentLocationAdapter

class SearchDestinationFragment : BaseFragment<FragmentSearchDestinationBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSearchDestinationBinding {
        return FragmentSearchDestinationBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        viewModel.recentLocations.observe(viewLifecycleOwner) { locations ->
            binding.rvRecentPlaces.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentPlaces.adapter = RecentLocationAdapter(locations) {
                findNavController().navigate(R.id.action_searchDestinationFragment_to_locationConfirmationFragment)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Mocking selection for MVP
        binding.etDestination.setOnEditorActionListener { _, _, _ ->
            findNavController().navigate(R.id.action_searchDestinationFragment_to_locationConfirmationFragment)
            true
        }
    }
}