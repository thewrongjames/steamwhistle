package com.steamwhistle

import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SteamWhistleRemoteDatabase {
    private const val TAG = "SteamWhistleRemoteDatabase"
    private const val USERS_COLLECTION_PATH = "users"
    private const val GAMES_COLLECTION_PATH = "games" // Not currently used since games is updated
                                                      // via server-side scripts instead
    private const val DEVICES_SUBCOLLECTION_PATH = "devices"
    private const val WATCHLIST_SUBCOLLECTION_PATH = "watchlist"

    private lateinit var userId: String

    // Callback for Auth service to load most recent user token for preparation of payload
    fun loadUid(newUid: String) {
        userId = newUid
        Log.d(TAG, "userId token loaded: $userId")
    }

    // This check runs prior to every call to the Firestore database
    private fun assertTokensLoaded() {
        if (!::userId.isInitialized || userId.isEmpty()) {
            throw IllegalStateException("userId is empty, load it first")
        }
    }

    /**
     * Attempts to get device token from database for the authenticated user
     * If user is not found in the database, throws an Exception
     * If device is not found as part of user's device list, returns false
     * Else if found, returns true
     */
    private suspend fun getDevice(deviceId: String): Device? = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        val db = FirebaseManager.getInstance().firestore

        try {
            val result = db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .collection(DEVICES_SUBCOLLECTION_PATH)
                .document(deviceId)
                .get()
                .await()
            if (!result.exists()) {
                Log.d(TAG, "No matching devices found")
                return@withContext null
            }

            return@withContext result.toObject(Device::class.java)

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext null
        }

    }

    /**
     * Adds a device token to the authenticated user's list of devices
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If the device is already present in the user's device list, nothing happens
     * Else, adds the device token to the user's list of devices in Firebase
     */
    suspend fun addDevice(deviceId: String) = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        val db = FirebaseManager.getInstance().firestore

        try {
            if (getDevice(deviceId) != null) {
                Log.d(TAG, "devices already in user devices, skipping")
            } else {
                db.collection(USERS_COLLECTION_PATH)
                    .document(userId)
                    .collection(DEVICES_SUBCOLLECTION_PATH)
                    .document(deviceId)
                    .set(Device(deviceId))

                Log.d(TAG, "device id $deviceId added for user $userId")
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
        }
    }

    /**
     * Adds a game or updates threshold info for a game to the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * Checks if threshold is negative, if so, throws an Exception
     * Will always refresh the metadata of the game stored in the database, so do not call this
     * method if you want to preserve the "updated" timestamp
     */
    suspend fun addOrUpdateWatchlistGame(
        appId: Int,
        threshold: Int,
        updatedSeconds: Long,
        updatedNanos: Int,
        createdSeconds: Long,
        createdNanos: Int,
    ) = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        if (threshold < 0) {
            throw IllegalArgumentException("watchlist threshold cannot be negative")
        }

        val db = FirebaseManager.getInstance().firestore

        // Check if game exists, if so, update timestamp rather than create timestamp
        val result = db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .collection(WATCHLIST_SUBCOLLECTION_PATH)
            .document(appId.toString()).get().await()

        val data = mutableMapOf(
            "appId" to appId,
            "threshold" to threshold,
            "updated" to Timestamp(updatedSeconds, updatedNanos)
        )

        // TODO: Only update firebase if the firebase record is older.

        if (result.exists()) {
            Log.d(TAG, "Game $appId already watched by user, updating data")
            data["created"] = result.data!!["created"] as Timestamp
        } else {
            Log.d(TAG, "Adding game $appId for user")
            data["created"] = Timestamp(createdSeconds, createdNanos)
        }

        db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .collection(WATCHLIST_SUBCOLLECTION_PATH)
            .document(appId.toString())
            .set(data)

    }

    /**
     * Removes a game from the user's watchlist if present based ONLY on the appId
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If the game is not in the user's watchlist, then this function does nothing
     * Else, removes the matching appId and threshold info from the user's watchlist
     */
    suspend fun removeGameFromWatchList(appId: Int) = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        val db = FirebaseManager.getInstance().firestore

        try {
            val result = db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .collection(WATCHLIST_SUBCOLLECTION_PATH)
                .document(appId.toString())
                .get()
                .await()

            if (result.exists()) {
                db.collection(USERS_COLLECTION_PATH)
                    .document(userId)
                    .collection(WATCHLIST_SUBCOLLECTION_PATH)
                    .document(appId.toString())
                    .delete()

                Log.d(TAG, "game $appId removed for user")

            } else {
                Log.d(TAG, "game $appId to be removed was not found in user's watchlist")
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
        }
    }


    /**
     * Gets all watched game thresholds present in the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * Returns the result as a list of games as a list of metadata, as game information is not
     * completely stored on the database (i.e. name and price)
     */
    suspend fun getAllWatchedGames(): List<Map<String, Any>>? = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        val db = FirebaseManager.getInstance().firestore

        try {
            val result = db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .collection(WATCHLIST_SUBCOLLECTION_PATH)
                .get()
                .await()

            val gameData = mutableListOf<Map<String, Any>>()

            for (doc in result) {
                gameData.add(doc.data)
                Log.d(TAG,"found ${doc.data["appId"]}")
            }

            return@withContext gameData
        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext null
        }

    }


    /**
     * Gets watched game threshold present in the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If game is not in user's watchlist, returns a value of -1
     * Always returns Long from database, refer to link:
     * https://firebase.google.com/docs/firestore/manage-data/add-data
     * quote: "Cloud Firestore always stores numbers as doubles" though this doesn't explain why
     * Long is returned, maybe because it was Int that was pushed to database?
     */
    suspend fun getThresholdForGame(appId: Int): Long? = withContext(Dispatchers.IO) {
        assertTokensLoaded()

        val db = FirebaseManager.getInstance().firestore

        try {
            val result = db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .collection(WATCHLIST_SUBCOLLECTION_PATH)
                .document(appId.toString())
                .get()
                .await()

            if (result.exists()) {
                Log.d(TAG, "game $appId had a threshold of ${result["threshold"]}")
                return@withContext result["threshold"] as Long
            } else {
                Log.d(TAG, "game $appId not in user's watchlist")
                return@withContext -1
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext null
        }
    }
}