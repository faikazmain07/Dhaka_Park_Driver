package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemOwnedParkingSpotBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ADD a click listener parameter to the constructor
class OwnedParkingSpotAdapter(
    private val spots: List<ParkingSpot>,
    private val onSpotClicked: (ParkingSpot) -> Unit // <--- ADD THIS PARAMETER
) : RecyclerView.Adapter<OwnedParkingSpotAdapter.OwnedSpotViewHolder>() {

    inner class OwnedSpotViewHolder(val binding: ListItemOwnedParkingSpotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnedSpotViewHolder {
        val binding = ListItemOwnedParkingSpotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OwnedSpotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OwnedSpotViewHolder, position: Int) {
        val spot = spots[position]
        holder.binding.tvSpotName.text = spot.name
        holder.binding.tvAvailableSlots.text = "Available: ${spot.availableSlots}/${spot.totalSlots}"

        val formattedHours = if (spot.operatingHoursStartMillis != 0L && spot.operatingHoursEndMillis != 0L) {
            val start = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(spot.operatingHoursStartMillis))
            val end = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(spot.operatingHoursEndMillis))
            "$start - $end"
        } else {
            "N/A"
        }
        holder.binding.tvPriceHours.text = "${spot.pricePerHour} BDT/hr | $formattedHours"

        val lat = spot.location?.latitude ?: 0.0
        val lng = spot.location?.longitude ?: 0.0
        holder.binding.tvAddress.text = "Lat: %.4f, Lng: %.4f".format(Locale.getDefault(), lat, lng)

        val vehicleTypesString = spot.vehicleTypes.joinToString(", ") { it.capitalize(Locale.getDefault()) }
        holder.binding.tvParkingTypeAndVehicles.text = "${spot.parkingType.capitalize(Locale.getDefault())} | ${vehicleTypesString}"

        // --- ADD THE CLICK LISTENER TO THE ITEM VIEW ---
        holder.itemView.setOnClickListener {
            onSpotClicked(spot) // Call the lambda when the item is clicked
        }
    }

    override fun getItemCount() = spots.size
}