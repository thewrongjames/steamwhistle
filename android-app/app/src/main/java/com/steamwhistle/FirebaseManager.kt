package com.steamwhistle

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * A class for holding our firebase instances. Allows them to be set to use the emulator if we are
 * running locally, and to use production otherwise.
 */
class FirebaseManager {
    companion object {
        private const val TAG = "FirebaseManager"

        // If you want to force the use of the production database, set this to true.
        private const val RUN_ON_PRODUCTION_EVEN_IN_DEBUG = true

        // See https://developer.android.com/studio/run/emulator-networking#networkaddresses
        private const val HOST_LOOPBACK = "10.0.2.2"

        @Volatile
        private var instance: FirebaseManager? = null

        /**
         * Get the singleton FirebaseManager instance.
         */
        fun getInstance(): FirebaseManager {
            synchronized(this) {
                val newInstance = instance ?: FirebaseManager()
                instance = newInstance
                return newInstance
            }
        }
    }

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val usingEmulators = BuildConfig.DEBUG && !RUN_ON_PRODUCTION_EVEN_IN_DEBUG

    init {
        if (usingEmulators) {
            Log.i(TAG, "Connecting Firebase to local emulators.")

            firestore.useEmulator(HOST_LOOPBACK, 8080)
            // The emulated database clears contents on shutdown, but by default the SDK on the
            // phone won't. This is to avoid sync issues.
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            firestore.firestoreSettings = settings

            auth.useEmulator(HOST_LOOPBACK, 9099)
        }
    }
}