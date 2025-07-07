package com.example.dhakaparkdriver

import com.google.firebase.firestore.GeoPoint

data class ParkingSpot(
    val id: String = "",
    val name: String = "",
    val availableSlots: Long = 0,
    val pricePerHour: Long = 0, // Using Long here as price might be integer BDT. If decimal, use Double.
    var distance: Float? = null, // For driver dashboard calculation

    val totalSlots: Long = 0, // Total capacity of the spot
    val operatingHoursStartMillis: Long = 0, // Operating start time in millis
    val operatingHoursEndMillis: Long = 0,   // Operating end time in millis
    val parkingType: String = "covered",     // "covered" or "open"
    val emergencyContact: String = "",       // Emergency contact number/name
    val vehicleTypes: List<String> = listOf(), // List of supported vehicle types (e.g., "car", "bike")
    val photoUrl: String? = null,            // URL to parking spot photo
    val location: GeoPoint? = null
)