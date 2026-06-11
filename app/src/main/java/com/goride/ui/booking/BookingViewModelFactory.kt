package com.goride.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goride.data.repository.BookingRepository

class BookingViewModelFactory(
    private val repository: BookingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BookingViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                BookingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(RideTrackingViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                RideTrackingViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}