package com.example.dhakaparkdriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dhakaparkdriver.databinding.ActivityMapBinding
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

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    // --- Class Variables ---
    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = Firebase.firestore
    private val parkingSpotLocations = mutableListOf<LatLng>() // List to hold spot locations for camera bounds

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showUserLocation()
        } else {
            Toast.makeText(this, "Location permission is required to show your position.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Lifecycle Functions ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupDriverStatusSwitch()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
        fetchAndDisplayParkingSpots()
    }


    // --- Helper Functions ---

    private fun fetchAndDisplayParkingSpots() {
        db.collection("parking_spots")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                // Clear the old list before adding new spots
                parkingSpotLocations.clear()

                for (document in documents) {
                    val name = document.getString("name") ?: "Unnamed Spot"
                    val location = document.getGeoPoint("location")

                    if (location != null) {
                        val spotLatLng = LatLng(location.latitude, location.longitude)
                        parkingSpotLocations.add(spotLatLng) // Add to our list for camera zooming

                        mMap.addMarker(
                            MarkerOptions()
                                .position(spotLatLng)
                                .title(name)
                                .icon(bitmapDescriptorFromVector(R.drawable.ic_parking_marker))
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting parking spots: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                showUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                val userLatLng = LatLng(location.latitude, location.longitude)

                // If there are no parking spots, just zoom to the user's location
                if (parkingSpotLocations.isEmpty()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    return@addOnSuccessListener
                }

                // --- CAMERA ZOOM LOGIC TO INCLUDE ALL POINTS ---
                // Create a bounds builder that includes the user's location
                val boundsBuilder = LatLngBounds.Builder().include(userLatLng)

                // Add all the parking spot locations to the bounds
                for (spotLocation in parkingSpotLocations) {
                    boundsBuilder.include(spotLocation)
                }

                // Build the bounds
                val bounds = boundsBuilder.build()

                // Animate the camera to show all the points, with 150 pixels of padding
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))

            } else {
                Toast.makeText(this, "Could not get your location. Please ensure location is enabled on your device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDriverStatusSwitch() {
        binding.tvDriverStatus.text = "You are Offline"
        binding.switchOnline.text = "Go Online"

        binding.switchOnline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvDriverStatus.text = "You are Online"
                binding.switchOnline.text = "Go Offline"
                Toast.makeText(this, "You are now ONLINE.", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvDriverStatus.text = "You are Offline"
                binding.switchOnline.text = "Go Online"
                Toast.makeText(this, "You are now OFFLINE.", Toast.LENGTH_SHORT).show()
            }
        }
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