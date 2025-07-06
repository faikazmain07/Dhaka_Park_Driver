package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemParkingSpotBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingListAdapter(
    private val spots: List<ParkingSpot>,
    private val onSpotClicked: (ParkingSpot) -> Unit
) : RecyclerView.Adapter<ParkingListAdapter.SpotViewHolder>() {

    inner class SpotViewHolder(val binding: ListItemParkingSpotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotViewHolder {
        val binding = ListItemParkingSpotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SpotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpotViewHolder, position: Int) {
        val spot = spots[position]
        holder.binding.tvSpotName.text = spot.name
        holder.binding.tvPrice.text = "${spot.pricePerHour} BDT/hr"

        // Display available slots
        holder.binding.tvAvailableSlots.text = "Available: ${spot.availableSlots}/${spot.totalSlots}"

        // Display operating hours
        val formattedHours = if (spot.operatingHoursStartMillis != 0L && spot.operatingHoursEndMillis != 0L) {
            val start = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(spot.operatingHoursStartMillis))
            val end = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(spot.operatingHoursEndMillis))
            "$start - $end"
        } else "N/A"
        holder.binding.tvOperatingHours.text = formattedHours // Assuming you have tvOperatingHours in list_item_parking_spot.xml

        // Display parking type and vehicle types
        holder.binding.tvParkingTypeAndVehicles.text = "${spot.parkingType.capitalize(Locale.getDefault())} | ${spot.vehicleTypes.joinToString(", ").capitalize(Locale.getDefault())}" // Assuming you have tvParkingTypeAndVehicles in list_item_parking_spot.xml

        // Display distance
        if (spot.distance != null) {
            val distanceInKm = spot.distance!! / 1000
            val df = DecimalFormat("#.##")
            holder.binding.tvDistance.text = "${df.format(distanceInKm)} km"
        } else {
            holder.binding.tvDistance.text = "-- km"
        }

        holder.itemView.setOnClickListener {
            onSpotClicked(spot)
        }
    }

    override fun getItemCount() = spots.size
}