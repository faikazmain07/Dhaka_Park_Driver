package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemParkingSpotBinding
import java.text.DecimalFormat

// ADD a click listener parameter to the constructor
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

        if (spot.distance != null) {
            val distanceInKm = spot.distance!! / 1000
            val df = DecimalFormat("#.##")
            holder.binding.tvDistance.text = "${df.format(distanceInKm)} km"
        } else {
            holder.binding.tvDistance.text = "-- km"
        }

        // SET THE CLICK LISTENER ON THE ITEM VIEW
        holder.itemView.setOnClickListener {
            onSpotClicked(spot)
        }
    }

    override fun getItemCount() = spots.size
}