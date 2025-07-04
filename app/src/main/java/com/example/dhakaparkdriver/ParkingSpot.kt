package com.example.dhakaparkdriver

import com.google.firebase.firestore.GeoPoint

data class ParkingSpot(
    val id: String = "",
    val name: String = "",
    val availableSlots: Long = 0,
    val pricePerHour: Long = 0,
    var distance: Float? = null, // For driver dashboard calculation

    // --- NEW FIELDS USED IN OWNER DASHBOARD ---
    val totalSlots: Long = 0, // Total capacity of the spot
    val operatingHours: String = "", // e.g., "8:00 AM - 10:00 PM"
    val location: GeoPoint? = null // For owner to view/edit location details
)