package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager // <--- ADD THIS IMPORT
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.net.Uri

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the splash screen full-screen for a cleaner look
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)

        // Check for incoming email verification link immediately
        if (intent != null && intent.data != null) {
            val link = intent.data.toString()
            Log.d("SplashActivity", "Incoming Deep Link: $link")

            if (auth.isSignInWithEmailLink(link)) {
                handleEmailLinkSignIn(link)
                return
            }
        }

        // Normal splash screen delay, then proceed with status check if no deep link
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 1500)
    }

    private fun handleEmailLinkSignIn(emailLink: String) {
        val uri = Uri.parse(emailLink)
        val oobCode = uri.getQueryParameter("oobCode")
        val email = uri.getQueryParameter("email")

        if (oobCode == null) {
            Log.e("SplashActivity", "Email link missing oobCode parameter. Cannot verify.")
            Toast.makeText(this, "Verification link invalid. Please resend.", Toast.LENGTH_LONG).show()
            navigateTo(LoginActivity::class.java)
            return
        }

        if (email.isNullOrEmpty()) {
            Log.e("SplashActivity", "Email not found in deep link. Cannot complete sign-in with link.")
            Toast.makeText(this, "Please go to Login and try logging in with your email.", Toast.LENGTH_LONG).show()
            navigateTo(LoginActivity::class.java)
            return
        }

        auth.signInWithEmailLink(email, emailLink)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SplashActivity", "Email link sign-in successful: ${task.result?.user?.email}")
                    Toast.makeText(this, "Email verified and logged in!", Toast.LENGTH_LONG).show()
                    fetchUserRoleAndRedirect(task.result?.user?.uid!!)
                } else {
                    Log.e("SplashActivity", "Error signing in with email link.", task.exception)
                    Toast.makeText(this, "Failed to sign in via link. Please try logging in normally.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    navigateTo(LoginActivity::class.java)
                }
            }
    }

    private fun checkUserStatus() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            navigateTo(RoleSelectionActivity::class.java)
        } else {
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