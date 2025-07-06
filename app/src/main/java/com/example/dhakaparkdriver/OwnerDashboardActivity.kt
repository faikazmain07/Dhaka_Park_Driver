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

    private var totalOwnerSlots = 0L // Keep track of total slots for occupancy calculation
    private var occupiedOwnerSlots = 0L // Keep track of currently occupied slots

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
        Log.d("OwnerDashboard", "OwnerDashboardActivity onResume - Refreshing data.")
        fetchOwnedParkingSpots() // This will also trigger summary data calculation
    }

    private fun setupRecyclerView() {
        // We'll pass a click listener to the adapter (for future detail/edit)
        ownedParkingSpotsAdapter = OwnedParkingSpotAdapter(ownedParkingSpotsList) { spot ->
            // TODO: In a future step, navigate to a detailed view for this specific parking spot
            Toast.makeText(this, "Managing spot: ${spot.name}", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, OwnerBookingManagementActivity::class.java).apply {
                putExtra("SELECTED_SPOT_ID", spot.id) // Pass the selected spot's ID
                putExtra("SELECTED_SPOT_NAME", spot.name) // Pass the selected spot's name
                // Add any other spot data needed for OwnerBookingDetailActivity
            }
            startActivity(intent)
        }
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
                        Log.d("OwnerDashboard", "Fetched owner name: $fullName")
                    } else {
                        binding.tvOwnerName.text = "Owner (Profile Missing)"
                        Log.w("OwnerDashboard", "Owner profile document not found for UID: ${currentUser.uid}")
                    }
                }
                .addOnFailureListener {
                    binding.tvOwnerName.text = "Owner (Error fetching name)"
                    Log.e("OwnerDashboard", "Error fetching owner name", it)
                }
        } else {
            binding.tvOwnerName.text = "Owner (Not Logged In)"
            Log.w("OwnerDashboard", "No current user for owner name display.")
        }
    }

    private fun setupClickListeners() {
        binding.btnAddParkingSpot.setOnClickListener {
            val intent = Intent(this, AddParkingSpotActivity::class.java)
            startActivity(intent)
        }

        binding.btnBookingsManagement.setOnClickListener {
            val intent = Intent(this, OwnerBookingManagementActivity::class.java)
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
        // TODO: Add listeners for btnRevenuePayments, etc.
    }

    private fun fetchOwnedParkingSpots() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("OwnerDashboard", "No current user to fetch owned spots. Clearing lists.")
            ownedParkingSpotsList.clear()
            ownedParkingSpotsAdapter.notifyDataSetChanged()
            binding.tvCurrentOccupancy.text = "0%"
            binding.tvTodayEarnings.text = "0.00 BDT"
            return
        }

        Log.d("OwnerDashboard", "Fetching parking spots for owner: ${currentUser.uid}")
        db.collection("parking_spots")
            .whereEqualTo("ownerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                ownedParkingSpotsList.clear()
                totalOwnerSlots = 0 // Reset total slots for new calculation
                val ownerSpotIds = mutableListOf<String>()

                if (documents.isEmpty) {
                    Log.d("OwnerDashboard", "No parking spots found for owner: ${currentUser.uid}")
                } else {
                    Log.d("OwnerDashboard", "Found ${documents.size()} parking spots for owner.")
                    for (document in documents) {
                        val spotId = document.id
                        val name = document.getString("name") ?: "Unnamed Spot"
                        val totalSlotsDoc = document.getLong("totalSlots") ?: 0
                        val availableSlots = document.getLong("availableSlots") ?: 0
                        val pricePerHour = document.getLong("pricePerHour") ?: 0
                        val operatingHoursStartMillis = document.getLong("operatingHoursStartMillis") ?: 0
                        val operatingHoursEndMillis = document.getLong("operatingHoursEndMillis") ?: 0
                        val parkingType = document.getString("parkingType") ?: "covered"
                        val emergencyContact = document.getString("emergencyContact") ?: ""
                        val vehicleTypes = document.get("vehicleTypes") as? List<String> ?: listOf()
                        val photoUrl = document.getString("photoUrl")
                        val location = document.getGeoPoint("location")

                        Log.d("OwnerDashboard", "Spot ID: $spotId, Name: $name, TotalSlots: $totalSlotsDoc, Price: $pricePerHour")

                        val spot = ParkingSpot(
                            id = spotId,
                            name = name,
                            totalSlots = totalSlotsDoc,
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
                        totalOwnerSlots += totalSlotsDoc
                        ownerSpotIds.add(spotId)
                    }
                }
                ownedParkingSpotsAdapter.notifyDataSetChanged()
                Log.d("OwnerDashboard", "Owned spots list updated. Total owner slots: $totalOwnerSlots")

                calculateAndDisplaySummaryData(ownerSpotIds)

            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error fetching owned parking spots", exception)
                Toast.makeText(this, "Error loading your spots: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateAndDisplaySummaryData(ownedSpotIds: List<String>) {
        Log.d("OwnerDashboard", "Calculating summary data for spot IDs: $ownedSpotIds")
        if (ownedSpotIds.isEmpty()) {
            binding.tvTodayEarnings.text = "0.00 BDT"
            binding.tvCurrentOccupancy.text = "0%"
            Log.d("OwnerDashboard", "No owned spot IDs. Earnings/Occupancy set to 0.")
            return
        }

        calculateTodayEarnings(ownedSpotIds)
        calculateCurrentOccupancy(ownedSpotIds)
    }

    private fun calculateTodayEarnings(ownedSpotIds: List<String>) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        Log.d("OwnerDashboard", "Calculating earnings for spot IDs: $ownedSpotIds from actualEndTime >= $todayStart")

        db.collection("bookings")
            .whereIn("spotId", ownedSpotIds)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("actualEndTime", todayStart)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("OwnerDashboard", "Earnings query successful. Found ${documents.size()} completed bookings.")
                var totalEarnings = 0.0
                for (doc in documents) {
                    val totalPriceString = doc.getString("totalPrice") ?: "0 BDT"
                    val priceParts = totalPriceString.split(" ")[0]
                    val bookingAmount = priceParts.toDoubleOrNull() ?: 0.0
                    totalEarnings += bookingAmount
                    Log.d("OwnerDashboard", "Processing completed booking ${doc.id}: Price $totalPriceString, Amount $bookingAmount. Running total: $totalEarnings")
                }
                val df = DecimalFormat("#.00")
                binding.tvTodayEarnings.text = "${df.format(totalEarnings)} BDT"
                Log.d("OwnerDashboard", "Today's Earnings: ${df.format(totalEarnings)} BDT")
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error calculating earnings", exception)
                binding.tvTodayEarnings.text = "Error"
            }
    }

    private fun calculateCurrentOccupancy(ownedSpotIds: List<String>) {
        Log.d("OwnerDashboard", "Calculating occupancy for spot IDs: $ownedSpotIds")

        db.collection("bookings")
            .whereIn("spotId", ownedSpotIds)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                val activeBookingsCount = documents.size()
                Log.d("OwnerDashboard", "Occupancy query successful. Found ${activeBookingsCount} active bookings.")
                Log.d("OwnerDashboard", "Total owner slots: $totalOwnerSlots")

                if (totalOwnerSlots > 0) {
                    val occupancyPercentage = (activeBookingsCount.toDouble() / totalOwnerSlots.toDouble()) * 100
                    val df = DecimalFormat("#.##")
                    binding.tvCurrentOccupancy.text = "${df.format(occupancyPercentage)}%"
                    Log.d("OwnerDashboard", "Current Occupancy: ${df.format(occupancyPercentage)}%")
                } else {
                    binding.tvCurrentOccupancy.text = "0%"
                    Log.d("OwnerDashboard", "Current Occupancy: 0% (No total slots)")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("OwnerDashboard", "Error calculating occupancy", exception)
                binding.tvCurrentOccupancy.text = "Error"
            }
    }
}