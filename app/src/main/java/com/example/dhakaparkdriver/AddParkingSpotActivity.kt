package com.example.dhakaparkdriver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dhakaparkdriver.databinding.ActivityAddParkingSpotBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.* // For Locale

class AddParkingSpotActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddParkingSpotBinding
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null // To store the selected point on the map
    private var locationMarker: Marker? = null // To hold the marker for the selected location

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddParkingSpotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the map fragment for location selection
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_selector) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnAddSpot.setOnClickListener {
            addParkingSpot()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Set a default view for the map (e.g., center of Dhaka)
        val defaultLocation = LatLng(23.777176, 90.399452) // Center of Dhaka, Bangladesh
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        mMap.setOnMapClickListener { latLng ->
            // When map is tapped, place/move a marker and update the selected location
            selectedLocation = latLng
            updateLocationMarker(latLng)
            // Display coordinates in the TextView, formatted to 4 decimal places
            binding.tvSelectedLocation.text = "Lat: %.4f, Lng: %.4f".format(Locale.getDefault(), latLng.latitude, latLng.longitude)
            Log.d("AddParkingSpotActivity", "Selected location: $latLng")
        }
    }

    private fun updateLocationMarker(latLng: LatLng) {
        locationMarker?.remove() // Remove any existing marker
        locationMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
                .icon(bitmapDescriptorFromVector(R.drawable.ic_parking_marker)) // Use your parking marker icon
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) // Zoom to selected point for better view
    }

    private fun addParkingSpot() {
        val spotName = binding.etSpotName.text.toString().trim()
        val totalSlotsStr = binding.etTotalSlots.text.toString().trim()
        val pricePerHourStr = binding.etPricePerHour.text.toString().trim()
        val operatingHours = binding.etOperatingHours.text.toString().trim()

        // Input Validation
        var isValid = true
        // Check if name is empty
        if (spotName.isEmpty()) {
            binding.tilSpotName.error = "Parking spot name is required"; isValid = false
        } else { binding.tilSpotName.error = null } // Clear error

        // Validate total slots
        val totalSlots = totalSlotsStr.toLongOrNull()
        if (totalSlots == null || totalSlots <= 0) {
            binding.tilTotalSlots.error = "Please enter a valid number of slots (>0)"; isValid = false
        } else { binding.tilTotalSlots.error = null }

        // Validate price per hour
        val pricePerHour = pricePerHourStr.toFloatOrNull()
        if (pricePerHour == null || pricePerHour <= 0) {
            binding.tilPricePerHour.error = "Please enter a valid price per hour (>0)"; isValid = false
        } else { binding.tilPricePerHour.error = null }

        // Validate operating hours
        if (operatingHours.isEmpty()) {
            binding.tilOperatingHours.error = "Operating hours are required"; isValid = false
        } else { binding.tilOperatingHours.error = null }

        // Validate that a location has been selected on the map
        if (selectedLocation == null) {
            Toast.makeText(this, "Please tap on the map to select the parking spot's location.", Toast.LENGTH_LONG).show()
            isValid = false
        }

        // If any validation fails, stop here
        if (!isValid) return

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Owner not logged in. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show progress/toast while processing
        binding.btnAddSpot.isEnabled = false
        Toast.makeText(this, "Adding parking spot...", Toast.LENGTH_SHORT).show()

        // Create the data map for Firestore
        val parkingSpotData = hashMapOf(
            "ownerId" to currentUser.uid, // Link the spot to the owner
            "name" to spotName,
            "totalSlots" to totalSlots,
            "availableSlots" to totalSlots, // Initially, all slots are available
            "pricePerHour" to pricePerHour, // Save as Float or Long as per Firestore model
            "operatingHours" to operatingHours,
            "location" to GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude), // Convert LatLng to Firestore GeoPoint
            "isAvailable" to true, // By default, a new spot is available
            "createdAt" to System.currentTimeMillis() // Timestamp of creation
        )

        // Save the data to the "parking_spots" collection in Firestore
        db.collection("parking_spots")
            .add(parkingSpotData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Parking spot added successfully!", Toast.LENGTH_LONG).show()
                Log.d("AddParkingSpotActivity", "Spot added with ID: ${documentReference.id}")
                finish() // Go back to OwnerDashboardActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add spot: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AddParkingSpotActivity", "Error adding parking spot to Firestore", e)
                binding.btnAddSpot.isEnabled = true // Re-enable button on failure
            }
    }

    // Helper function (copied from DriverDashboardActivity) for custom marker icons
    // This function can also be placed in a utility class if used by many activities.
    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(this, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}