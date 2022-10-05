package com.steamwhistle

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    lateinit var tvToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.loginEmail)
        etPassword = findViewById(R.id.loginPassword)
        btnLogin = findViewById(R.id.loginLoginButton)
        tvToRegister = findViewById(R.id.toRegister)

        // make button listener
        btnLogin.setOnClickListener {
            login(etEmail.text.toString(), etPassword.text.toString())
        }

        // switch to register
        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        // We want to check if the user is still signed in
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload();
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun sendEmailVerification() {}

    private fun updateUI(user: FirebaseUser?) {}

    private fun reload() {}

    companion object {
        private const val TAG = "EmailPasswordSignIn"
    }
}