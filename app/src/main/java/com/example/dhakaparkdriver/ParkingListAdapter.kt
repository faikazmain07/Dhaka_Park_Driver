package com.example.dhakaparkdriver

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ItemParkingSpotBinding

// The context is passed in from the Activity to allow us to start a new activity from here.
class ParkingListAdapter(
    private val parkingSpots: List<ParkingSpot>,
    private val context: Context
) : RecyclerView.Adapter<ParkingListAdapter.ParkingSpotViewHolder>() {

    // The ViewHolder holds the views for a single list item, accessed via ViewBinding.
    inner class ParkingSpotViewHolder(val binding: ItemParkingSpotBinding) : RecyclerView.ViewHolder(binding.root)

    // This is called when a new ViewHolder is needed. It inflates the item layout.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingSpotViewHolder {
        val binding = ItemParkingSpotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParkingSpotViewHolder(binding)
    }

    // This returns the total number of items in the list.
    override fun getItemCount(): Int = parkingSpots.size

    // This is called to display the data at a specific position.
    override fun onBindViewHolder(holder: ParkingSpotViewHolder, position: Int) {
        val spot = parkingSpots[position]

        // Bind the data from our ParkingSpot object to the TextViews.
        holder.binding.tvSpotName.text = spot.spotName
        holder.binding.tvPrice.text = "${spot.pricePerHour ?: "N/A"} BDT/hr"

        // Format the distance nicely to one decimal place.
        if (spot.distance != null) {
            val distanceInKm = spot.distance!! / 1000
            holder.binding.tvDistance.text = String.format("%.1f km away", distanceInKm)
        } else {
            holder.binding.tvDistance.text = "Distance unknown"
        }

        // --- THIS IS THE ACTIVATED CLICK LISTENER ---
        holder.itemView.setOnClickListener {
            // Create an Intent to open the ParkingDetailActivity.
            val intent = Intent(context, ParkingDetailActivity::class.java)

            // Pass the unique ID of the clicked parking spot to the detail activity.
            // This is crucial for fetching the correct document from Firestore.
            intent.putExtra("PARKING_SPOT_ID", spot.id)

            // Start the new activity.
            context.startActivity(intent)
        }
    }
}