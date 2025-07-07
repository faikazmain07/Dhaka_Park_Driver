package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonOwner.setOnClickListener {
            navigateToLogin("owner")
        }

        binding.buttonDriver.setOnClickListener {
            navigateToLogin("driver")
        }

        binding.buttonParkingAttendant.setOnClickListener {
            navigateToLogin("guard")
        }
    }

    private fun navigateToLogin(role: String) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("USER_ROLE", role)
        }
        startActivity(intent)
    }
}