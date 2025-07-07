package com.example.dhakaparkdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.dhakaparkdriver.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var userRole: String = "driver" // Default role for safety

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("RegisterActivity", "Google sign-in successful. ID: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("RegisterActivity", "Google sign in failed.", e)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("RegisterActivity", "Google sign in cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userRole = intent.getStringExtra("USER_ROLE") ?: "driver"
        Log.d("RegisterActivity", "User role received: $userRole")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerWithEmail()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        Log.d("RegisterActivity", "Launching Google Sign-In flow...")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.progressBar.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser!!
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser) {
                        Log.d("RegisterActivity", "New user signed up with Google. Saving profile.")
                        val nameFromGoogle = firebaseUser.displayName ?: ""
                        val emailFromGoogle = firebaseUser.email ?: ""

                        val nameToSave = if (binding.etFullName.text.toString().isNotBlank()) {
                            binding.etFullName.text.toString().trim()
                        } else {
                            nameFromGoogle
                        }

                        saveUserProfile(nameToSave, emailFromGoogle)
                    } else {
                        Log.d("RegisterActivity", "Existing user signed in with Google. Navigating to dashboard.")
                        navigateToDashboardForExistingUser()
                    }
                } else {
                    Log.w("RegisterActivity", "Firebase Auth with Google failed: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, "Firebase authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerWithEmail() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true
        if (fullName.isEmpty()) { binding.etFullName.error = "Full name is required"; isValid = false }
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; isValid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = "Enter a valid email"; isValid = false }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; isValid = false }
        if (password.length < 6) { binding.etPassword.error = "Password must be at least 6 characters"; isValid = false }

        if (!isValid) return

        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "Email/Password registration successful. Saving profile.")
                    saveUserProfile(fullName, email)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.w("RegisterActivity", "Email/Password registration failed: ${task.exception?.message}", task.exception)
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserProfile(fullName: String, email: String) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e("RegisterActivity", "Attempted to save profile but FirebaseUser is null.")
            binding.progressBar.visibility = View.GONE
            Toast.makeText(baseContext, "User not authenticated for profile save.", Toast.LENGTH_LONG).show()
            return
        }

        val userData = hashMapOf(
            "uid" to firebaseUser.uid,
            "fullName" to fullName,
            "email" to email,
            "role" to userRole,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(firebaseUser.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "User profile successfully saved to Firestore for UID: ${firebaseUser.uid}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Registration successful! Please check your email for verification.", Toast.LENGTH_LONG).show()

                sendEmailVerification() // Call to send email verification

                auth.signOut()
                val intent = Intent(this, EmailVerificationActivity::class.java).apply {
                    putExtra("USER_EMAIL", email)
                    putExtra("USER_ROLE", userRole)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save spot: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RegisterActivity", "Error saving user profile to Firestore.", e)
                Toast.makeText(baseContext, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser

        if (user == null) {
            Log.e("RegisterActivity", "sendEmailVerification: currentUser is null. Cannot send email.")
            Toast.makeText(baseContext, "Error: User not found for verification.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("RegisterActivity", "Attempting to send standard verification email to: ${user.email}")
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "SUCCESS: Standard email verification request sent for ${user.email}")
                } else {
                    Log.e("RegisterActivity", "FAILURE: Failed to send standard verification email for ${user.email}", task.exception)
                    Toast.makeText(baseContext, "Failed to send verification email. Reason: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToDashboardForExistingUser() {
        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DriverDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}