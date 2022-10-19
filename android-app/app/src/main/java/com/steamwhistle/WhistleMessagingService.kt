package com.steamwhistle

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WhistleMessagingService: FirebaseMessagingService() {

    private const val TAG = "WhistleMessagingService"
    private lateinit var deviceId: String

    // Token management functions
    // Attempts to get token from Firebase and save it
    fun registerToken() {
        val instance = FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                Log.d(TAG, "Initial token generated")
                deviceId = task.result

                SteamWhistleRemoteDatabase.loadDeviceToken(deviceId)
                // By this point, user is already authenticated
                CoroutineScope(Dispatchers.IO).launch {
                    SteamWhistleRemoteDatabase.loadUserDeviceToDatabase()
                }
            })
    }

    // Triggers when a new token is generated, for example, if the security
    // of the previous token is compromised
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token generated")
        deviceId = token

        SteamWhistleRemoteDatabase.loadDeviceToken(deviceId)
        CoroutineScope(Dispatchers.IO).launch {
            SteamWhistleRemoteDatabase.loadUserDeviceToDatabase()
        }
    }


    // Messaging functions
    // Display foreground notifications
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

    }

}