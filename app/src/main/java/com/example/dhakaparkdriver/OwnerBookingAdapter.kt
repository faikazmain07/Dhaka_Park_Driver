package com.example.dhakaparkdriver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dhakaparkdriver.databinding.ListItemOwnerBookingBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View // <--- ADD THIS LINE
// Define a lambda for click actions: (Booking object, action type string) -> Unit
class OwnerBookingAdapter(
    private val bookings: List<Booking>,
    private val onBookingAction: (Booking, String) -> Unit
) : RecyclerView.Adapter<OwnerBookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(val binding: ListItemOwnerBookingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ListItemOwnerBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        // Display Driver Email
        holder.binding.tvDriverEmail.text = booking.driverId // For now, just show driver ID; fetch email in activity if needed.

        // Display Booking Status
        holder.binding.tvBookingStatus.text = booking.status.capitalize(Locale.getDefault())
        // Optionally, change color based on status
        // holder.binding.tvBookingStatus.setTextColor(
        //     when(booking.status) {
        //         "active" -> ContextCompat.getColor(holder.itemView.context, R.color.green_500) // Assuming you define green_500
        //         "completed" -> ContextCompat.getColor(holder.itemView.context, R.color.blue_500) // Assuming you define blue_500
        //         "cancelled" -> ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
        //         else -> ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
        //     }
        // )

        // Display Intended Time
        val intendedStart = formatTime(booking.startTimeMillis)
        val intendedEnd = formatTime(booking.endTimeMillis)
        holder.binding.tvIntendedTime.text = "Intended: $intendedStart - $intendedEnd"

        // Display Actual Time (if available)
        if (booking.actualStartTime != null) {
            val actualStart = formatTime(booking.actualStartTime)
            val actualEnd = if (booking.actualEndTime != null) formatTime(booking.actualEndTime) else "Ongoing"
            holder.binding.tvActualTime.text = "Actual: $actualStart - $actualEnd"
            holder.binding.tvActualTime.visibility = View.VISIBLE
        } else {
            holder.binding.tvActualTime.visibility = View.GONE
        }

        // Display Total Price
        holder.binding.tvTotalPrice.text = "Total: ${booking.totalPrice}"

        // Set up click listeners for Edit and Delete icons
        holder.binding.ivEditBooking.setOnClickListener {
            onBookingAction(booking, "edit")
        }
        holder.binding.ivDeleteBooking.setOnClickListener {
            onBookingAction(booking, "delete")
        }
    }

    override fun getItemCount() = bookings.size

    // Helper function for formatting time
    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}