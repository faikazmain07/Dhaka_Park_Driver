package com.example.dhakaparkdriver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.dhakaparkdriver.R

class UserRoleSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_role_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonOwner = view.findViewById<Button>(R.id.button_owner)
        val buttonDriver = view.findViewById<Button>(R.id.button_driver)
        val buttonParkingAttendant = view.findViewById<Button>(R.id.button_parking_attendant)

        // This will navigate to the login screen for any role for now.
        // We can add logic later to pass the selected role.
        val navigateToLogin = View.OnClickListener {
            findNavController().navigate(R.id.action_userRoleSelectionFragment_to_loginFragment)
        }

        buttonOwner.setOnClickListener(navigateToLogin)
        buttonDriver.setOnClickListener(navigateToLogin)
        buttonParkingAttendant.setOnClickListener(navigateToLogin)
    }
}