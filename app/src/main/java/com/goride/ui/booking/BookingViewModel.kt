package com.goride.ui.booking

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goride.data.models.RideData
import com.goride.data.models.RideRequest
import com.goride.data.repository.BookingRepository
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.Locale

class BookingViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _rideBookingResult = MutableLiveData<Result<RideData>>()
    val rideBookingResult: LiveData<Result<RideData>> = _rideBookingResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun requestRide(
        pickupLat: Double,
        pickupLng: Double,
        destLat: Double,
        destLng: Double,
        vehicleType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // ── Coordinate Rounding ──────────────────────────────────────────
            val rPickupLat = roundCoordinate(pickupLat)
            val rPickupLng = roundCoordinate(pickupLng)
            val rDestLat = roundCoordinate(destLat)
            val rDestLng = roundCoordinate(destLng)

            // ── Payload Debug ──────────────────────────────────────────────────
            Log.d("RIDE_DEBUG", "--- REQUEST START (ViewModel) ---")
            Log.d("RIDE_DEBUG", "Original: pickupLat=$pickupLat, pickupLng=$pickupLng")
            Log.d("RIDE_DEBUG", "Rounded:  pickupLat=$rPickupLat, pickupLng=$rPickupLng")
            Log.d("RIDE_DEBUG", "Original: destLat=$destLat, destLng=$destLng")
            Log.d("RIDE_DEBUG", "Rounded:  destLat=$rDestLat, destLng=$rDestLng")
            Log.d("RIDE_DEBUG", "vehicleType=$vehicleType")

            try {
                val request = RideRequest(rPickupLat, rPickupLng, rDestLat, rDestLng, vehicleType)
                val response = repository.requestRide(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success && body.data != null) {
                        Log.d("RIDE_DEBUG", "SUCCESS: Ride ID = ${body.data.id}")
                        _rideBookingResult.value = Result.success(body.data)
                    } else {
                        val msg = body.message.ifEmpty { "Booking failed" }
                        Log.e("RIDE_DEBUG", "FAILURE (Business logic): $msg")
                        _rideBookingResult.value = Result.failure(Exception(msg))
                    }
                } else {
                    val errorMsg = formatErrorMessage(response)
                    Log.e("RIDE_DEBUG", "FAILURE (HTTP): $errorMsg")
                    _rideBookingResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e("RIDE_DEBUG", "ERROR: No connection", e)
                _rideBookingResult.value = Result.failure(
                    Exception("No internet connection. Please check your network.")
                )
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("RIDE_DEBUG", "ERROR: Timeout", e)
                _rideBookingResult.value = Result.failure(
                    Exception("Request timed out. Please try again.")
                )
            } catch (e: Exception) {
                Log.e("RIDE_DEBUG", "ERROR: Unexpected", e)
                _rideBookingResult.value = Result.failure(
                    Exception(e.message ?: "Unexpected error occurred.")
                )
            } finally {
                _isLoading.value = false
                Log.d("RIDE_DEBUG", "--- REQUEST END ---")
            }
        }
    }

    private fun roundCoordinate(value: Double): Double {
        return try {
            String.format(Locale.US, "%.6f", value).toDouble()
        } catch (e: Exception) {
            value
        }
    }

    private fun formatErrorMessage(response: Response<*>): String {
        val code = response.code()
        val errorBodyString = response.errorBody()?.string() ?: "{}"
        return "HTTP $code\n$errorBodyString"
    }
}
