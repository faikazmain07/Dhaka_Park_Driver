package com.example.dhakaparkdriver

data class ParkingSpot(
    val id: String = "",
    val name: String = "",
    val availableSlots: Long = 0,
    val pricePerHour: Long = 0,
    // Add this distance property. It's a 'var' because we will calculate it.
    var distance: Float? = null
)