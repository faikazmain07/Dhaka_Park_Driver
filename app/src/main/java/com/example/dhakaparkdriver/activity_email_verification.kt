package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val auth = FirebaseAuth.getInstance()

    private var userRole: String = "driver"
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user role and email from intent
        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Show user email
        binding.tvUserEmail.text = userEmail

        Toast.makeText(
            this,
            "Verification email sent to $userEmail.",
            Toast.LENGTH_LONG
        ).show()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Check verification button → navigate directly to login
        binding.btnCheckVerification.setOnClickListener {
            Toast.makeText(
                this,
                "Please log in with your verified email.",
                Toast.LENGTH_LONG
            ).show()
            navigateToLogin()
        }

        // Resend email
        binding.btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        // Go to login button
        binding.btnGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser

        if (user == null || user.email.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Error: No user found to resend email. Please login.",
                Toast.LENGTH_SHORT
            ).show()
            navigateToLogin()
            return
        }

        binding.btnResendEmail.isEnabled = false
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()

        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                binding.btnResendEmail.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Verification email sent to ${user.email}. Check inbox/spam.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e(
                        "EmailVerificationActivity",
                        "Failed to resend verification email for ${user.email}",
                        task.exception
                    )
                    Toast.makeText(
                        this,
                        "Failed to resend email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("USER_ROLE", userRole)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
