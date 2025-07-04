package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dhakaparkdriver.databinding.ActivityOwnerDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private lateinit var ownedParkingSpotsAdapter: OwnedParkingSpotAdapter
    private val ownedParkingSpotsList = mutableListOf<ParkingSpot>()

    // Keep track of total slots for occupancy calculation
    private var totalOwnerSlots = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        displayOwnerName()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh all data whenever the activity comes to foreground
        fetchOwnedParkingSpots()
        calculateAndDisplaySummaryData()
    }

    private fun setupRecyclerView() {
        ownedParkingSpotsAdapter = OwnedParkingSpotAdapter(ownedParkingSpotsList)
        binding.rvOwnedParkingSpots.layoutManager = LinearLayoutManager(this)
        binding.rvOwnedParkingSpots.adapter = ownedParkingSpotsAdapter
    }

    private fun displayOwnerName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "Owner"
                        binding.tvOwnerName.text = fullName
                    } else {
                        binding.tvOwnerName.text = "Owner (Profile Missing)"
                    }
                }
                .addOnFailureListener {
                    binding.tvOwnerName.text = "Owner (Error fetching name)"
                }
        } else {
            binding.tvOwnerName.text = "Owner (Not Logged In)"
        }
    }

    private fun setupClickListeners() {
        binding.btnAddParkingSpot.setOnClickListener {
            val intent = Intent(this, AddParkingSpotActivity::class.java)
            startActivity(intent)
        }

        binding.btnOwnerLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
        // TODO: Add listeners for btnBookingsManagement, btnRevenuePayments, etc.
    }

    private fun fetchOwnedParkingSpots() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("OwnerDashboard", "No current user to fetch owned spots.")
            ownedParkingSpotsList.clear()
            ownedParkingSpotsAdapter.notifyDataSetChanged()
            return
        }

        db.collection("parking_spots")
            .whereEqualTo("ownerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                ownedParkingSpotsList.clear()
                totalOwnerSlots = 0 // Reset total slots
                if (documents.isEmpty) {
                    Log.d("OwnerDashboard", "No parking spots found for owner: ${currentUser.uid}")
                } else {
                    for (document in documents) {
                        val spotId = document.id
                        val name = document.getString("name") ?: "Unnamed Spot"
                        val totalSlotsDoc = document.getLong("totalSlots") ?: 0
                        val availableSlots = document.getLong("availableSlots") ?: 0
                        val pricePerHour = document.getLong("pricePerHour") ?: 0
                        val operatingHours = document.getString("operatingHours") ?: "N/A"
                        val location = document.getGeoPoint("location")

                        val spot = ParkingSpot(
                            id = spotId,
                            name = name,
                            totalSlots = totalSlotsDoc,
                            availableSlots = availableSlots,
                            pricePerHour = pricePerHour,
                            operatingHours = operatingHours,
                            location = location
                        )
                        ownedParkingSpotsList.add(spot)
                        totalOwnerSlots += totalSlotsDoc // Add to total slots
                    }
                    Log.d("OwnerDashboard", "Fetched ${ownedParkingSpotsList.size} spots for owner. Total slots: $totalOwnerSlots")
                }
                ownedParkingSpotsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error fetching owned parking spots", exception)
                Toast.makeText(this, "Error loading your spots: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateAndDisplaySummaryData() {
        val currentUser = auth.currentUser
        if (currentUser == null || ownedParkingSpotsList.isEmpty()) {
            binding.tvTodayEarnings.text = "0.00 BDT"
            binding.tvCurrentOccupancy.text = "0%"
            return
        }

        // Get a list of IDs of all parking spots owned by the current user
        val ownedSpotIds = ownedParkingSpotsList.map { it.id }

        if (ownedSpotIds.isEmpty()) {
            binding.tvTodayEarnings.text = "0.00 BDT"
            binding.tvCurrentOccupancy.text = "0%"
            return
        }

        // Calculate Today's Earnings
        calculateTodayEarnings(ownedSpotIds)

        // Calculate Current Occupancy
        calculateCurrentOccupancy(ownedSpotIds)
    }

    private fun calculateTodayEarnings(ownedSpotIds: List<String>) {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val startOfDayMillis = today.timeInMillis

        db.collection("bookings")
            .whereIn("spotId", ownedSpotIds) // Filter bookings for owner's spots
            .whereEqualTo("sessionStatus", "ended") // Only count completed sessions
            .whereGreaterThanOrEqualTo("actualEndTime", startOfDayMillis) // Bookings ended today
            .get()
            .addOnSuccessListener { documents ->
                var totalEarnings = 0.0
                for (doc in documents) {
                    val totalPriceString = doc.getString("totalPrice") ?: "0 BDT"
                    val priceParts = totalPriceString.split(" ")[0] // Get "300" from "300 BDT"
                    totalEarnings += priceParts.toDoubleOrNull() ?: 0.0 // Convert to double
                }
                val df = DecimalFormat("#.00")
                binding.tvTodayEarnings.text = "${df.format(totalEarnings)} BDT"
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error calculating earnings", exception)
                binding.tvTodayEarnings.text = "Error"
            }
    }

    private fun calculateCurrentOccupancy(ownedSpotIds: List<String>) {
        val currentTimeMillis = System.currentTimeMillis()

        db.collection("bookings")
            .whereIn("spotId", ownedSpotIds)
            .whereEqualTo("sessionStatus", "active") // Only count active sessions
            .get()
            .addOnSuccessListener { documents ->
                val activeBookingsCount = documents.size()
                Log.d("OwnerDashboard", "Active bookings: $activeBookingsCount")
                Log.d("OwnerDashboard", "Total owner slots: $totalOwnerSlots")

                if (totalOwnerSlots > 0) {
                    val occupancyPercentage = (activeBookingsCount.toDouble() / totalOwnerSlots.toDouble()) * 100
                    val df = DecimalFormat("#.##")
                    binding.tvCurrentOccupancy.text = "${df.format(occupancyPercentage)}%"
                } else {
                    binding.tvCurrentOccupancy.text = "0%" // No slots or no active bookings
                }
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error calculating occupancy", exception)
                binding.tvCurrentOccupancy.text = "Error"
            }
    }
}