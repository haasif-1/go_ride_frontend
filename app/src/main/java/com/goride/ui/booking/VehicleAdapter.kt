package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.R
import com.goride.data.models.VehicleModel
import com.goride.databinding.ItemVehicleBinding

class VehicleAdapter(
    private var vehicles: List<VehicleModel>,
    private val onVehicleSelected: (VehicleModel) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(private val binding: ItemVehicleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(vehicle: VehicleModel, position: Int) {
            binding.tvVehicleName.text = vehicle.name
            binding.tvPrice.text = vehicle.price
            binding.tvEta.text = vehicle.eta
            binding.tvSeats.text = vehicle.seats
            binding.ivVehicle.setImageResource(vehicle.imageResId)
            
            binding.tvPopular.visibility = if (vehicle.isPopular) View.VISIBLE else View.GONE
            
            val isSelected = position == selectedPosition
            binding.rbSelect.isChecked = isSelected
            
            if (isSelected) {
                binding.vehicleCard.setStrokeColor(binding.root.context.getColor(R.color.primary))
                binding.vehicleCard.setCardBackgroundColor(binding.root.context.getColor(R.color.primary_light))
            } else {
                binding.vehicleCard.setStrokeColor(binding.root.context.getColor(R.color.gray_200))
                binding.vehicleCard.setCardBackgroundColor(binding.root.context.getColor(R.color.white))
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onVehicleSelected(vehicle)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVehicleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(vehicles[position], position)
    }

    override fun getItemCount() = vehicles.size
}