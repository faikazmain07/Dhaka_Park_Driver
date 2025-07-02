package com.example.dhakaparkdriver

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityBookingBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private var spotId: String? = null
    private var pricePerHour: Long = 0
    private var totalSlots: Long = 0
    private var startTimeMillis: Long? = null
    private var endTimeMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        spotId = intent.getStringExtra("SPOT_ID")
        val spotName = intent.getStringExtra("SPOT_NAME") ?: "Unknown Spot"
        pricePerHour = intent.getLongExtra("SPOT_PRICE_PER_HOUR", 0)
        totalSlots = intent.getLongExtra("SPOT_AVAILABLE_SLOTS", 0)

        binding.tvSpotName.text = spotName
        binding.tvTotalSlots.text = "Total Parking Slots: $totalSlots"

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.tvStartTime.setOnClickListener { showTimePicker(isStartTime = true) }
        binding.tvEndTime.setOnClickListener { showTimePicker(isStartTime = false) }
        binding.btnConfirmBooking.setOnClickListener { confirmBooking() }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
            .setTitleText(if (isStartTime) "Select Start Time" else "Select End Time")
            .build()

        picker.show(supportFragmentManager, "TIME_PICKER_TAG")

        picker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val selectedTimeMillis = calendar.timeInMillis
            val formattedTime = formatTime(selectedTimeMillis)

            if (isStartTime) {
                startTimeMillis = selectedTimeMillis
                binding.tvStartTime.text = formattedTime
            } else {
                endTimeMillis = selectedTimeMillis
                binding.tvEndTime.text = formattedTime
            }

            if (startTimeMillis != null && endTimeMillis != null) {
                checkAvailability()
            }
        }
    }

    // --- THIS IS THE MODIFIED "TEST VERSION" OF THE FUNCTION ---
    private fun checkAvailability() {
        // Basic validation to ensure we have the necessary data before proceeding.
        if (startTimeMillis == null || endTimeMillis == null || spotId == null) {
            Toast.makeText(this, "Time or Spot ID is missing.", Toast.LENGTH_SHORT).show()
            return // Exit if data is incomplete
        }

        // Ensure the end time is after the start time.
        if (endTimeMillis!! <= startTimeMillis!!) {
            Toast.makeText(this, "End time must be after start time.", Toast.LENGTH_SHORT).show()
            binding.btnConfirmBooking.isEnabled = false
            binding.tvTotalPrice.text = "-- BDT"
            return // Exit if times are invalid
        }

        // --- TEMPORARY TEST LOGIC ---
        // We are completely skipping the Firestore query for now.
        // We will assume that a slot is always available and proceed directly to the price calculation.
        Log.d("BookingActivity", "TEST MODE: Bypassing Firestore availability check.")
        Toast.makeText(this, "DEBUG: Checking price...", Toast.LENGTH_SHORT).show()
        binding.tvTimeAvailability.text = "Slots available (Debug Mode)"

        // Because we are assuming a slot is available, we now directly call the price calculation.
        calculatePrice()
    }

    private fun calculatePrice() {
        if (pricePerHour <= 0) {
            binding.tvTotalPrice.text = "Price not available"
            binding.btnConfirmBooking.isEnabled = false
            return
        }

        val durationMillis = endTimeMillis!! - startTimeMillis!!
        val durationHours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(durationHours)

        val totalHours = if (remainingMinutes > 0) durationHours + 1 else durationHours
        val finalHoursToCharge = if (totalHours < 1) 1 else totalHours
        val totalPrice = finalHoursToCharge * pricePerHour

        binding.tvTotalPrice.text = "$totalPrice BDT"
        binding.btnConfirmBooking.isEnabled = true
    }

    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun confirmBooking() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to book.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spotId == null || startTimeMillis == null || endTimeMillis == null) {
            Toast.makeText(this, "Please select all booking details.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmBooking.isEnabled = false
        Toast.makeText(this, "Confirming booking...", Toast.LENGTH_SHORT).show()

        val bookingData = hashMapOf(
            "spotId" to spotId,
            "driverId" to currentUser.uid,
            "startTime" to Date(startTimeMillis!!),
            "endTime" to Date(endTimeMillis!!),
            "totalPrice" to binding.tvTotalPrice.text.toString(),
            "status" to "confirmed",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("bookings").add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmBooking.isEnabled = true
            }
    }
}