package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private val db = Firebase.firestore // Reference to the Firestore database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // --- Input Validation (no changes here) ---
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            binding.etFullName.requestFocus()
            return
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // The original logic to hide the progress bar has been moved
                // inside the success/failure listeners for better accuracy.

                if (task.isSuccessful) {
                    saveUserProfile(fullName, email) // Call new function to save profile
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        baseContext,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserProfile(fullName: String, email: String) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(baseContext, "User not found after creation.", Toast.LENGTH_LONG).show()
            return
        }

        val selectedRoleId = binding.rgRoles.checkedRadioButtonId
        if (selectedRoleId == -1) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(baseContext, "Please select a role.", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedRadioButton = findViewById<RadioButton>(selectedRoleId)
        // Clean up the role name to be a single lowercase word, e.g., "parkingowner"
        val userRole = selectedRadioButton.text.toString().lowercase().replace(" ", "")

        // We add a new status to track if the profile is complete
        val userData = hashMapOf(
            "uid" to firebaseUser.uid,
            "fullName" to fullName,
            "email" to email,
            "role" to userRole,
            "createdAt" to System.currentTimeMillis(),
            "profileStatus" to "pending_setup" // Status indicates more info is needed
        )

        db.collection("users").document(firebaseUser.uid)
            .set(userData)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Account created. Please complete your profile.", Toast.LENGTH_LONG).show()

                // --- THIS IS THE CRITICAL NEW ROUTING LOGIC ---
                when (userRole) {
                    "driver" -> {
                        // If user is a driver, go to the profile setup screen
                        val intent = Intent(this, DriverProfileSetupActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    "parkingowner", "guard" -> {
                        // TODO: Create dashboards for Owner and Guard roles.
                        // For now, we can show a message and maybe go to a placeholder screen or log them out.
                        Toast.makeText(this, "Owner/Guard dashboard is not yet available.", Toast.LENGTH_LONG).show()
                        // Let's send them to the login screen for now.
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        // Fallback case, should not happen.
                        Toast.makeText(this, "Unknown role. Please contact support.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}