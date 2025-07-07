package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var userRole: String = "driver" // Default role

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"
        Log.d("LoginActivity", "User role received: $userRole")

        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("USER_ROLE", userRole)
            }
            startActivity(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; isValid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = "Enter a valid email"; isValid = false }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; isValid = false }

        if (!isValid) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnResendVerification.visibility = View.GONE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    if (firebaseUser != null && firebaseUser.isEmailVerified) {
                        Log.d("LoginActivity", "Login successful and email verified. UID: ${firebaseUser.uid}")
                        fetchUserRoleAndRedirect(firebaseUser.uid)
                    } else {
                        if (firebaseUser != null) {
                            Toast.makeText(this, "Please verify your email address to log in.", Toast.LENGTH_LONG).show()
                            binding.btnResendVerification.visibility = View.VISIBLE
                            binding.btnResendVerification.setOnClickListener { resendVerificationEmail() }
                            auth.signOut()
                            binding.etPassword.text?.clear()
                        } else {
                            Toast.makeText(this, "Login error: User data missing.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Toast.makeText(baseContext, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    binding.btnResendVerification.visibility = View.GONE
                }
            }
    }

    private fun fetchUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    Log.d("LoginActivity", "User role fetched: $role for UID: $uid")
                    when (role) {
                        "driver" -> navigateToDashboard(DriverDashboardActivity::class.java)
                        "owner" -> navigateToDashboard(OwnerDashboardActivity::class.java)
                        "guard" -> navigateToDashboard(GuardDashboardActivity::class.java)
                        else -> {
                            Log.w("LoginActivity", "User role not found or invalid: $role. UID: $uid")
                            Toast.makeText(this, "User role not set. Please log in again.", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            navigateToDashboard(LoginActivity::class.java)
                        }
                    }
                } else {
                    Log.w("LoginActivity", "User profile document not found for UID: $uid")
                    Toast.makeText(this, "User profile missing. Please log in again.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    navigateToDashboard(LoginActivity::class.java)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LoginActivity", "Failed to fetch user role from Firestore for UID: $uid", exception)
                Toast.makeText(this, "Failed to get user role. Please log in again.", Toast.LENGTH_LONG).show()
                auth.signOut()
                navigateToDashboard(LoginActivity::class.java)
            }
    }

    private fun navigateToDashboard(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser

        if (user == null || user.email.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No user logged in or email missing.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnResendVerification.isEnabled = false
        Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()

        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                binding.btnResendVerification.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to ${user.email}. Please check your inbox and spam folder.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginActivity", "Failed to send verification email", task.exception)
                }
            }
    }

    override fun onBackPressed() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}