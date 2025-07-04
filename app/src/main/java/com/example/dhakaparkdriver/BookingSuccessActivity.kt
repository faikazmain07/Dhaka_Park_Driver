package com.example.dhakaparkdriver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityBookingSuccessBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class BookingSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get booking details passed from BookingActivity
        val bookingId = intent.getStringExtra("BOOKING_ID")
        val spotName = intent.getStringExtra("SPOT_NAME")
        val startTime = intent.getStringExtra("START_TIME")
        val endTime = intent.getStringExtra("END_TIME")

        // Display booking details
        val detailsText = "Spot: $spotName\nTime: $startTime - $endTime"
        binding.tvBookingDetails.text = detailsText

        // Generate and display the QR code
        if (bookingId != null) {
            generateQrCode(bookingId)
        }

        // Set listener for the Done button to go back to the dashboard
        binding.btnDone.setOnClickListener {
            finish()
        }
    }

    private fun generateQrCode(bookingId: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            // The QR code will contain the unique booking ID
            val bitmap = barcodeEncoder.encodeBitmap(bookingId, BarcodeFormat.QR_CODE, 400, 400)
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}