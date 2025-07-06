package com.example.dhakaparkdriver

import android.content.Intent
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
import kotlin.collections.ArrayList

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private var spotId: String? = null
    private var pricePerHour: Long = 0
    private var totalSlots: Long = 0
    private var startTimeMillis: Long? = null
    private var endTimeMillis: Long? = null

    // NEW: BookingActivity will now receive these as well
    private var parkingType: String = ""
    private var vehicleTypes: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve all data passed from the previous activity
        spotId = intent.getStringExtra("SPOT_ID")
        val spotName = intent.getStringExtra("SPOT_NAME") ?: "Unknown Spot"
        pricePerHour = intent.getLongExtra("SPOT_PRICE_PER_HOUR", 0)
        totalSlots = intent.getLongExtra("SPOT_TOTAL_SLOTS", 0) // Changed to total slots
        val availableSlots = intent.getLongExtra("SPOT_AVAILABLE_SLOTS", 0) // Still pass available

        // NEW: Retrieve new data
        parkingType = intent.getStringExtra("SPOT_PARKING_TYPE") ?: "N/A"
        vehicleTypes = intent.getStringArrayListExtra("SPOT_VEHICLE_TYPES") ?: listOf()


        // Update the UI with the initial spot information
        binding.tvSpotName.text = spotName
        binding.tvTotalSlots.text = "Total Parking Slots: $totalSlots" // Updated to totalSlots
        binding.tvAvailableSlots.text = "Available Slots: $availableSlots" // Display available slots separately

        // TODO: Display parkingType and vehicleTypes in BookingActivity UI if desired.
        // Example: Toast.makeText(this, "Parking Type: $parkingType, Vehicles: ${vehicleTypes.joinToString()}", Toast.LENGTH_LONG).show()

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

            if (isStartTime) {
                startTimeMillis = selectedTimeMillis
                binding.tvStartTime.text = formatTime(startTimeMillis!!)
            } else {
                endTimeMillis = selectedTimeMillis
                binding.tvEndTime.text = formatTime(endTimeMillis!!)
            }

            if (startTimeMillis != null && endTimeMillis != null) {
                checkAvailability()
            }
        }
    }

    private fun checkAvailability() {
        if (startTimeMillis == null || endTimeMillis == null || spotId == null) return

        if (endTimeMillis!! <= startTimeMillis!!) {
            Toast.makeText(this, "End time must be after start time.", Toast.LENGTH_SHORT).show()
            binding.btnConfirmBooking.isEnabled = false
            return
        }

        binding.tvTimeAvailability.text = "Checking availability..."
        binding.btnConfirmBooking.isEnabled = false

        db.collection("bookings")
            .whereEqualTo("spotId", spotId)
            .get()
            .addOnSuccessListener { documents ->
                var conflictingBookings = 0
                for (doc in documents) {
                    val bookingStartTimeMs = doc.getLong("startTimeMillis")
                    val bookingEndTimeMs = doc.getLong("endTimeMillis")

                    if (bookingStartTimeMs != null && bookingEndTimeMs != null) {
                        if (startTimeMillis!! < bookingEndTimeMs && endTimeMillis!! > bookingStartTimeMs) {
                            conflictingBookings++
                        }
                    }
                }

                val availableNow = totalSlots - conflictingBookings

                if (availableNow > 0) {
                    binding.tvTimeAvailability.text = "$availableNow slots available for this time"
                    calculatePrice()
                } else {
                    binding.tvTimeAvailability.text = "No slots available for this time"
                    binding.tvTotalPrice.text = "-- BDT"
                    binding.btnConfirmBooking.isEnabled = false
                }
            }
            .addOnFailureListener { exception ->
                binding.tvTimeAvailability.text = "Could not check availability."
                Log.e("BookingActivity", "Error checking availability", exception)
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculatePrice() {
        if (pricePerHour <= 0) {
            binding.tvTotalPrice.text = "Price not available"
            binding.btnConfirmBooking.isEnabled = false
            return
        }

        val durationMillis = endTimeMillis!! - startTimeMillis!!
        val durationHours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

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
        val currentUser = auth.currentUser ?: return
        if (spotId == null || startTimeMillis == null || endTimeMillis == null) {
            Toast.makeText(this, "Please select all booking details.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmBooking.isEnabled = false
        Toast.makeText(this, "Confirming booking...", Toast.LENGTH_SHORT).show()

        val newBooking = Booking(
            spotId = spotId!!,
            driverId = currentUser.uid,
            startTimeMillis = startTimeMillis!!,
            endTimeMillis = endTimeMillis!!,
            totalPrice = binding.tvTotalPrice.text.toString(),
            status = "confirmed",
        )

        db.collection("bookings").add(newBooking)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_LONG).show()

                val intent = Intent(this, BookingSuccessActivity::class.java).apply {
                    putExtra("BOOKING_ID", documentReference.id)
                    putExtra("SPOT_NAME", binding.tvSpotName.text.toString())
                    putExtra("START_TIME", binding.tvStartTime.text.toString())
                    putExtra("END_TIME", binding.tvEndTime.text.toString())
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmBooking.isEnabled = true
            }
    }
}