package com.example.dhakaparkdriver

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class to represent a booking document in Firestore
data class Booking(
    val id: String = "",
    val spotId: String = "",
    val driverId: String = "",
    val startTimeMillis: Long = 0,       // Driver's intended start time (milliseconds)
    val endTimeMillis: Long = 0,         // Driver's intended end time (milliseconds)
    val totalPrice: String = "0 BDT",    // Total price calculated at booking, stored as string
    val status: String = "confirmed",    // Current state: "confirmed", "active", "completed", "cancelled", "no_show"

    val actualStartTime: Long? = null,   // Actual time session started (milliseconds)
    val actualEndTime: Long? = null,     // Actual time session ended (milliseconds)

    @ServerTimestamp // Automatically sets creation timestamp on Firestore server
    val createdAt: Date? = null,
    @ServerTimestamp // Automatically sets last updated timestamp on Firestore server
    val lastUpdated: Date? = null,
    val numberOfSlots: Int = 1           // Number of slots booked in this one transaction
)