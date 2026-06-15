package com.goride.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goride.R
import com.goride.data.models.LocationModel
import com.goride.databinding.ItemSavedLocationBinding

class SavedLocationsAdapter(
    private val onDeleteClick: (LocationModel) -> Unit
) : ListAdapter<LocationModel, SavedLocationsAdapter.LocationViewHolder>(LocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemSavedLocationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LocationViewHolder(private val binding: ItemSavedLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: LocationModel) {
            binding.tvLocationName.text = location.name
            binding.tvLocationAddress.text = location.address
            
            // Set icon based on name for demo purposes
            val iconRes = when (location.name.lowercase()) {
                "home" -> R.drawable.ic_home
                "work" -> R.drawable.ic_shield // Using shield as a placeholder for work if ic_work is missing
                else -> R.drawable.ic_location
            }
            binding.ivLocationIcon.setImageResource(iconRes)

            binding.btnDelete.setOnClickListener {
                onDeleteClick(location)
            }
        }
    }

    class LocationDiffCallback : DiffUtil.ItemCallback<LocationModel>() {
        override fun areItemsTheSame(oldItem: LocationModel, newItem: LocationModel): Boolean {
            return oldItem.latitude == newItem.latitude && oldItem.longitude == newItem.longitude
        }

        override fun areContentsTheSame(oldItem: LocationModel, newItem: LocationModel): Boolean {
            return oldItem == newItem
        }
    }
}
