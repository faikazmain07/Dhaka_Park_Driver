package com.example.dhakaparkdriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.LatLng

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    // --- Class Variables ---
    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // This is the modern way to ask for permissions.
    // It launches the permission dialog and defines a callback for the user's response.
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // If user grants permission, show their location on the map.
            showUserLocation()
        } else {
            // If user denies permission, show a message explaining why it's needed.
            Toast.makeText(this, "Location permission is required to show your position.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Lifecycle Functions ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up ViewBinding to easily access UI elements from the XML layout.
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the location client.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Find the map fragment from our XML layout and prepare it.
        // This is an asynchronous call; the result is delivered to onMapReady().
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment // Make sure ID matches XML
        mapFragment.getMapAsync(this)

        // Set up the listener for the Online/Offline switch.
        setupDriverStatusSwitch()
    }

    /**
     * This required callback is triggered automatically when the Google Map is fully loaded
     * and ready to be used. This is the main entry point for map interaction.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Now that the map is ready, we can check for location permission.
        checkLocationPermission()
    }


    // --- Helper Functions ---

    /**
     * Checks if the app already has location permission.
     * If yes, it calls showUserLocation().
     * If no, it launches the permission request dialog.
     */
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, proceed to show the user's location.
                showUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Optionally, show a dialog explaining why you need the permission
                // before asking again. For now, we'll just ask directly.
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Permission has not been granted, so ask for it.
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Enables the "My Location" blue dot and button on the map,
     * fetches the last known location, and moves the camera to it.
     * NOTE: This is marked @SuppressLint because we are safely checking for permission
     * in the checkLocationPermission() function before this is ever called.
     */
    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        mMap.isMyLocationEnabled = true // Shows the blue "my location" dot.
        mMap.uiSettings.isMyLocationButtonEnabled = true // Shows the "center on me" button.

        // Get the last known location of the device. This is often faster than requesting a fresh one.
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            // The location can be null if the device location has been turned off.
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                // Move the map's camera to the user's location with a standard zoom level.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } else {
                Toast.makeText(this, "Could not get your location. Please ensure location is enabled on your device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets up the logic for the Online/Offline switch and status text.
     */
    private fun setupDriverStatusSwitch() {
        // Set initial state from the switch's default value (e.g., 'off')
        binding.tvDriverStatus.text = "You are Offline"
        binding.switchOnline.text = "Go Online"

        // Listen for changes in the switch's state
        binding.switchOnline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // When the switch is turned ON
                binding.tvDriverStatus.text = "You are Online"
                binding.switchOnline.text = "Go Offline"
                // TODO: Add logic here to notify your backend that the driver is online
                Toast.makeText(this, "You are now ONLINE.", Toast.LENGTH_SHORT).show()
            } else {
                // When the switch is turned OFF
                binding.tvDriverStatus.text = "You are Offline"
                binding.switchOnline.text = "Go Online"
                // TODO: Add logic here to notify your backend that the driver is offline
                Toast.makeText(this, "You are now OFFLINE.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}