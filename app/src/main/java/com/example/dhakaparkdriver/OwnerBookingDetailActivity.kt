package com.example.dhakaparkdriver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dhakaparkdriver.databinding.ActivityOwnerBookingDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query // <--- ADDED THIS IMPORT
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OwnerBookingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private var spotId: String? = null
    private var spotName: String? = null
    private var totalSlots: Long = 0
    private var pricePerHour: Long = 0

    private lateinit var ownerBookingAdapter: OwnerBookingAdapter
    private val bookingsList = mutableListOf<Booking>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        spotId = intent.getStringExtra("SELECTED_SPOT_ID")
        spotName = intent.getStringExtra("SELECTED_SPOT_NAME")

        if (spotId == null) {
            Toast.makeText(this, "Parking Spot ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupBookingsRecyclerView()
        fetchParkingSpotDetails(spotId!!)
    }

    override fun onResume() {
        super.onResume()
        if (spotId != null) {
            fetchBookingsForSpot(spotId!!) // Re-fetch bookings whenever activity comes to foreground
        }
    }

    private fun setupBookingsRecyclerView() {
        ownerBookingAdapter = OwnerBookingAdapter(bookingsList) { booking, actionType ->
            handleBookingAction(booking, actionType)
        }
        binding.rvSpotBookings.layoutManager = LinearLayoutManager(this)
        binding.rvSpotBookings.adapter = ownerBookingAdapter
    }

    private fun fetchParkingSpotDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("parking_spots").document(id).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    val name = document.getString("name") ?: "Unnamed Spot"
                    val total = document.getLong("totalSlots") ?: 0
                    val price = document.getLong("pricePerHour") ?: 0
                    val operatingHoursStartMillis = document.getLong("operatingHoursStartMillis") ?: 0
                    val operatingHoursEndMillis = document.getLong("operatingHoursEndMillis") ?: 0

                    spotName = name
                    totalSlots = total
                    pricePerHour = price

                    binding.tvSpotNameTitle.text = "Bookings for $name"
                    val formattedHours = if (operatingHoursStartMillis != 0L && operatingHoursEndMillis != 0L) {
                        val start = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(operatingHoursStartMillis))
                        val end = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(operatingHoursEndMillis))
                        "$start - $end"
                    } else "N/A"
                    binding.tvSpotDetails.text = "Total Slots: $total | Price: $price BDT/hr | Hours: $formattedHours"

                    fetchBookingsForSpot(id) // Now fetch bookings
                } else {
                    Toast.makeText(this, "Parking Spot not found.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching spot details: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun fetchBookingsForSpot(spotId: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("spotId", spotId)
            .orderBy("startTimeMillis", Query.Direction.ASCENDING) // Order by start time
            .get()
            .addOnSuccessListener { documents ->
                bookingsList.clear()
                for (document in documents) {
                    document.toObject(Booking::class.java).let { booking ->
                        if (booking != null) {
                            bookingsList.add(booking.copy(id = document.id))
                        }
                    }
                }
                ownerBookingAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                Log.d("OwnerBookingDetail", "Fetched ${bookingsList.size} bookings for spot: $spotId")
            }
            .addOnFailureListener { e ->
                Log.e("OwnerBookingDetail", "Error fetching bookings for spot $spotId", e)
                Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun handleBookingAction(booking: Booking, actionType: String) {
        when (actionType) {
            "edit" -> {
                Toast.makeText(this, "Edit booking: ${booking.id}", Toast.LENGTH_SHORT).show()
            }
            "delete" -> {
                deleteBooking(booking)
            }
        }
    }

    private fun deleteBooking(booking: Booking) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("bookings").document(booking.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Booking deleted successfully!", Toast.LENGTH_SHORT).show()
                Log.d("OwnerBookingDetail", "Booking ${booking.id} deleted.")
                // Refresh the list after deletion
                fetchBookingsForSpot(booking.spotId) // <--- CORRECTED CALL
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("OwnerBookingDetail", "Error deleting booking ${booking.id}", e)
                Toast.makeText(this, "Failed to delete booking: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    // Helper function for formatting time (from BookingActivity)
    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    // Helper function for formatting full date and time (useful for actual times)
    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}