package com.goride.ui.booking

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.R
import com.goride.data.models.RideResponse
import com.goride.databinding.ItemRideHistoryBinding

class RideHistoryAdapter(private val rides: List<RideResponse>) :
    RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRideHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ride: RideResponse) {
            binding.tvRideId.text = "#RID${ride.id}"
            binding.tvDate.text = ride.createdAt
            binding.tvPickup.text = ride.pickupAddress
            binding.tvDest.text = ride.destinationAddress
            binding.tvFare.text = ride.fare
            binding.tvVehicleType.text = ride.vehicleName ?: "Vehicle"
            binding.chipStatus.text = ride.status

            val statusColor = when (ride.status.uppercase()) {
                "COMPLETED" -> R.color.success
                "CANCELLED" -> R.color.error
                "REQUESTED" -> R.color.warning
                else -> R.color.primary
            }
            binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(
                binding.root.context.getColor(statusColor)
            )
            binding.chipStatus.setTextColor(binding.root.context.getColor(R.color.white))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRideHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rides[position])
    }

    override fun getItemCount() = rides.size
}