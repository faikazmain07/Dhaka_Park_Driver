package com.example.dhakaparkdriver

import com.google.firebase.firestore.GeoPoint

data class ParkingSpot(
    val id: String = "",
    val spotName: String? = null,
    val location: GeoPoint? = null,
    val pricePerHour: Long? = null,
    // We can add more fields like availableSlots later
    var distance: Float? = null // This will be calculated on the device
)