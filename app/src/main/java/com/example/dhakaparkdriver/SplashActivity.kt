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
// Removed: import android.net.Uri (no longer handling deep links here)

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)

        // Standard splash screen delay, then proceed with status check
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 1500)
    }

    private fun checkUserStatus() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            navigateTo(RoleSelectionActivity::class.java)
        } else {
            // A user is signed in. Reload to get the latest verification status.
            firebaseUser.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    if (firebaseUser.isEmailVerified) {
                        Log.d("SplashActivity", "User is logged in and email verified: ${firebaseUser.email}. Fetching role.")
                        fetchUserRoleAndRedirect(firebaseUser.uid)
                    } else {
                        Log.d("SplashActivity", "User email not verified after reload: ${firebaseUser.email}. Signing out.")
                        Toast.makeText(this, "Please verify your email address to continue.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        navigateTo(LoginActivity::class.java)
                    }
                } else {
                    Log.e("SplashActivity", "Failed to reload user data on splash.", reloadTask.exception)
                    Toast.makeText(this, "Failed to check user status. Please log in again.", Toast.LENGTH_LONG).show()
                    auth.signOut()
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
                        "owner" -> navigateTo(OwnerDashboardActivity::class.java)
                        "guard" -> navigateTo(GuardDashboardActivity::class.java)
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

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}