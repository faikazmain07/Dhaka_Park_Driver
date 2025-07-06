package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dhakaparkdriver.databinding.ActivityOwnerBookingManagementBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class OwnerBookingManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBookingManagementBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private lateinit var ownedParkingSpotsAdapter: OwnedParkingSpotAdapter // Reusing the existing adapter
    private val ownedParkingSpotsList = mutableListOf<ParkingSpot>() // List to display owner's spots

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBookingManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list of parking spots whenever this activity comes to foreground
        fetchOwnedParkingSpots()
    }

    private fun setupRecyclerView() {
        // We'll pass a click listener to the adapter
        ownedParkingSpotsAdapter = OwnedParkingSpotAdapter(ownedParkingSpotsList) { spot ->
            // When an owned spot is clicked, launch its detail/management activity
            launchOwnerBookingDetail(spot)
        }
        binding.rvParkingSpotsToManage.layoutManager = LinearLayoutManager(this)
        binding.rvParkingSpotsToManage.adapter = ownedParkingSpotsAdapter
    }

    private fun fetchOwnedParkingSpots() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("OwnerBookingMgmt", "No current user to fetch owned spots for management.")
            ownedParkingSpotsList.clear()
            ownedParkingSpotsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Please log in to manage bookings.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        db.collection("parking_spots")
            .whereEqualTo("ownerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                ownedParkingSpotsList.clear()
                if (documents.isEmpty) {
                    Log.d("OwnerBookingMgmt", "No parking spots found for owner: ${currentUser.uid}")
                    Toast.makeText(this, "You have no parking spots to manage.", Toast.LENGTH_LONG).show()
                } else {
                    for (document in documents) {
                        val spotId = document.id
                        val name = document.getString("name") ?: "Unnamed Spot"
                        val totalSlots = document.getLong("totalSlots") ?: 0
                        val availableSlots = document.getLong("availableSlots") ?: 0
                        val pricePerHour = document.getLong("pricePerHour") ?: 0
                        val operatingHoursStartMillis = document.getLong("operatingHoursStartMillis") ?: 0
                        val operatingHoursEndMillis = document.getLong("operatingHoursEndMillis") ?: 0
                        val parkingType = document.getString("parkingType") ?: "covered"
                        val emergencyContact = document.getString("emergencyContact") ?: ""
                        val vehicleTypes = document.get("vehicleTypes") as? List<String> ?: listOf()
                        val photoUrl = document.getString("photoUrl")
                        val location = document.getGeoPoint("location")

                        val spot = ParkingSpot(
                            id = spotId,
                            name = name,
                            totalSlots = totalSlots,
                            availableSlots = availableSlots,
                            pricePerHour = pricePerHour,
                            operatingHoursStartMillis = operatingHoursStartMillis,
                            operatingHoursEndMillis = operatingHoursEndMillis,
                            parkingType = parkingType,
                            emergencyContact = emergencyContact,
                            vehicleTypes = vehicleTypes,
                            photoUrl = photoUrl,
                            location = location
                        )
                        ownedParkingSpotsList.add(spot)
                    }
                    Log.d("OwnerBookingMgmt", "Fetched ${ownedParkingSpotsList.size} spots for management.")
                }
                ownedParkingSpotsAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerBookingMgmt", "Error fetching owned parking spots for management", exception)
                Toast.makeText(this, "Error loading your spots: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    // Helper to launch the detail activity for a specific parking spot
    private fun launchOwnerBookingDetail(spot: ParkingSpot) {
        // TODO: Create OwnerBookingDetailActivity and pass spot data
        Toast.makeText(this, "Managing bookings for ${spot.name}", Toast.LENGTH_SHORT).show()
        // Example:
        // val intent = Intent(this, OwnerBookingDetailActivity::class.java).apply {
        //     putExtra("SPOT_ID", spot.id)
        //     putExtra("SPOT_NAME", spot.name)
        //     // Pass any other necessary spot details
        // }
        // startActivity(intent)
    }
}