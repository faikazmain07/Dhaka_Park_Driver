package com.example.dhakaparkdriver

import android.os.Bundle
import android.view.View
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
        totalSlots = intent.getLongExtra("SPOT_AVAILABLE_SLOTS", 0) // We use this as total slots now

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

            if (isStartTime) {
                startTimeMillis = calendar.timeInMillis
                binding.tvStartTime.text = formatTime(startTimeMillis!!)
            } else {
                endTimeMillis = calendar.timeInMillis
                binding.tvEndTime.text = formatTime(endTimeMillis!!)
            }

            // After any time is picked, check availability and calculate price
            if (startTimeMillis != null && endTimeMillis != null) {
                checkAvailability()
            }
        }
    }

    private fun checkAvailability() {
        if (startTimeMillis == null || endTimeMillis == null || spotId == null) return

        if (endTimeMillis!! <= startTimeMillis!!) {
            Toast.makeText(this, "End time must be after start time.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.tvTimeAvailability.text = "Checking availability..."
        binding.btnConfirmBooking.isEnabled = false

        val userStartTime = Date(startTimeMillis!!)
        val userEndTime = Date(endTimeMillis!!)

        // Query for all bookings for this specific spot
        db.collection("bookings")
            .whereEqualTo("spotId", spotId)
            .get()
            .addOnSuccessListener { documents ->
                var conflictingBookings = 0
                for (doc in documents) {
                    val bookingStartTime = doc.getDate("startTime")
                    val bookingEndTime = doc.getDate("endTime")

                    // Check for time overlap
                    if (bookingStartTime != null && bookingEndTime != null) {
                        if (userStartTime.before(bookingEndTime) && userEndTime.after(bookingStartTime)) {
                            conflictingBookings++
                        }
                    }
                }

                val availableNow = totalSlots - conflictingBookings
                binding.tvTimeAvailability.text = "$availableNow slots available for this time"

                if (availableNow > 0) {
                    calculatePrice() // Calculate price only if slots are available
                } else {
                    binding.tvTimeAvailability.text = "No slots available for this time"
                    binding.tvTotalPrice.text = "-- BDT"
                    binding.btnConfirmBooking.isEnabled = false
                }
            }
            .addOnFailureListener {
                binding.tvTimeAvailability.text = "Could not check availability."
                Toast.makeText(this, "Error checking availability.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculatePrice() {
        // This function is now simpler as it's only called when a slot is available
        val durationMillis = endTimeMillis!! - startTimeMillis!!
        val durationHours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(durationHours)

        val totalHours = if (remainingMinutes > 0) durationHours + 1 else durationHours
        val finalHoursToCharge = if (totalHours < 1) 1 else totalHours
        val totalPrice = finalHoursToCharge * pricePerHour

        binding.tvTotalPrice.text = "$totalPrice BDT"
        binding.btnConfirmBooking.isEnabled = true
    }

    // confirmBooking() and formatTime() functions remain the same as before
    // ...
    private fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun confirmBooking() {
        // ... (The confirmBooking function from the previous step goes here, no changes needed)
    }
}