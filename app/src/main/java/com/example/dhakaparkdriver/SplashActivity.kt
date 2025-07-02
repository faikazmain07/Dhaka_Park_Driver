package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Use a Handler to delay the screen transition slightly for a better splash experience
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 1500) // 1.5 second delay
    }

    private fun checkUserStatus() {
        // Check if a user is currently logged into Firebase Authentication
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            // No user is signed in, go to the LoginActivity
            navigateTo(LoginActivity::class.java)
        } else {
            // A user is signed in, now we need to check their role from Firestore
            fetchUserRoleAndRedirect(firebaseUser.uid)
        }
    }

    private fun fetchUserRoleAndRedirect(uid: String) {
        // Get the user's document from the "users" collection in Firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get the role field from the document
                    val role = document.getString("role")

                    // Route the user based on their role
                    when (role) {
                        "driver" -> navigateTo(DriverDashboardActivity::class.java) // <--- CORRECT: Navigates to the new dashboard
                        "owner" -> {
                            // TODO: Create OwnerDashboardActivity and navigate here
                            Toast.makeText(this, "Owner Dashboard coming soon!", Toast.LENGTH_SHORT).show()
                            // For now, owners can go to the driver dashboard as a placeholder
                            navigateTo(DriverDashboardActivity::class.java)
                        }
                        "guard" -> {
                            // TODO: Create GuardDashboardActivity and navigate here
                            Toast.makeText(this, "Guard Dashboard coming soon!", Toast.LENGTH_SHORT).show()
                            navigateTo(DriverDashboardActivity::class.java)
                        }
                        else -> {
                            // Role is unknown or not set, send to login
                            Toast.makeText(this, "User role not found. Please log in again.", Toast.LENGTH_SHORT).show()
                            navigateTo(LoginActivity::class.java)
                        }
                    }
                } else {
                    // Document doesn't exist, which is strange. Send to login.
                    Toast.makeText(this, "User profile not found. Please log in again.", Toast.LENGTH_SHORT).show()
                    navigateTo(LoginActivity::class.java)
                }
            }
            .addOnFailureListener {
                // Failed to fetch from Firestore, send to login as a fallback
                Toast.makeText(this, "Failed to get user role. Please log in again.", Toast.LENGTH_SHORT).show()
                navigateTo(LoginActivity::class.java)
            }
    }

    // Helper function to make navigation cleaner
    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finish SplashActivity so it's removed from the back stack
    }
}