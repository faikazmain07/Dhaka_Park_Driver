package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemOwnedParkingSpotBinding

class OwnedParkingSpotAdapter(private val spots: List<ParkingSpot>) :
    RecyclerView.Adapter<OwnedParkingSpotAdapter.OwnedSpotViewHolder>() {

    inner class OwnedSpotViewHolder(val binding: ListItemOwnedParkingSpotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnedSpotViewHolder {
        val binding = ListItemOwnedParkingSpotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OwnedSpotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OwnedSpotViewHolder, position: Int) {
        val spot = spots[position]
        holder.binding.tvSpotName.text = spot.name
        holder.binding.tvAvailableSlots.text = "Available: ${spot.availableSlots}/${spot.totalSlots}" // totalSlots is in ParkingSpot model now
        holder.binding.tvPriceHours.text = "${spot.pricePerHour} BDT/hr | ${spot.operatingHours}" // operatingHours is in ParkingSpot model now
        // Placeholder for address for now (if not stored in Firestore directly)
        holder.binding.tvAddress.text = "Lat: %.4f, Lng: %.4f".format(spot.location?.latitude ?: 0.0, spot.location?.longitude ?: 0.0)

        // TODO: Add click listener for individual spot details/management later
    }

    override fun getItemCount() = spots.size
}