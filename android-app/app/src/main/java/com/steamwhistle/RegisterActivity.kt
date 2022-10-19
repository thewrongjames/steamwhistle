package com.steamwhistle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * This is the activity that prompts user to register.
 */
class RegisterActivity: AppCompatActivity() {

    private lateinit var emailView: EditText
    private lateinit var confirmPasswordView: EditText
    private lateinit var passwordView: EditText
    private lateinit var registerButton: Button
    private lateinit var toLoginView: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        setContentView(R.layout.activity_register)

        emailView = findViewById(R.id.registerEmail)
        passwordView = findViewById(R.id.registerPassword)
        confirmPasswordView = findViewById(R.id.registerConfirmPassword)
        registerButton = findViewById(R.id.registerRegisterButton)
        toLoginView = findViewById(R.id.toLogin)

        // Register button listener
        registerButton.setOnClickListener {
            createAccount(emailView.text.toString(), passwordView.text.toString())
        }

        // Login listener
        toLoginView.setOnClickListener {
            switchToLogin()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    private fun createAccount(email: String, password: String) {

        if (!validateInput(email, password)) {
            Log.w(TAG, "createUserWithEmail:failure, invalid email or password")
            Toast.makeText(baseContext, "Invalid email or password.",
                Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

                Log.d(TAG, "createUserWithEmail:success")
                SteamWhistleRemoteDatabase.loadUserToken(auth.uid)
                switchToHome()

            } else {

                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Authentication failed.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            return true
        }
        return false
    }

    private fun switchToHome() {
        val intent = Intent(this, WatchlistActivity::class.java)
        startActivity(intent)
    }

    private fun switchToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "EmailPasswordRegister"
    }
}