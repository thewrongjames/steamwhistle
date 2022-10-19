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
 * This is the activity that prompts user to login.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var loginButton: Button
    private lateinit var toRegisterButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            switchToHome()
        }

        setContentView(R.layout.activity_login)

        emailView = findViewById(R.id.loginEmail)
        passwordView = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginLoginButton)
        toRegisterButton = findViewById(R.id.toRegister)

        // Login button listener
        loginButton.setOnClickListener {
            login(emailView.text.toString(), passwordView.text.toString())
        }

        // Register listener
        toRegisterButton.setOnClickListener {
            switchToRegister()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d(TAG, "signInWithEmail:success")
                    SteamWhistleRemoteDatabase.loadUserToken(auth.uid)
                    switchToHome()

                } else {

                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun switchToHome() {
        val intent = Intent(this, WatchlistActivity::class.java)
        startActivity(intent)
    }

    private fun switchToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "EmailPasswordSignIn"
    }
}