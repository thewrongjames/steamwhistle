package com.steamwhistle

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SteamWhistleRemoteDatabase {
    private const val TAG = "SteamWhistleRemoteDatabase"
    private const val USERS_COLLECTION_PATH = "users"
    private const val GAMES_COLLECTION_PATH = "games" // Not currently used since games is updated
                                                      // via server-side scripts instead

    private lateinit var deviceId: String
    private lateinit var userId: String


    // Callback for FCM service to load most recent device token for preparation of payload
    fun loadDeviceToken(token: String) {
        deviceId = token
        Log.d(TAG, "deviceId token loaded: $deviceId")
    }

    // Callback for Auth service to load most recent user token for preparation of payload
    fun loadUserToken(token: String?) {
        userId = token!!
        Log.d(TAG, "userId token loaded: $userId")
    }

    // This check runs prior to every call to the Firestore database
    private fun checkTokensLoaded() {
        if (!::userId.isInitialized ||
            !::deviceId.isInitialized ||
            userId.isEmpty() ||
            deviceId.isEmpty()
        ) {
            throw IllegalStateException("userId or deviceId is empty, load the tokens first")
        }
    }

    private suspend fun checkUserInDatabase() {
        if (getUser() == null) {
            Log.d(TAG,"User not found, add user with device")
            throw IllegalStateException("user $userId does not exist in the database")
        }
    }

    /* Wrapper function to send device token to the database, based on the userId as the key
     * If the user is already added to the database, then it checks to see if the device token is
     * already in the list of devices. If so, this does nothing.
     *
     * Otherwise, it adds the device to the list of devices for that user.
     * If the user does not exist, it populates the basic information for that user and also
     * adds the current device to the list of devices
     */
    suspend fun registerDeviceToken() = withContext(Dispatchers.IO) {

        checkTokensLoaded()

        // Check user exists
        val result = getUser()

        if (result == null) {
            addUser()
        } else {
            addDevice()
        }

    }

    /* Gets user from database by loaded userId
     * Returns null if no match is found
     */
    suspend fun getUser(): User? = withContext(Dispatchers.IO) {

        checkTokensLoaded()

        val db = FirebaseFirestore.getInstance()

        try {
            val result = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()

            if (!result.exists()) {
                Log.d(TAG, "could not find user $userId")
                return@withContext null
            }

            return@withContext result.toObject(User::class.java)

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext null
        }

    }

    /* Gets device token from database for the authenticated user
     * If user is not found in the database, throws an Exception
     * If device is not found as part of user's device list, returns false
     * Else if found, returns true
     */
    suspend fun getDevice(): Boolean = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        try {
            val result = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()

            if (!result.exists()) {
                throw IllegalStateException("could not find device for user as user does not exist: $userId")
            } else {
                val user = result.toObject(User::class.java)
                return@withContext deviceId in user!!.devices
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext false
        }

    }

    /* Adds user to the remote database, based on loaded information.
     * Resultant user will always have at least one device associated with it
     *
     * Does not return any value, since Firestore does not return a snapshot to read from
     * after adding/setting. If you want to confirm user is added, call getUser() manually
     *
     * Refer to StackOverflow link:
     * https://stackoverflow.com/questions/61427687/how-to-get-document-after-set-method-in-firestore-while-updating
     */
    suspend fun addUser() = withContext(Dispatchers.IO) {

        checkTokensLoaded()

        val db = FirebaseFirestore.getInstance()

        try {

            val newUser = User(
                arrayListOf(deviceId),
                mutableMapOf()
            )

            db.collection(USERS_COLLECTION_PATH).document(userId).set(newUser)

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
        }
    }

    /* Adds a device token to the authenticated user's list of devices
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If the device is already present in the user's device list, nothing happens
     * Else, adds the device token to the user's list of devices in Firebase
     */
    suspend fun addDevice() = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        try {

            val user = getUser()

            if (user!!.devices.contains(deviceId)) {
                Log.d(TAG, "devices already in user devices, skipping")
            } else {
                user.devices.add(deviceId)
                db.collection(USERS_COLLECTION_PATH).document(userId).set(user)
                Log.d(TAG, "device id $deviceId for user $userId")
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
        }
    }

    /* Adds a game or updates threshold info for a game to the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * Checks if threshold is negative, if so, throws an Exception
     */
    suspend fun addGameToWatchList(game: WatchlistGame) = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        val user = getUser()
        val appId = game.appId.toString()
        val threshold = game.threshold.toInt()

        if (threshold < 0) {
            throw IllegalArgumentException("watchlist threshold cannot be negative")
        }

        // Add game to user object
        if (appId in user!!.watchlist) {
            Log.d(TAG, "game $appId already in user's watchlist, updating threshold info")
        }

        user.watchlist[appId] = threshold

        db.collection(USERS_COLLECTION_PATH).document(userId).set(user)
    }

    /* Removes a game from the user's watchlist if present
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If the game is not in the user's watchlist, then this function does nothing
     * Else, removes the matching appId and threshold info from the user's watchlist
     */
    suspend fun removeGameFromWatchList(game: WatchlistGame) = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        try {
            val result = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
            val user = result.toObject(User::class.java)
            val appId = game.appId.toString()

            if (appId in user!!.watchlist) {
                user.watchlist.remove(appId)
                db.collection(USERS_COLLECTION_PATH).document(userId).set(user)
            } else {
                Log.d(TAG, "game $appId to be removed was not found in user's watchlist")
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
        }

    }


    /* Gets all watched game thresholds present in the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     */
    suspend fun getAllWatchedGames(): MutableMap<String, Int>? = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        try {
            val result = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
            val user = result.toObject(User::class.java)

            return@withContext user!!.watchlist
        } catch (e: Exception) {
            Log.e(TAG, e.message.orEmpty())
            return@withContext null
        }

    }


    /* Gets all watched game thresholds present in the authenticated user's watchlist
     * If the user is not in the database (e.g. manual deletion) then an exception is thrown
     * If game is not in user's watchlist, returns a value of -1
     */
    suspend fun getThresholdForGame(game: WatchlistGame): Int? = withContext(Dispatchers.IO) {

        checkTokensLoaded()
        checkUserInDatabase()

        val db = FirebaseFirestore.getInstance()

        try {
            val result = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
            val user = result.toObject(User::class.java)
            val appId = game.appId.toString()

            if (appId in user!!.watchlist) {
                return@withContext user.watchlist[appId]
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