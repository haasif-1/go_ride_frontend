package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.data.models.NominatimPlace
import com.goride.databinding.ItemSearchResultBinding

class PlaceSearchAdapter(
    private val places: List<NominatimPlace>,
    private val onClick: (NominatimPlace) -> Unit
) : RecyclerView.Adapter<PlaceSearchAdapter.PlaceViewHolder>() {

    inner class PlaceViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: NominatimPlace) {
            val parts = place.displayName.split(",", limit = 2)
            val mainLocation = parts.getOrNull(0)?.trim() ?: ""
            val fullAddress = parts.getOrNull(1)?.trim() ?: ""

            binding.tvMainLocation.text = mainLocation
            binding.tvFullAddress.text = fullAddress

            binding.root.setOnClickListener {
                onClick(place)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlaceViewHolder {

        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PlaceViewHolder(binding)
    }

    override fun getItemCount() = places.size

    override fun onBindViewHolder(
        holder: PlaceViewHolder,
        position: Int
    ) {
        holder.bind(places[position])
    }
}