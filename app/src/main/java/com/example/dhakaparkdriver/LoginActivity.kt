package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import android.util.Log // <--- ADD THIS LINE

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var userRole: String = "driver" // Default role

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the role passed from RoleSelectionActivity (e.g., "driver", "owner", "guard")
        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"

        auth = FirebaseAuth.getInstance()

        // Set up all UI click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Listener for the main Login button
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // Listener for the "Don't have an account? Register Now" text
        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                // Pass the selected role along to the registration screen
                putExtra("USER_ROLE", userRole)
            }
            startActivity(intent)
        }

        // Listener for the "Resend Verification Email" button (initially hidden)
        // Its visibility and click listener are set inside loginUser() if needed.
        // No setup needed here for it initially.
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Input Validation
        var isValid = true
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; isValid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = "Enter a valid email"; isValid = false }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; isValid = false }

        if (!isValid) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnResendVerification.visibility = View.GONE // Hide resend button during login attempt

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser // Get the newly logged-in user

                    // --- NEW: Check if email is verified ---
                    if (firebaseUser != null && firebaseUser.isEmailVerified) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        // Route to the dashboard. SplashActivity will handle detailed routing on next app open.
                        val intent = Intent(this, DriverDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Email is NOT verified
                        if (firebaseUser != null) {
                            Toast.makeText(this, "Please verify your email address to log in.", Toast.LENGTH_LONG).show()
                            binding.btnResendVerification.visibility = View.VISIBLE // Show resend button
                            binding.btnResendVerification.setOnClickListener { resendVerificationEmail() }
                            // IMPORTANT: Sign out the user to prevent them from staying logged in without verification
                            auth.signOut()
                            // You might want to clear email/password fields here for security
                            binding.etPassword.text?.clear()
                        } else {
                            // This case should ideally not be hit if task.isSuccessful but user is null
                            Toast.makeText(this, "Login error: User not found after successful task.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Login failed for reasons other than verification (e.g., wrong password)
                    Toast.makeText(baseContext, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    binding.btnResendVerification.visibility = View.GONE // Ensure resend button is hidden
                }
            }
    }

    // --- NEW: Function to resend email verification ---
    private fun resendVerificationEmail() {
        val user = auth.currentUser

        // Basic check for user existence and email presence
        if (user == null || user.email.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No user logged in or email missing.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnResendVerification.isEnabled = false // Disable to prevent multiple rapid clicks
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()

        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                binding.btnResendVerification.isEnabled = true // Re-enable button regardless of success/failure
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to ${user.email}. Please check your inbox and spam folder.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginActivity", "Failed to send verification email", task.exception)
                }
            }
    }
}