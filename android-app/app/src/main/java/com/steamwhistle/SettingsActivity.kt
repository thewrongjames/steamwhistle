package com.steamwhistle

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity() {
    private val auth = FirebaseManager.getInstance().auth

    private lateinit var emailView: TextView
    private lateinit var logoutButton: Button
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        emailView = findViewById(R.id.profileEmail)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.settingsBackButton)

        emailView.text = auth.currentUser?.email

        // Login button listener
        logoutButton.setOnClickListener {
            logout()
        }

        // Register listener
        backButton.setOnClickListener {
            switchToHome()
        }
    }

    private fun switchToHome() {
        val intent = Intent(this, WatchlistActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        auth.signOut()
        startActivity(intent)
    }

    companion object {
        private const val TAG = "Settings"
    }
}