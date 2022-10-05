package com.steamwhistle

import android.app.Activity
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

class RegisterActivity: Activity() {

    lateinit var etEmail: EditText
    lateinit var etConfirmPassword: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    lateinit var tvToLogin: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        setContentView(R.layout.activity_register)

        // get view bindings
        etEmail = findViewById(R.id.registerEmail)
        etPassword = findViewById(R.id.registerPassword)
        etConfirmPassword = findViewById(R.id.registerConfirmPassword)
        btnRegister = findViewById(R.id.registerRegisterButton)
        tvToLogin = findViewById(R.id.toLogin)

        // make button listener
        btnRegister.setOnClickListener {
            createAccount(etEmail.text.toString(), etPassword.text.toString())
        }

        // switch activity on login
        tvToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
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

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(RegisterActivity.TAG, "createUserWithEmail:success")
                val user = auth.currentUser
                updateUI(user)
            } else {
                // If sign in fails, display a message to the user.
                Log.w(RegisterActivity.TAG, "createUserWithEmail:failure", task.exception)
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
        private const val TAG = "EmailPasswordRegister"
    }
}