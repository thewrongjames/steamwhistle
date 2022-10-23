package com.steamwhistle

import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SteamWhistleRemoteDatabase {
    private const val TAG = "SteamWhistleRemoteDatabase"
    private const val USERS_COLLECTION_PATH = "users"
    private const val GAMES_COLLECTION_PATH = "games"
    private const val DEVICES_SUBCOLLECTION_PATH = "devices"
    private const val WATCHLIST_SUBCOLLECTION_PATH = "watchlist"

    private lateinit var userId: String

    // Callback for Auth service to load most recent user token for preparation of payload
    fun loadUid(newUid: String) {
        userId = newUid
        Log.d(TAG, "userId token loaded: $userId")
    }

    // This check runs prior to every call to the Firestore database
    private fun assertUidLoaded() {
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
        assertUidLoaded()

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
        assertUidLoaded()

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
        appId: Long,
        threshold: Long,
        updatedSeconds: Long,
        updatedNanos: Int,
        createdSeconds: Long,
        createdNanos: Int,
        isActive: Boolean,
    ) = withContext(Dispatchers.IO) {
        assertUidLoaded()

        if (threshold < 0) {
            throw IllegalArgumentException("watchlist threshold cannot be negative")
        }

        val db = FirebaseManager.getInstance().firestore

        // Check if game exists, if so, update timestamp rather than create timestamp
        val result = db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .collection(WATCHLIST_SUBCOLLECTION_PATH)
            .document(appId.toString()).get().await()

        val localUpdated = Timestamp(updatedSeconds, updatedNanos)
        val localCreated = Timestamp(createdSeconds, createdNanos)
        val data = mutableMapOf(
            "appId" to appId,
            "threshold" to threshold,
            "updated" to localUpdated,
            "created" to localCreated,
            "isActive" to isActive,
        )

        // TODO: Only update firebase if the firebase record is older.

        if (result.exists()) {
            Log.d(TAG, "Game $appId already watched by user, updating data")

            val firebaseUpdated = result.data?.get("updated")
            val firebaseCreated = result.data?.get("created")

            if (firebaseUpdated is Timestamp && firebaseUpdated.compareTo(localUpdated) > 1) {
                Log.i(TAG, "The firebase record is newer than the local record, not updating.")
                return@withContext
            }

            // If there is a created time on firebase, maintain that.
            if (firebaseCreated is Timestamp) {
                data["created"] = firebaseCreated
            }
        }

        db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .collection(WATCHLIST_SUBCOLLECTION_PATH)
            .document(appId.toString())
            .set(data)
    }

    /**
     * Get the firebase games in the given list of [appIds] from firestore. Returns a list of
     * retrieved games. Does not require the uid to be loaded. The map is not guaranteed to to have
     * one entry for each given appId.
     */
    suspend fun getGames(appIds: List<Long>): List<FirebaseGame> = withContext(
        Dispatchers.IO
    ) {
        // Making a firebase query with an empty array as the "in" query throws an exception.
        // Fortunately, if we are looking for 0 appIds, we don't really need to make the query, do
        // we.
        if (appIds.isEmpty()) return@withContext listOf<FirebaseGame>()

        val db = FirebaseManager.getInstance().firestore
        val firebaseResult = db.collection(GAMES_COLLECTION_PATH)
            .whereIn("appId", appIds)
            .get()
            .await()

        val result = mutableListOf<FirebaseGame>()

        for (document in firebaseResult) {
            val appId = document.get("appId")
            val name = document.get("name")
            val priceData = document.get("priceData")
            var price: Long? = null

            if (priceData is HashMap<*, *>) {
                val uncheckedPrice = priceData["final"]
                if (uncheckedPrice is Long) {
                    price = uncheckedPrice
                }
            }

            if (appId !is Long || name !is String || price == null) {
                Log.e(TAG, "Got unexpected game response from firebase: $appId, $name, $priceData")
                continue
            }

            result.add(FirebaseGame(appId, name, price))
        }

        return@withContext result
    }

    /**
     * Get the firebase records for every game on this user's watchlist (including inactive ones).
     * This will error if the uid has not been loaded. It returns a list of
     * [FirebaseWatchlistItem]s.
     */
    suspend fun getWatchlistItems(): List<FirebaseWatchlistItem> = withContext(Dispatchers.IO) {
        assertUidLoaded()

        val db = FirebaseManager.getInstance().firestore
        val firebaseResult = db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .collection(WATCHLIST_SUBCOLLECTION_PATH)
            .get()
            .await()

        val result = mutableListOf<FirebaseWatchlistItem>()

        for (document in firebaseResult) {
            val appId = document.get("appId")
            val threshold = document.get("threshold")
            val updated = document.get("updated")
            val created = document.get("created")
            val isActive = document.get("isActive")

            if (
                appId !is Long
                || threshold !is Long
                || updated !is Timestamp
                || created !is Timestamp
                || isActive !is Boolean
            ) {
                Log.e(
                    TAG,
                    """
                    Got unexpected watchlist item response; appId: $appId; threshold: $threshold;
                    updated: $updated; created: $created; isActive: $isActive.
                    """.trimIndent()
                )
                continue
            }

            result.add(
                FirebaseWatchlistItem(
                appId,
                threshold,
                updated.seconds,
                updated.nanoseconds,
                created.seconds,
                created.nanoseconds,
                isActive
            )
            )
        }

        return@withContext result
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
    suspend fun getThresholdForGame(appId: Long): Long? = withContext(Dispatchers.IO) {
        assertUidLoaded()

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