package com.goride.ui.booking

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.R
import com.goride.data.models.LocalBookingRecord
import com.goride.databinding.ItemRideHistoryBinding

/**
 * Adapter for the Booking History screen backed by locally-stored
 * [LocalBookingRecord] objects.  No API calls or ViewModels needed here —
 * the data is already in memory when the adapter is created.
 */
class LocalBookingAdapter(private val bookings: List<LocalBookingRecord>) :
    RecyclerView.Adapter<LocalBookingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRideHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: LocalBookingRecord) {
            binding.tvRideId.text      = "#${record.rideId.take(8).uppercase()}"
            binding.tvDate.text        = record.bookedAt
            binding.tvPickup.text      = record.pickupAddress
            binding.tvDest.text        = record.destinationAddress
            binding.tvFare.text        = "Rs. %.0f".format(record.fare)
            binding.tvVehicleType.text = record.vehicleType

            binding.chipStatus.text = record.status

            val ctx = binding.root.context
            val colorRes = when (record.status.uppercase()) {
                "COMPLETED"  -> R.color.success
                "CANCELLED"  -> R.color.error
                "REQUESTED"  -> R.color.warning
                "CONFIRMED"  -> R.color.primary
                else         -> R.color.primary
            }
            binding.chipStatus.chipBackgroundColor =
                ColorStateList.valueOf(ctx.getColor(colorRes))
            binding.chipStatus.setTextColor(ctx.getColor(R.color.white))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRideHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(bookings[position])

    override fun getItemCount() = bookings.size
}
