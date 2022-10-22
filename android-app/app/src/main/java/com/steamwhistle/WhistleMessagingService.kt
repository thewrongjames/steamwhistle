package com.steamwhistle

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WhistleMessagingService(): FirebaseMessagingService() {
    companion object {
        private const val TAG = "WhistleMessagingService"
    }

    private lateinit var deviceId: String

    /**
     * Add a listener to add the device ID to firestore when it becomes available. The firestore
     * upload will be done in the given coroutine [scope].
     */
    fun addTokenListener(scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            deviceId = task.result
            scope.launch { SteamWhistleRemoteDatabase.addDevice(deviceId) }

            Log.d(TAG, deviceId)
        })
    }

    // Display foreground notifications
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        // TODO: do something with this data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

    }
}