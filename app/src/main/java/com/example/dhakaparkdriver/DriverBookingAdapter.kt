package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemDriverBookingBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriverBookingAdapter(private val bookings: List<Booking>) :
    RecyclerView.Adapter<DriverBookingAdapter.DriverBookingViewHolder>() {

    inner class DriverBookingViewHolder(val binding: ListItemDriverBookingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverBookingViewHolder {
        val binding = ListItemDriverBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DriverBookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DriverBookingAdapter.DriverBookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.binding.tvSpotName.text = "Loading spot name..."
        Firebase.firestore.collection("parking_spots").document(booking.spotId).get()
            .addOnSuccessListener { documentSnapshot ->
                val spotName = documentSnapshot.getString("name") ?: "Unknown Spot"
                holder.binding.tvSpotName.text = spotName
            }
            .addOnFailureListener {
                holder.binding.tvSpotName.text = "Error getting spot name"
            }

        holder.binding.tvBookingStatus.text = booking.status.capitalize(Locale.getDefault())

        val intendedStart = formatTime(booking.startTimeMillis)
        val intendedEnd = formatTime(booking.endTimeMillis)
        holder.binding.tvIntendedTime.text = "Intended: $intendedStart - $intendedEnd"

        if (booking.actualStartTime != null) {
            val actualStart = formatTime(booking.actualStartTime)
            val actualEnd = if (booking.actualEndTime != null) formatTime(booking.actualEndTime) else "Ongoing"
            holder.binding.tvActualTime.text = "Actual: $actualStart - $actualEnd"
            holder.binding.tvActualTime.visibility = View.VISIBLE
        } else {
            holder.binding.tvActualTime.visibility = View.GONE
        }

        holder.binding.tvTotalPrice.text = "Total: ${booking.totalPrice}"
        holder.binding.tvNumberOfSlots.text = "Slots: ${booking.numberOfSlots}"
    }

    override fun getItemCount() = bookings.size

    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}