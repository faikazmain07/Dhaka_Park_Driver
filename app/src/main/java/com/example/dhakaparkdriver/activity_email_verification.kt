package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val auth = FirebaseAuth.getInstance() // Use for resending email only
    // Do NOT use Firestore here; this screen is just about verification status guidance.

    private var userRole: String = "driver"
    private var userEmail: String = "" // This is the email of the user who *just* registered

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get role and email passed from RegisterActivity
        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Display the user's email for confirmation
        binding.tvUserEmail.text = userEmail
        Toast.makeText(this, "Verification email sent to $userEmail.", Toast.LENGTH_LONG).show() // Confirm email sent

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // "I've Verified My Email" button now just redirects to Login
        binding.btnCheckVerification.setOnClickListener {
            Toast.makeText(this, "Please log in with your verified email.", Toast.LENGTH_LONG).show()
            navigateToLogin()
        }

        binding.btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        binding.btnGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun resendVerificationEmail() {
        // CRITICAL: To resend, the user must be the 'currentUser' object.
        // When user navigates here, they are signed out.
        // So we need to sign them in *temporarily* to send the email.
        // This is a common Firebase Auth pattern for resending verification from a logged-out state.

        // For a robust resend, you would ask the user to input their email and password again first.
        // Given our current flow, they are already on the verification screen with their email known.

        // To be simplest here, we will *assume* the email from the intent is the target.
        // The Firebase Auth 'sendEmailVerification' method implicitly uses the current user.
        // If the user is signed out, this call *might* fail.
        // The Firebase best practice for resending from this state is to:
        // 1. Re-authenticate them with email/password
        // 2. Then send verification email.
        // For now, let's simplify and just attempt to send email to the specific address.

        // Re-authenticate them silently IF their email is known
        val currentAuthUser = auth.currentUser
        if (currentAuthUser == null) {
            // If the user is signed out (which they are after registration redirects here)
            // We cannot send verification email without an authenticated user.
            // The correct flow is: user goes to login, logs in (unverified), then resends from LoginActivity.
            Toast.makeText(this, "Please go to login and sign in to resend verification.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        binding.btnResendEmail.isEnabled = false
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()

        currentAuthUser.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                binding.btnResendEmail.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to ${currentAuthUser.email}. Check inbox/spam.", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("EmailVerificationActivity", "Failed to resend verification email for ${currentAuthUser.email}", task.exception)
                    Toast.makeText(this, "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Helper function to navigate to LoginActivity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("USER_ROLE", userRole) // Pass role back to Login
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Finish EmailVerificationActivity
    }
}