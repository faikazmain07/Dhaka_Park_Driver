package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityDriverProfileSetupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DriverProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverProfileSetupBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line correctly inflates the layout and gives us access to all views
        binding = ActivityDriverProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Add logic for profile picture selection in a future step
        // binding.ibProfileImage.setOnClickListener { ... }

        binding.btnFinishSetup.setOnClickListener {
            updateDriverProfile()
        }
    }

    private fun updateDriverProfile() {
        // Get all data from the form fields using the binding object
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val licenseNumber = binding.etLicenseNumber.text.toString().trim()
        val licensePlate = binding.etLicensePlate.text.toString().trim()
        val carModel = binding.etCarModel.text.toString().trim()
        val carColor = binding.etCarColor.text.toString().trim()

        // Input Validation for all fields
        if (phoneNumber.isEmpty() || licenseNumber.isEmpty() || licensePlate.isEmpty() || carModel.isEmpty() || carColor.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Create a map of the new data to update in Firestore
        val driverDetails = mapOf(
            "phoneNumber" to phoneNumber,
            "licenseNumber" to licenseNumber,
            "vehicleInfo" to mapOf( // Storing vehicle info in a nested map
                "licensePlate" to licensePlate,
                "model" to carModel,
                "color" to carColor
            ),
            "profileStatus" to "approved" // Later, this would be "pending_verification"
        )

        // Find the user's document in the "users" collection and update it
        db.collection("users").document(currentUser.uid)
            .update(driverDetails)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                goToMapActivity()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}