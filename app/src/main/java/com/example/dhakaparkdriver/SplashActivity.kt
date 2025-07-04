package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
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
        // Optional: Make splash screen full-screen for a clean look
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)

        // Use a Handler to delay the screen transition slightly for a better splash experience
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 1500) // 1.5 second delay
    }

    private fun checkUserStatus() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            // No user is signed in, go to RoleSelectionActivity
            navigateTo(RoleSelectionActivity::class.java)
        } else {
            // A user is signed in. IMPORTANT: Reload to get the latest verification status.
            firebaseUser.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    // Check verification status AFTER reloading
                    if (firebaseUser.isEmailVerified) {
                        Log.d("SplashActivity", "User is logged in and email verified: ${firebaseUser.email}. Fetching role.")
                        // If verified, proceed to fetch the role from Firestore
                        fetchUserRoleAndRedirect(firebaseUser.uid)
                    } else {
                        // User is logged in, but email is NOT verified after reload
                        Log.d("SplashActivity", "User email not verified after reload: ${firebaseUser.email}. Signing out.")
                        Toast.makeText(this, "Please verify your email address to continue.", Toast.LENGTH_LONG).show()
                        auth.signOut() // Sign them out
                        navigateTo(LoginActivity::class.java) // Send them to Login screen (where they can resend email)
                    }
                } else {
                    // Failed to reload user data (e.g., network issue)
                    Log.e("SplashActivity", "Failed to reload user data on splash.", reloadTask.exception)
                    Toast.makeText(this, "Failed to check user status. Please log in again.", Toast.LENGTH_LONG).show()
                    auth.signOut() // Sign them out as a precaution
                    navigateTo(LoginActivity::class.java)
                }
            }
        }
    }

    private fun fetchUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    when (role) {
                        "driver" -> navigateTo(DriverDashboardActivity::class.java)
                        "owner" -> navigateTo(OwnerDashboardActivity::class.java) // <--- ADDED: Route to Owner Dashboard
                        "guard" -> {
                            // We will create GuardDashboardActivity soon
                            Toast.makeText(this, "Guard Dashboard coming soon!", Toast.LENGTH_SHORT).show()
                            navigateTo(DriverDashboardActivity::class.java) // Temporary placeholder
                        }
                        else -> {
                            Log.w("SplashActivity", "User role not found or invalid: $role. UID: $uid")
                            Toast.makeText(this, "User role not set. Please log in again.", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            navigateTo(LoginActivity::class.java)
                        }
                    }
                } else {
                    Log.w("SplashActivity", "User profile document not found for UID: $uid")
                    Toast.makeText(this, "User profile missing. Please log in again.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    navigateTo(LoginActivity::class.java)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SplashActivity", "Failed to fetch user role from Firestore for UID: $uid", exception)
                Toast.makeText(this, "Failed to get user role. Please log in again.", Toast.LENGTH_LONG).show()
                auth.signOut()
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