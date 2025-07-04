package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.view.View // <--- ADD THIS LINE

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private var userRole: String = "driver" // Role passed from previous activity
    private var userEmail: String = ""      // Email passed from previous activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get role and email passed from RegisterActivity
        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Display the user's email for confirmation
        binding.tvUserEmail.text = userEmail

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCheckVerification.setOnClickListener {
            checkEmailVerificationAndProceed()
        }

        binding.btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        binding.btnGoToLogin.setOnClickListener {
            // Allows user to go to login screen if they decide not to verify immediately
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("USER_ROLE", userRole) // Pass role back
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun checkEmailVerificationAndProceed() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            Toast.makeText(this, "No user logged in. Please go to Login.", Toast.LENGTH_SHORT).show()
            binding.btnGoToLogin.visibility = View.VISIBLE
            return
        }

        binding.btnCheckVerification.isEnabled = false
        Toast.makeText(this, "Checking verification status...", Toast.LENGTH_SHORT).show()

        firebaseUser.reload().addOnCompleteListener { reloadTask ->
            binding.btnCheckVerification.isEnabled = true
            if (reloadTask.isSuccessful) {
                if (firebaseUser.isEmailVerified) {
                    Log.d("EmailVerificationActivity", "Email is verified for ${firebaseUser.email}")
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()

                    // User is verified, now fetch their profile data to route them
                    fetchUserRoleAndRedirect(firebaseUser.uid)

                } else {
                    Log.d("EmailVerificationActivity", "Email NOT verified for ${firebaseUser.email} after reload.")
                    Toast.makeText(this, "Email not verified yet. Please click the link in your email.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e("EmailVerificationActivity", "Failed to reload user for verification check.", reloadTask.exception)
                Toast.makeText(this, "Failed to check status. Check internet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser

        if (user == null || user.email.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No user found to resend email.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnResendEmail.isEnabled = false // Disable to prevent multiple clicks
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()

        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                binding.btnResendEmail.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to ${user.email}. Check inbox/spam.", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("EmailVerificationActivity", "Failed to resend verification email for ${user.email}", task.exception)
                    Toast.makeText(this, "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // This function is copied from SplashActivity to handle post-verification routing
    private fun fetchUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    when (role) {
                        "driver" -> navigateTo(DriverDashboardActivity::class.java)
                        "owner" -> {
                            Toast.makeText(this, "Owner Dashboard coming soon!", Toast.LENGTH_SHORT).show()
                            navigateTo(DriverDashboardActivity::class.java) // Placeholder
                        }
                        "guard" -> {
                            Toast.makeText(this, "Guard Dashboard coming soon!", Toast.LENGTH_SHORT).show()
                            navigateTo(DriverDashboardActivity::class.java) // Placeholder
                        }
                        else -> {
                            Log.w("EmailVerificationActivity", "User role not found or invalid: $role. UID: $uid")
                            Toast.makeText(this, "User role not set. Please log in again.", Toast.LENGTH_SHORT).show()
                            auth.signOut() // Sign out to force re-login/re-register
                            navigateTo(LoginActivity::class.java)
                        }
                    }
                } else {
                    Log.w("EmailVerificationActivity", "User profile document not found for UID: $uid")
                    Toast.makeText(this, "User profile missing. Please log in again.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    navigateTo(LoginActivity::class.java)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EmailVerificationActivity", "Failed to fetch user role from Firestore for UID: $uid", exception)
                Toast.makeText(this, "Failed to get user role. Please log in again.", Toast.LENGTH_LONG).show()
                auth.signOut()
                navigateTo(LoginActivity::class.java)
            }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finish EmailVerificationActivity
    }
}