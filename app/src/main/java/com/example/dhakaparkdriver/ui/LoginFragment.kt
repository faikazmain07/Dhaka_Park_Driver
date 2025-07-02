package com.example.dhakaparkdriver.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dhakaparkdriver.DriverDashboardActivity
import com.example.dhakaparkdriver.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    // Use _binding pattern for fragments
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // TODO: Navigate to RegisterFragment when it's created
        binding.tvGoToRegister.setOnClickListener {
            // findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            Toast.makeText(requireContext(), "Register flow coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Input Validation
        if (email.isEmpty() || password.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Login Successful!", Toast.LENGTH_SHORT).show()

                    // TODO: Here we should fetch the user's role and navigate to the correct dashboard.
                    // For now, we'll just go to the DriverDashboardActivity.
                    val intent = Intent(requireContext(), DriverDashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish() // Finish MainActivity

                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Clean up the binding when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}