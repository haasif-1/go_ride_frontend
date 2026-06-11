package com.goride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.goride.data.models.DriverModel
import com.goride.databinding.ItemDriverBinding

class DriverAdapter(
    private val drivers: List<DriverModel>,
    private val onDriverSelected: (DriverModel) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val binding = ItemDriverBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DriverViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        holder.bind(drivers[position])
    }

    override fun getItemCount(): Int = drivers.size

    inner class DriverViewHolder(private val binding: ItemDriverBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(driver: DriverModel) {
            binding.apply {
                tvDriverName.text = driver.name
                tvRating.text = driver.rating.toString()
                tvVehicleName.text = driver.vehicle
                tvPlateNumber.text = driver.plateNumber
                tvEta.text = "${driver.etaMinutes} min"
                tvFareBadge.text = "Rs. ${driver.fare.toInt()}"

                btnSelectDriver.setOnClickListener {
                    onDriverSelected(driver)
                }
            }
        }
    }
}
