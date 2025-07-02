package com.example.dhakaparkdriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
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
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DriverDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = Firebase.firestore
    private val markerMap = HashMap<Marker, ParkingSpot>()

    private lateinit var parkingListAdapter: ParkingListAdapter
    private val parkingSpotList = mutableListOf<ParkingSpot>()

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showUserLocation()
        } else {
            Toast.makeText(this, "Location permission is required to see nearby spots.", Toast.LENGTH_LONG).show()
            fetchAndDisplayParkingSpots(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_container) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupRecyclerView() {
        parkingListAdapter = ParkingListAdapter(parkingSpotList) { spot ->
            launchBookingActivity(spot)
        }
        binding.rvParkingSpots.layoutManager = LinearLayoutManager(this)
        binding.rvParkingSpots.adapter = parkingListAdapter
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Set the custom info window adapter to show details on marker tap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        // Set the listener for when a user clicks on the info window itself
        mMap.setOnInfoWindowClickListener { marker ->
            val spot = markerMap[marker]
            if (spot != null) {
                launchBookingActivity(spot)
            }
        }
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            showUserLocation()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            fetchAndDisplayParkingSpots(location)
        }
    }

    private fun fetchAndDisplayParkingSpots(userLocation: Location?) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("parking_spots").whereEqualTo("isAvailable", true).get()
            .addOnSuccessListener { documents ->
                mMap.clear()
                markerMap.clear()
                parkingSpotList.clear()

                for (document in documents) {
                    val name = document.getString("name") ?: "N/A"
                    val locationData = document.getGeoPoint("location")
                    val availableSlots = document.getLong("availableSlots") ?: 0
                    val price = document.getLong("pricePerHour") ?: 0
                    val operatingHours = document.getString("operatingHours") ?: "N/A"

                    val parkingSpot = ParkingSpot(
                        id = document.id,
                        name = name,
                        availableSlots = availableSlots,
                        pricePerHour = price
                    )

                    if (userLocation != null && locationData != null) {
                        val spotLocation = Location("").apply {
                            latitude = locationData.latitude
                            longitude = locationData.longitude
                        }
                        parkingSpot.distance = userLocation.distanceTo(spotLocation)
                    }
                    parkingSpotList.add(parkingSpot)

                    if (locationData != null) {
                        val spotLatLng = LatLng(locationData.latitude, locationData.longitude)
                        val snippet = "Available: $availableSlots | ${price} BDT/hr\nTap to book now!" // Simple snippet for the info window

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(spotLatLng)
                                .title(name)
                                .snippet(snippet)
                                .icon(bitmapDescriptorFromVector(R.drawable.ic_parking_marker))
                        )
                        if (marker != null) {
                            markerMap[marker] = parkingSpot
                        }
                    }
                }

                parkingSpotList.sortBy { it.distance }
                parkingListAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                zoomCameraToFit(userLocation)
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error getting parking spots: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchBookingActivity(spot: ParkingSpot) {
        val intent = Intent(this, BookingActivity::class.java).apply {
            putExtra("SPOT_ID", spot.id)
            putExtra("SPOT_NAME", spot.name)
            putExtra("SPOT_PRICE_PER_HOUR", spot.pricePerHour)
            putExtra("SPOT_AVAILABLE_SLOTS", spot.availableSlots)
        }
        startActivity(intent)
    }

    private fun zoomCameraToFit(userLocation: Location?) {
        if (markerMap.isEmpty() && userLocation == null) return

        val boundsBuilder = LatLngBounds.Builder()
        markerMap.keys.forEach { marker ->
            boundsBuilder.include(marker.position)
        }
        if (userLocation != null) {
            boundsBuilder.include(LatLng(userLocation.latitude, userLocation.longitude))
        }

        if (markerMap.isNotEmpty() || userLocation != null) {
            val bounds = boundsBuilder.build()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
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