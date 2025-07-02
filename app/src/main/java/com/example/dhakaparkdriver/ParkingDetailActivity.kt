package com.example.dhakaparkdriver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.dhakaparkdriver.databinding.ActivityParkingDetailBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ParkingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParkingDetailBinding
    private val db = Firebase.firestore
    private var spotId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the Parking Spot ID passed from the adapter
        spotId = intent.getStringExtra("PARKING_SPOT_ID")

        if (spotId == null) {
            Toast.makeText(this, "Error: Parking spot not found.", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no ID is provided
            return
        }

        loadParkingDetails()

        binding.btnProceedToBooking.setOnClickListener {
            // TODO: Navigate to the actual BookingActivity
            Toast.makeText(this, "Booking functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadParkingDetails() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("parking_spots").document(spotId!!)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    // Populate all the UI elements with data from Firestore
                    binding.tvSpotName.text = document.getString("spotName") ?: "N/A"
                    binding.tvAddress.text = document.getString("address") ?: "Address not available"
                    binding.tvPrice.text = "Price: ${document.getLong("pricePerHour") ?: "N/A"} BDT/hr"

                    val availableSlots = document.getLong("availableSlots") ?: 0
                    val totalSlots = document.getLong("totalSlots") ?: 0
                    binding.tvAvailableSlots.text = "Available Slots: $availableSlots / $totalSlots"

                    binding.tvOperatingHours.text = "Hours: ${document.getString("operatingHours") ?: "N/A"}"

                    // Load the first image using Glide
                    val imageUrls = document.get("imageUrls") as? List<String>
                    if (!imageUrls.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrls[0]) // Load the first image
                            .centerCrop()
                            .into(binding.ivSpotImage)
                    }

                } else {
                    Toast.makeText(this, "Could not load parking details.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load details. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
}