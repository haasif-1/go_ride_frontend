package com.goride.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.data.models.LocationModel
import com.goride.databinding.ItemRecentLocationBinding

class RecentLocationAdapter(
    private val locations: List<LocationModel>,
    private val onItemClick: (LocationModel) -> Unit
) : RecyclerView.Adapter<RecentLocationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRecentLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(location: LocationModel) {
            binding.tvName.text = location.name
            binding.tvAddress.text = location.address
            binding.root.setOnClickListener { onItemClick(location) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentLocationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(locations[position])
    }

    override fun getItemCount() = locations.size
}