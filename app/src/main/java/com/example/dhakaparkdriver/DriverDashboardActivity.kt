package com.example.dhakaparkdriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dhakaparkdriver.databinding.ActivityDriverDashboardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DriverDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = Firebase.firestore

    // --- NEW: Variables for the RecyclerView ---
    private lateinit var parkingListAdapter: ParkingListAdapter
    private val parkingSpotList = mutableListOf<ParkingSpot>()
    // ---

    private val parkingSpotLocations = mutableListOf<LatLng>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showUserLocation()
        } else {
            Toast.makeText(this, "Location permission is required to show your position.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- ADDED: Setup for the RecyclerView ---
        parkingListAdapter = ParkingListAdapter(parkingSpotList, this) // Pass context
        binding.rvParkingSpots.layoutManager = LinearLayoutManager(this)
        binding.rvParkingSpots.adapter = parkingListAdapter
        // ---

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment_container) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                showUserLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchAndDisplayParkingSpots(location)
            } else {
                Toast.makeText(this, "Could not get your location. Please ensure location is enabled.", Toast.LENGTH_SHORT).show()
                fetchAndDisplayParkingSpots(null)
            }
        }
    }

    private fun fetchAndDisplayParkingSpots(userLocation: android.location.Location?) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("parking_spots")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                parkingSpotLocations.clear()
                parkingSpotList.clear() // Clear the list for the adapter
                mMap.clear()

                for (document in documents) {
                    val spot = ParkingSpot(
                        id = document.id,
                        spotName = document.getString("spotName"),
                        location = document.getGeoPoint("location"),
                        pricePerHour = document.getLong("pricePerHour")
                    )

                    if (userLocation != null && spot.location != null) {
                        val spotLocation = android.location.Location("").apply {
                            latitude = spot.location!!.latitude
                            longitude = spot.location!!.longitude
                        }
                        spot.distance = userLocation.distanceTo(spotLocation)
                    }

                    parkingSpotList.add(spot)

                    // Marker adding logic (no changes here)
                    val location = document.getGeoPoint("location")
                    val availableSlots = document.getLong("availableSlots") ?: 0
                    val operatingHours = document.getString("operatingHours") ?: "N/A"
                    val price = document.getLong("pricePerHour") ?: 0
                    val snippet = "Available Slots: $availableSlots\n" +
                            "Hours: $operatingHours\n" +
                            "Price: $price BDT/hr"
                    if (location != null) {
                        val spotLatLng = LatLng(location.latitude, location.longitude)
                        parkingSpotLocations.add(spotLatLng)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(spotLatLng)
                                .title(spot.spotName)
                                .snippet(snippet)
                                .icon(bitmapDescriptorFromVector(R.drawable.ic_parking_marker))
                        )
                    }
                }

                parkingSpotList.sortBy { it.distance } // Sort by distance
                parkingListAdapter.notifyDataSetChanged() // Refresh the list
                binding.progressBar.visibility = View.GONE

                zoomCameraToFit(userLocation)
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error getting spots: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun zoomCameraToFit(userLocation: android.location.Location?) {
        if (parkingSpotLocations.isEmpty() && userLocation == null) return
        if (parkingSpotLocations.isEmpty() && userLocation != null) {
            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            return
        }
        val boundsBuilder = LatLngBounds.Builder()
        for (spotLocation in parkingSpotLocations) {
            boundsBuilder.include(spotLocation)
        }
        if (userLocation != null) {
            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            boundsBuilder.include(userLatLng)
        }
        val bounds = boundsBuilder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(this, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}