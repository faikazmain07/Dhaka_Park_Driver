package com.example.dhakaparkdriver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dhakaparkdriver.databinding.ActivityDriverBookingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DriverBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverBookingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private lateinit var driverBookingAdapter: DriverBookingAdapter
    private val bookingsList = mutableListOf<Booking>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        fetchDriverBookings()
    }

    private fun setupRecyclerView() {
        driverBookingAdapter = DriverBookingAdapter(bookingsList)
        binding.rvDriverBookings.layoutManager = LinearLayoutManager(this)
        binding.rvDriverBookings.adapter = driverBookingAdapter
    }

    private fun fetchDriverBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view bookings.", Toast.LENGTH_SHORT).show()
            bookingsList.clear()
            driverBookingAdapter.notifyDataSetChanged()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("driverId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING) // Order by most recent booking
            .get()
            .addOnSuccessListener { documents ->
                bookingsList.clear()
                for (document in documents) {
                    document.toObject(Booking::class.java).let { booking ->
                        if (booking != null) {
                            bookingsList.add(booking.copy(id = document.id))
                        }
                    }
                }
                driverBookingAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                Log.d("DriverBookings", "Fetched ${bookingsList.size} bookings for driver: ${currentUser.uid}")
            }
            .addOnFailureListener { e ->
                Log.e("DriverBookings", "Error fetching driver bookings", e)
                Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }
}