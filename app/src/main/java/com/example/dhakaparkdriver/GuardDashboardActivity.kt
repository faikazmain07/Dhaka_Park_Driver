package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityGuardDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.core.content.ContextCompat // <--- ADD THIS LINE
import android.Manifest // <--- ADD THIS LINE
import android.content.pm.PackageManager // <--- ADD THIS LINE

class GuardDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show()
            Log.d("GuardDashboard", "QR Code Scan cancelled.")
        } else {
            val scannedBookingId = result.contents
            Toast.makeText(this, "Scanned: $scannedBookingId", Toast.LENGTH_LONG).show()
            Log.d("GuardDashboard", "QR Code Scanned: $scannedBookingId")

            // --- UPDATED: Launch GuardBookingDetailActivity ---
            val intent = Intent(this, GuardBookingDetailActivity::class.java).apply {
                putExtra("BOOKING_ID", scannedBookingId)
            }
            startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayGuardName()
        setupClickListeners()
    }

    private fun displayGuardName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "Guard"
                        binding.tvGuardName.text = fullName
                        Log.d("GuardDashboard", "Fetched guard name: $fullName")
                    } else {
                        binding.tvGuardName.text = "Guard (Profile Missing)"
                        Log.w("GuardDashboard", "Guard profile document not found for UID: ${currentUser.uid}")
                    }
                }
                .addOnFailureListener {
                    binding.tvGuardName.text = "Guard (Error fetching name)"
                    Log.e("GuardDashboard", "Error fetching guard name", it)
                }
        } else {
            binding.tvGuardName.text = "Guard (Not Logged In)"
            Log.w("GuardDashboard", "No current user for guard name display.")
        }
    }

    private fun setupClickListeners() {
        binding.btnScanQrCode.setOnClickListener {
            checkCameraPermissionAndLaunchScanner()
        }

        binding.btnGuardLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun checkCameraPermissionAndLaunchScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("GuardDashboard", "Camera permission already granted. Launching scanner.")
            launchQrScanner()
        } else {
            Log.d("GuardDashboard", "Requesting camera permission.")
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchQrScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a driver's booking QR code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        options.setCameraId(0)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    // Launcher for Camera Permission request (from previous step)
    private val cameraPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("GuardDashboard", "Camera permission granted. Launching scanner.")
            launchQrScanner()
        } else {
            Log.w("GuardDashboard", "Camera permission denied.")
            Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
        }
    }
}