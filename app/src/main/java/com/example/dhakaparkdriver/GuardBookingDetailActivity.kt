package com.example.dhakaparkdriver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityGuardBookingDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GuardBookingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardBookingDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private var bookingId: String? = null // The ID scanned from QR code
    private var currentBooking: Booking? = null // Holds the fetched booking data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the booking ID passed from GuardDashboardActivity
        bookingId = intent.getStringExtra("BOOKING_ID")

        if (bookingId == null) {
            Toast.makeText(this, "Booking ID not found. Cannot display details.", Toast.LENGTH_LONG).show()
            Log.e("GuardBookingDetail", "Booking ID is null on creation.")
            finish()
            return
        }

        fetchBookingDetails(bookingId!!) // Fetch details when activity starts
        setupClickListeners()
    }

    private fun fetchBookingDetails(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("bookings").document(id).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    currentBooking = document.toObject(Booking::class.java)?.copy(id = document.id)
                    if (currentBooking != null) {
                        displayBookingData(currentBooking!!)
                    } else {
                        Toast.makeText(this, "Booking data is malformed.", Toast.LENGTH_LONG).show()
                        Log.e("GuardBookingDetail", "Fetched document is malformed: ${document.id}")
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Booking not found.", Toast.LENGTH_LONG).show()
                    Log.w("GuardBookingDetail", "Booking document not found for ID: $id")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching booking: ${exception.message}", Toast.LENGTH_LONG).show()
                Log.e("GuardBookingDetail", "Error fetching booking details for ID: $id", exception)
                finish()
            }
    }

    private fun displayBookingData(booking: Booking) {
        binding.tvBookingStatus.text = booking.status.capitalize(Locale.getDefault()) // Display status
        binding.tvSpotName.text = "Spot ID: ${booking.spotId}" // For now, just show Spot ID

        // Fetch Driver's email
        db.collection("users").document(booking.driverId).get()
            .addOnSuccessListener { userDoc ->
                val driverEmail = userDoc.getString("email") ?: "Unknown Driver"
                binding.tvDriverEmail.text = driverEmail
            }
            .addOnFailureListener {
                binding.tvDriverEmail.text = "Error fetching driver email."
                Log.e("GuardBookingDetail", "Error fetching driver email for ${booking.driverId}", it)
            }

        // Display intended times
        val intendedStart = formatTime(booking.startTimeMillis)
        val intendedEnd = formatTime(booking.endTimeMillis)
        binding.tvIntendedTime.text = "$intendedStart - $intendedEnd"

        // Display actual times if set
        if (booking.actualStartTime != null) {
            binding.tvActualStartTimeLabel.visibility = View.VISIBLE
            binding.tvActualStartTime.visibility = View.VISIBLE
            binding.tvActualStartTime.text = formatTime(booking.actualStartTime)
        } else {
            binding.tvActualStartTimeLabel.visibility = View.GONE
            binding.tvActualStartTime.visibility = View.GONE
        }

        if (booking.actualEndTime != null) {
            binding.tvActualEndTimeLabel.visibility = View.VISIBLE
            binding.tvActualEndTime.visibility = View.VISIBLE
            binding.tvActualEndTime.text = formatTime(booking.actualEndTime)
        } else {
            binding.tvActualEndTimeLabel.visibility = View.GONE
            binding.tvActualEndTime.visibility = View.GONE
        }

        // Display total price
        binding.tvTotalPrice.text = booking.totalPrice

        // Adjust button states based on booking status
        updateButtonStates(booking.status)
    }

    private fun updateButtonStates(status: String) {
        when (status) {
            "confirmed" -> { // Booking is confirmed but not started
                binding.btnStartSession.isEnabled = true
                binding.btnEndSession.isEnabled = false
            }
            "active" -> { // Session is ongoing
                binding.btnStartSession.isEnabled = false
                binding.btnEndSession.isEnabled = true
            }
            "completed", "cancelled", "no_show" -> { // Session is finished
                binding.btnStartSession.isEnabled = false
                binding.btnEndSession.isEnabled = false
                Toast.makeText(this, "Session already ${status.capitalize(Locale.getDefault())}.", Toast.LENGTH_SHORT).show()
            }
            else -> { // Default/Unknown state
                binding.btnStartSession.isEnabled = false
                binding.btnEndSession.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnStartSession.setOnClickListener {
            startParkingSession()
        }
        binding.btnEndSession.setOnClickListener {
            endParkingSession()
        }
    }

    private fun startParkingSession() {
        val booking = currentBooking ?: run {
            Toast.makeText(this, "Booking data missing.", Toast.LENGTH_SHORT).show()
            return
        }
        if (booking.status != "confirmed") {
            Toast.makeText(this, "Session can only be started from 'confirmed' status.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnStartSession.isEnabled = false
        binding.btnEndSession.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "status" to "active",
            "actualStartTime" to System.currentTimeMillis(),
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("bookings").document(booking.id).update(updates)
            .addOnSuccessListener {
                Log.d("GuardBookingDetail", "Booking ${booking.id} status updated to ACTIVE.")
                Toast.makeText(this, "Session started successfully!", Toast.LENGTH_SHORT).show()
                updateButtonStates("active") // Update UI immediately

                // --- CRITICAL: Decrement available slots in the parking spot using simple update ---
                updateParkingSpotSlots(booking.spotId, -1) // -1 to decrement
                binding.progressBar.visibility = View.GONE // Hide progress bar after booking update
            }
            .addOnFailureListener { e ->
                Log.e("GuardBookingDetail", "Error starting session for booking ${booking.id}", e)
                Toast.makeText(this, "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnStartSession.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun endParkingSession() {
        val booking = currentBooking ?: run {
            Toast.makeText(this, "Booking data missing.", Toast.LENGTH_SHORT).show()
            return
        }
        if (booking.status != "active") {
            Toast.makeText(this, "Session can only be ended from 'active' status.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnStartSession.isEnabled = false
        binding.btnEndSession.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "actualEndTime" to System.currentTimeMillis(),
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("bookings").document(booking.id).update(updates)
            .addOnSuccessListener {
                Log.d("GuardBookingDetail", "Booking ${booking.id} status updated to COMPLETED.")
                Toast.makeText(this, "Session ended successfully!", Toast.LENGTH_SHORT).show()
                updateButtonStates("completed") // Update UI immediately

                // --- CRITICAL: Increment available slots in the parking spot using simple update ---
                updateParkingSpotSlots(booking.spotId, 1) // +1 to increment
                binding.progressBar.visibility = View.GONE // Hide progress bar after booking update
            }
            .addOnFailureListener { e ->
                Log.e("GuardBookingDetail", "Error ending session for booking ${booking.id}", e)
                Toast.makeText(this, "Failed to end session: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnEndSession.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
    }

    // --- NEW: Function to Update Parking Spot Available Slots (Simple Read-Modify-Write) ---
    private fun updateParkingSpotSlots(spotId: String, change: Int) {
        db.collection("parking_spots").document(spotId).get() // Read the current value
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val currentAvailableSlots = documentSnapshot.getLong("availableSlots") ?: 0L
                    val newAvailableSlots = currentAvailableSlots + change

                    val totalSlots = documentSnapshot.getLong("totalSlots") ?: 0L

                    // Basic bounds check to prevent going below 0 or above totalSlots
                    if (newAvailableSlots < 0 || newAvailableSlots > totalSlots) {
                        Log.w("GuardBookingDetail", "Slot update rejected: New slots ($newAvailableSlots) out of bounds (total $totalSlots) for $spotId. Current: $currentAvailableSlots")
                        Toast.makeText(this, "Slot update invalid: Cannot go below 0 or above total.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener // Stop here
                    }

                    // Write the new value directly
                    db.collection("parking_spots").document(spotId).update(
                        "availableSlots", newAvailableSlots,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                        .addOnSuccessListener {
                            Log.d("GuardBookingDetail", "Available slots for $spotId updated to $newAvailableSlots successfully.")
                            // No Toast needed, primary action feedback is enough.
                        }
                        .addOnFailureListener { e ->
                            Log.e("GuardBookingDetail", "Failed to write updated available slots for $spotId: ${e.message}", e)
                            Toast.makeText(this, "Failed to update available slots: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Log.e("GuardBookingDetail", "Parking spot document $spotId does not exist for slot update.")
                    Toast.makeText(this, "Parking spot not found for slot update.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GuardBookingDetail", "Failed to read parking spot $spotId for slot update: ${e.message}", e)
                Toast.makeText(this, "Failed to get parking spot data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}