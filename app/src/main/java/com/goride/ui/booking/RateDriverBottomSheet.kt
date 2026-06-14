package com.goride.ui.booking

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.goride.databinding.BottomSheetRateDriverBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RateDriverBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateDriverBinding? = null
    private val binding get() = _binding!!

    var onNavigateHome: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRateDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val driverName = requireArguments().getString(ARG_DRIVER_NAME).orEmpty()
        val driverVehicle = requireArguments().getString(ARG_DRIVER_VEHICLE).orEmpty()

        binding.tvDriverName.text = driverName
        binding.tvDriverVehicle.text = driverVehicle

        binding.ivClose.setOnClickListener {

            dismissAllowingStateLoss()

            binding.root.postDelayed({
                onNavigateHome?.invoke()
            }, 200)
        }

        binding.btnRateDriver.setOnClickListener {

            val rating = binding.ratingBar.rating

            if (rating < 1f) {
                Toast.makeText(
                    requireContext(),
                    "Please select a rating",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val toast = Toast.makeText(
                requireContext(),
                "Thank you for rating $driverName",
                Toast.LENGTH_SHORT
            )

            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

            binding.root.postDelayed({

                dismissAllowingStateLoss()

                onNavigateHome?.invoke()

            }, 1200)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DRIVER_NAME = "driver_name"
        private const val ARG_DRIVER_VEHICLE = "driver_vehicle"

        fun newInstance(driverName: String, driverVehicle: String): RateDriverBottomSheet {
            return RateDriverBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DRIVER_NAME, driverName)
                    putString(ARG_DRIVER_VEHICLE, driverVehicle)
                }
            }
        }
    }
}
