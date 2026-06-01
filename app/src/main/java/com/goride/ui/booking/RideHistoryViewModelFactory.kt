package com.goride.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goride.data.repository.RideHistoryRepository

class RideHistoryViewModelFactory(
    private val repository: RideHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RideHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RideHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}