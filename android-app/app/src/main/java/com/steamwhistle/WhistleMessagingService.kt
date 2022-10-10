package com.steamwhistle

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WhistleMessagingService(): FirebaseMessagingService() {

    lateinit var deviceId: String

    fun addTokenListener() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(WatchlistActivity.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            deviceId = task.result

            Log.d(WatchlistActivity.TAG, deviceId)
        })
    }

    // Display foreground notifications
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(WatchlistActivity.TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(WatchlistActivity.TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(WatchlistActivity.TAG, "Message Notification Body: ${it.body}")
        }

    }

}