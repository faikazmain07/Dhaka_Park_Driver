package com.example.dhakaparkdriver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.bumptech.glide.Glide
import java.io.IOException // NEW IMPORT
import android.location.Geocoder // NEW IMPORT
import java.text.SimpleDateFormat
import java.util.*

class AddParkingSpotActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddParkingSpotBinding
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private var locationMarker: Marker? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private var operatingHoursStartMillis: Long? = null
    private var operatingHoursEndMillis: Long? = null

    // Removed PlacesClient, using Geocoder instead
    private lateinit var geocoder: Geocoder // NEW: Geocoder instance


    private var selectedImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).into(binding.ivSpotPhotoPreview)
        } else {
            Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
        }
    }

    // Removed autocompleteLauncher


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddParkingSpotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Geocoder
        geocoder = Geocoder(this, Locale.getDefault())

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_frame_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        setupClickListeners()
        setupAddressSearch() // Setup for address search
    }

    private fun setupClickListeners() {
        binding.btnAddSpot.setOnClickListener {
            addParkingSpot()
        }

        binding.tvOperatingHoursStart.setOnClickListener {
            showTimePicker(isStartTime = true)
        }
        binding.tvOperatingHoursEnd.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        binding.btnSelectPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }
    }

    // NEW: Setup for Address Search functionality using Geocoder
    private fun setupAddressSearch() {
        binding.tilAddressSearch.setEndIconOnClickListener {
            val query = binding.etAddressSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchAddress(query) // Call local search function
            } else {
                Toast.makeText(this, "Please enter an address to search.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etAddressSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etAddressSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchAddress(query) // Call local search function
                } else {
                    Toast.makeText(this, "Please enter an address to search.", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    // NEW: Function to perform address search using Geocoder
    private fun searchAddress(query: String) {
        binding.progressBar.visibility = View.VISIBLE // Show progress while searching
        try {
            val addresses = geocoder.getFromLocationName(query, 1) // Get up to 1 result
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)

                selectedLocation = latLng
                updateLocationMarker(latLng) // Move map and pin
                binding.tvSelectedLocation.text = address.getAddressLine(0) ?: "Lat: %.4f, Lng: %.4f".format(Locale.getDefault(), latLng.latitude, latLng.longitude)
                binding.etAddressSearch.setText(address.getAddressLine(0)) // Pre-fill search bar
                Toast.makeText(this, "Moved map to: ${address.getAddressLine(0)}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No results found for '$query'.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("AddParkingSpotActivity", "Geocoder service not available or network error", e)
            Toast.makeText(this, "Address search error: Check network connection.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("AddParkingSpotActivity", "Error during geocoding", e)
            Toast.makeText(this, "Error searching address: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            binding.progressBar.visibility = View.GONE // Hide progress
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLocation = LatLng(23.777176, 90.399452) // Center of Dhaka
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        mMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            updateLocationMarker(latLng)
            binding.tvSelectedLocation.text = "Lat: %.4f, Lng: %.4f".format(Locale.getDefault(), latLng.latitude, latLng.longitude)
            Log.d("AddParkingSpotActivity", "Selected map location: $latLng")
        }
    }

    private fun updateLocationMarker(latLng: LatLng) {
        locationMarker?.remove()
        locationMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
                .icon(bitmapDescriptorFromVector(R.drawable.ic_parking_marker))
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
            .setTitleText(if (isStartTime) "Select Opening Time" else "Select Closing Time")
            .build()

        picker.show(supportFragmentManager, "OPERATING_HOURS_PICKER")

        picker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val selectedTimeMillis = calendar.timeInMillis
            val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedTimeMillis))

            if (isStartTime) {
                operatingHoursStartMillis = selectedTimeMillis
                binding.tvOperatingHoursStart.text = formattedTime
            } else {
                operatingHoursEndMillis = selectedTimeMillis
                binding.tvOperatingHoursEnd.text = formattedTime
            }
        }
    }

    private fun addParkingSpot() {
        val spotName = binding.etSpotName.text.toString().trim()
        val totalSlotsStr = binding.etTotalSlots.text.toString().trim()
        val pricePerHourStr = binding.etPricePerHour.text.toString().trim()
        val emergencyContact = binding.etEmergencyContact.text.toString().trim()

        val selectedParkingTypeId = binding.rgCoveredOpen.checkedRadioButtonId
        val parkingType = if (selectedParkingTypeId != -1) {
            findViewById<RadioButton>(selectedParkingTypeId).text.toString().lowercase(Locale.getDefault())
        } else {
            "covered"
        }

        val vehicleTypes = mutableListOf<String>()
        if (binding.cbCar.isChecked) vehicleTypes.add("car")
        if (binding.cbBike.isChecked) vehicleTypes.add("bike")
        if (binding.cbTruck.isChecked) vehicleTypes.add("truck")


        // Input Validation
        var isValid = true
        if (spotName.isEmpty()) {
            binding.tilSpotName.error = "Parking spot name is required"; isValid = false
        } else { binding.tilSpotName.error = null }

        val totalSlotsValidated = totalSlotsStr.toLongOrNull()
        if (totalSlotsValidated == null || totalSlotsValidated <= 0) {
            binding.tilTotalSlots.error = "Please enter a valid number of slots (>0)"; isValid = false
        } else { binding.tilTotalSlots.error = null }

        val pricePerHourValidated = pricePerHourStr.toFloatOrNull()
        if (pricePerHourValidated == null || pricePerHourValidated <= 0) {
            binding.tilPricePerHour.error = "Please enter a valid price per hour (>0)"; isValid = false
        } else { binding.tilPricePerHour.error = null }

        if (operatingHoursStartMillis == null || operatingHoursEndMillis == null) {
            Toast.makeText(this, "Please select both operating start and end times.", Toast.LENGTH_LONG).show()
            isValid = false
        } else if (operatingHoursEndMillis!! <= operatingHoursStartMillis!!) {
            Toast.makeText(this, "Operating end time must be after start time.", Toast.LENGTH_LONG).show()
            isValid = false
        }

        if (vehicleTypes.isEmpty()) {
            Toast.makeText(this, "Please select at least one supported vehicle type.", Toast.LENGTH_LONG).show()
            isValid = false
        }

        if (selectedLocation == null) {
            Toast.makeText(this, "Please tap on the map to select the parking spot's location.", Toast.LENGTH_LONG).show()
            isValid = false
        }

        if (!isValid) return

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Owner not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddSpot.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Adding parking spot...", Toast.LENGTH_SHORT).show()

        if (selectedImageUri != null) {
            uploadPhotoAndAddSpot(
                spotName,
                totalSlotsValidated!!,
                pricePerHourValidated!!,
                operatingHoursStartMillis!!,
                operatingHoursEndMillis!!,
                parkingType, emergencyContact, vehicleTypes, currentUser.uid
            )
        } else {
            saveParkingSpotToFirestore(
                spotName,
                totalSlotsValidated!!,
                pricePerHourValidated!!,
                operatingHoursStartMillis!!,
                operatingHoursEndMillis!!,
                parkingType, emergencyContact, vehicleTypes, null, currentUser.uid
            )
        }
    }

    private fun uploadPhotoAndAddSpot(
        spotName: String, totalSlots: Long, pricePerHour: Float,
        operatingHoursStartMillis: Long, operatingHoursEndMillis: Long,
        parkingType: String, emergencyContact: String, vehicleTypes: List<String>,
        ownerId: String
    ) {
        val photoRef = storage.reference.child("parking_spot_photos/${System.currentTimeMillis()}-${ownerId}.jpg")
        selectedImageUri?.let { uri ->
            photoRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d("AddParkingSpotActivity", "Photo uploaded. Download URL: $downloadUri")
                        saveParkingSpotToFirestore(
                            spotName, totalSlots, pricePerHour, operatingHoursStartMillis, operatingHoursEndMillis,
                            parkingType, emergencyContact, vehicleTypes, downloadUri.toString(), ownerId
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AddParkingSpotActivity", "Photo upload failed", e)
                    Toast.makeText(this, "Failed to upload photo: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnAddSpot.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun saveParkingSpotToFirestore(
        spotName: String, totalSlots: Long, pricePerHour: Float,
        operatingHoursStartMillis: Long, operatingHoursEndMillis: Long,
        parkingType: String, emergencyContact: String, vehicleTypes: List<String>,
        photoUrl: String?, ownerId: String
    ) {
        val parkingSpotData = hashMapOf(
            "ownerId" to ownerId,
            "name" to spotName,
            "totalSlots" to totalSlots,
            "availableSlots" to totalSlots,
            "pricePerHour" to pricePerHour,
            "operatingHoursStartMillis" to operatingHoursStartMillis,
            "operatingHoursEndMillis" to operatingHoursEndMillis,
            "parkingType" to parkingType,
            "emergencyContact" to emergencyContact,
            "vehicleTypes" to vehicleTypes,
            "photoUrl" to photoUrl,
            "location" to GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude),
            "isAvailable" to true,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("parking_spots")
            .add(parkingSpotData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Parking spot added successfully!", Toast.LENGTH_LONG).show()
                Log.d("AddParkingSpotActivity", "Spot added with ID: ${documentReference.id}")
                binding.progressBar.visibility = View.GONE
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add spot: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AddParkingSpotActivity", "Error adding parking spot to Firestore", e)
                binding.btnAddSpot.isEnabled = true
                binding.progressBar.visibility = View.GONE
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