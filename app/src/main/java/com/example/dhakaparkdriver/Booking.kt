package com.example.dhakaparkdriver

// Data class to represent a booking document in Firestore
data class Booking(
    val id: String = "",
    val spotId: String = "",
    val driverId: String = "",
    val startTimeMillis: Long = 0, // When the driver intended to start parking
    val endTimeMillis: Long = 0,   // When the driver intended to end parking
    val totalPrice: String = "0 BDT", // Stored as string from BookingActivity
    val status: String = "confirmed", // e.g., "confirmed", "active", "completed", "cancelled"
    val createdAt: Long = 0,

    // --- Fields for session management and actual usage ---
    val actualStartTime: Long? = null, // When the guard actually started the session
    val actualEndTime: Long? = null,   // When the guard actually ended the session
    val sessionStatus: String = "pending" // e.g., "pending" (after booking), "active", "ended", "no_show"
)