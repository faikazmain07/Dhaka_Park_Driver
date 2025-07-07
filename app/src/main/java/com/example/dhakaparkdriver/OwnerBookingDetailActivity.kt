package com.example.dhakaparkdriver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView // NEW IMPORT
import android.widget.ProgressBar // NEW IMPORT
import android.widget.ImageView // NEW IMPORT
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
// Removed: import com.example.dhakaparkdriver.databinding.ActivityOwnerBookingDetailBinding <-- DELETE THIS LINE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OwnerBookingDetailActivity : AppCompatActivity() {

    // REMOVED: private lateinit var binding: ActivityOwnerBookingDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private var spotId: String? = null
    private var spotName: String? = null
    private var totalSlots: Long = 0
    private var pricePerHour: Long = 0

    private lateinit var ownerBookingAdapter: OwnerBookingAdapter
    private val bookingsList = mutableListOf<Booking>()

    // Manual View References
    private lateinit var tvSpotNameTitle: TextView
    private lateinit var tvSpotDetails: TextView
    private lateinit var tvBookingsLabel: TextView
    private lateinit var rvSpotBookings: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed: binding = ActivityOwnerBookingDetailBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_owner_booking_detail) // Set content view directly

        // --- MANUAL findViewById for all views ---
        tvSpotNameTitle = findViewById(R.id.tvSpotNameTitle)
        tvSpotDetails = findViewById(R.id.tvSpotDetails)
        tvBookingsLabel = findViewById(R.id.tvBookingsLabel)
        rvSpotBookings = findViewById(R.id.rvSpotBookings)
        progressBar = findViewById(R.id.progressBar)
        // --- END MANUAL findViewById ---


        // Get spot details passed from OwnerBookingManagementActivity
        spotId = intent.getStringExtra("SELECTED_SPOT_ID")
        spotName = intent.getStringExtra("SELECTED_SPOT_NAME")

        if (spotId == null) {
            Toast.makeText(this, "Parking Spot ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Fetch parking spot details to display name, total slots, price
        fetchParkingSpotDetails(spotId!!)

        // Setup RecyclerView for bookings
        setupBookingsRecyclerView()
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
        rvSpotBookings.layoutManager = LinearLayoutManager(this) // Use manual rvSpotBookings
        rvSpotBookings.adapter = ownerBookingAdapter // Use manual rvSpotBookings
    }

    private fun fetchParkingSpotDetails(id: String) {
        progressBar.visibility = View.VISIBLE // Use manual progressBar
        db.collection("parking_spots").document(id).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE // Use manual progressBar
                if (document.exists()) {
                    val name = document.getString("name") ?: "Unnamed Spot"
                    val total = document.getLong("totalSlots") ?: 0
                    val price = document.getLong("pricePerHour") ?: 0
                    val operatingHoursStartMillis = document.getLong("operatingHoursStartMillis") ?: 0
                    val operatingHoursEndMillis = document.getLong("operatingHoursEndMillis") ?: 0

                    spotName = name
                    totalSlots = total
                    pricePerHour = price

                    // Display spot name and general details
                    tvSpotNameTitle.text = "Bookings for $name" // Use manual tvSpotNameTitle
                    val formattedHours = if (operatingHoursStartMillis != 0L && operatingHoursEndMillis != 0L) {
                        val start = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(operatingHoursStartMillis))
                        val end = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(operatingHoursEndMillis))
                        "$start - $end"
                    } else "N/A"
                    tvSpotDetails.text = "Total Slots: $total | Price: $price BDT/hr | Hours: $formattedHours" // Use manual tvSpotDetails

                    fetchBookingsForSpot(id) // Now fetch bookings
                } else {
                    Toast.makeText(this, "Parking Spot not found.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE // Use manual progressBar
                Toast.makeText(this, "Error fetching spot details: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun fetchBookingsForSpot(spotId: String) {
        progressBar.visibility = View.VISIBLE // Use manual progressBar
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
                progressBar.visibility = View.GONE // Use manual progressBar
                Log.d("OwnerBookingDetail", "Fetched ${bookingsList.size} bookings for spot: $spotId")
            }
            .addOnFailureListener { e ->
                Log.e("OwnerBookingDetail", "Error fetching bookings for spot $spotId", e)
                Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE // Use manual progressBar
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
        progressBar.visibility = View.VISIBLE // Use manual progressBar
        db.collection("bookings").document(booking.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Booking deleted successfully!", Toast.LENGTH_SHORT).show()
                Log.d("OwnerBookingDetail", "Booking ${booking.id} deleted.")
                fetchBookingsForSpot(booking.spotId)
                progressBar.visibility = View.GONE // Use manual progressBar
            }
            .addOnFailureListener { e ->
                Log.e("OwnerBookingDetail", "Error deleting booking ${booking.id}", e)
                Toast.makeText(this, "Failed to delete booking: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE // Use manual progressBar
            }
    }

    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}