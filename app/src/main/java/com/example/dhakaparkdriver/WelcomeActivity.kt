package com.example.dhakaparkdriver // FIXED: Correct package name

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.dhakaparkdriver.databinding.ActivityWelcomeBinding // FIXED: Import with correct package
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    // Declare ViewBinding and Firebase Auth
    private lateinit var binding: ActivityWelcomeBinding // FIXED: This will now be recognized
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root) // FIXED: Correct way to set content view with binding

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Check if the user is null (they shouldn't be if they reached this screen)
        if (currentUser == null) {
            // If something went wrong and there's no user, go back to the registration screen
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Display the user's email in the TextView
        binding.tvUserEmail.text = currentUser.email // FIXED: This will now be recognized

        // Set up the logout button
        binding.btnLogout.setOnClickListener { // FIXED: This will now be recognized
            // Sign the user out of Firebase
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Go back to the registration screen and clear the activity stack
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}