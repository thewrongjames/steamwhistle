package com.steamwhistle

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WhistleMessagingService(): FirebaseMessagingService() {
    companion object {
        private const val TAG = "WhistleMessagingService"
        const val NOTIFICATION_APP_ID_KEY = "appId"
        const val NOTIFICATION_PRICE_KEY = "currentPrice"
        const val NOTIFICATION_NAME_KEY = "appName"
    }

    private lateinit var database: SteamWhistleDatabase
    private lateinit var dao: WatchlistDao

    // Create a coroutine scope for performing database changes. We need to ensure that we clean
    // this up ourselves, so we cancel it in onDestroy.
    private val scope = CoroutineScope(Dispatchers.IO)

    // A handler for the main thread, allowing us to tell it to make toast.
    private lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()

        database = SteamWhistleDatabase.getDatabase(this)
        dao = database.watchlistDao()

        handler = Handler(Looper.getMainLooper())
    }

    // Display foreground notifications
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val appIdString = remoteMessage.data[NOTIFICATION_APP_ID_KEY]
            val name = remoteMessage.data[NOTIFICATION_NAME_KEY]
            val priceString = remoteMessage.data[NOTIFICATION_PRICE_KEY]

            scope.launch {
                dao.attemptToUpdateLocalGameFromNotificationData(appIdString, name, priceString)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val notificationString = it.body
            Log.d(TAG, "Message Notification Body: $notificationString")
            if (notificationString != null) {
                handler.post {
                    Toast.makeText(this, notificationString, Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e(TAG, "Got null notification body.")
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up our coroutine scope.
        scope.cancel()
    }
}