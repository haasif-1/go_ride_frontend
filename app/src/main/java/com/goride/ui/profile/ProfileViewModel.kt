package com.goride.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideStats
import com.goride.data.models.UserProfile
import com.goride.data.repository.BookingHistoryManager
import com.goride.data.repository.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val dataStoreManager: DataStoreManager,
    private val bookingHistoryManager: BookingHistoryManager
) : ViewModel() {

    // Observe user profile changes directly from DataStore
    val userProfile: LiveData<UserProfile?> = dataStoreManager.userProfile.asLiveData()

    private val _rideStats = MutableLiveData<RideStats>()
    val rideStats: LiveData<RideStats> = _rideStats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Event to notify UI when profile update is successful
    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    init {
        loadRealRideStats()
        ensureProfileExists()
    }

    private fun ensureProfileExists() {
        viewModelScope.launch {
            val profile = dataStoreManager.userProfile.first()
            if (profile == null) {
                // Initial default values for a new user
                val email = dataStoreManager.userEmail.first() ?: "user@goride.com"
                dataStoreManager.saveUserProfile(
                    UserProfile(
                        name = "GoRide User",
                        email = email,
                        phone = "",
                        role = "Rider"
                    )
                )
            }
        }
    }

    fun loadRealRideStats() {
        viewModelScope.launch {
            val bookings = bookingHistoryManager.getBookings()
            if (bookings.isEmpty()) {
                _rideStats.value = RideStats()
                return@launch
            }

            val total = bookings.size
            val completed = bookings.count { 
                it.status.equals("Completed", ignoreCase = true) || 
                it.status.equals("Success", ignoreCase = true) 
            }
            val cancelled = bookings.count { it.status.equals("Cancelled", ignoreCase = true) }
            
            // Format: "15 Jun 2026, 05:14 PM" or similar if possible, otherwise keep it simple
            val lastBooking = bookings.firstOrNull()
            val lastDate = lastBooking?.bookedAt?.split("T")?.get(0) ?: "--"
            
            val preferredVehicle = bookings.groupBy { it.vehicleType }
                .maxByOrNull { it.value.size }?.key ?: "--"

            _rideStats.value = RideStats(
                totalBookings = total,
                completedBookings = completed,
                cancelledBookings = cancelled,
                lastBookingDate = lastDate,
                preferredVehicle = preferredVehicle
            )
        }
    }

    /**
     * Updates user profile data and persists it locally.
     * Profile image is saved to internal storage to ensure it survives URI permission losses.
     */
    fun updateProfile(name: String, phone: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProfile = dataStoreManager.userProfile.first() ?: UserProfile()
                
                var profileImagePath = currentProfile.profileImage
                if (newImageUri != null) {
                    // Permanently save image to internal storage
                    val savedPath = dataStoreManager.saveProfileImageToInternal(newImageUri)
                    if (savedPath != null) {
                        profileImagePath = savedPath
                    }
                }

                val updatedProfile = currentProfile.copy(
                    name = name,
                    phone = phone,
                    profileImage = profileImagePath
                )
                
                dataStoreManager.saveUserProfile(updatedProfile)
                _updateStatus.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _updateStatus.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearSession()
        }
    }
}

class ProfileViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val bookingHistoryManager: BookingHistoryManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(dataStoreManager, bookingHistoryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
