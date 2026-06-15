package com.goride.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.goride.data.models.LocalBookingRecord

/**
 * Manages local booking history using SharedPreferences + Gson.
 * Uses the same Gson pattern already established in DataStoreManager
 * for recentLocations, so no new dependencies are needed.
 */
class BookingHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "booking_history_prefs"
        private const val KEY_BOOKINGS  = "local_bookings"
    }

    /** Prepend a new booking to the list so that the latest is always first. */
    fun saveBooking(record: LocalBookingRecord) {
        val current = getBookings().toMutableList()
        current.add(0, record)          // latest first
        prefs.edit()
            .putString(KEY_BOOKINGS, gson.toJson(current))
            .apply()
    }

    /** Returns all saved bookings (latest first). */
    fun getBookings(): List<LocalBookingRecord> {
        val json = prefs.getString(KEY_BOOKINGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalBookingRecord>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Clears all locally stored bookings (useful for testing). */
    fun clearBookings() {
        prefs.edit().remove(KEY_BOOKINGS).apply()
    }
}
