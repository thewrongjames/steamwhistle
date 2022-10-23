package com.steamwhistle

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class RemoteDatabaseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        private const val TAG = "RemoteDatabaseWorker"

        const val KEY_METHOD = "method"
        const val KEY_UID = "uid"
        const val KEY_APP_ID = "appId"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_UPDATED_SECONDS = "updatedSeconds"
        const val KEY_UPDATED_NANOS = "updatedNanos"
        const val KEY_CREATED_SECONDS = "createdSeconds"
        const val KEY_CREATED_NANOS = "createdNanos"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_IS_ACTIVE = "isActive"
        const val KEY_APP_IDS = "appIds"

        const val METHOD_LOAD_USER_TOKEN = "loadUserToken"
        const val METHOD_ADD_OR_UPDATE_WATCHLIST_GAME = "addOrUpdateWatchlistGame"
        const val METHOD_GET_THRESHOLD_FOR_GAME = "getThresholdForGame"
        const val METHOD_ADD_DEVICE = "addDevice"
        const val METHOD_GET_GAMES = "getGames"
        const val METHOD_GET_WATCHLIST_ITEMS = "getWatchlistItems"

        const val RESULT_KEY = "result"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "adding task to queue")

        // Retrieve params needed for database function calls
        // We cannot pass objects into inputData, so method is a String
        val method: String? = inputData.getString(KEY_METHOD)
        val token: String? = inputData.getString(KEY_UID)
        val appId: Long = inputData.getLong(KEY_APP_ID,-1)
        val threshold: Long = inputData.getLong(KEY_THRESHOLD, -1)
        val updatedSeconds: Long = inputData.getLong(KEY_UPDATED_SECONDS, -1)
        val updatedNanos: Int = inputData.getInt(KEY_UPDATED_NANOS, -1)
        val createdSeconds: Long = inputData.getLong(KEY_CREATED_SECONDS, -1)
        val createdNanos: Int = inputData.getInt(KEY_CREATED_NANOS, -1)
        val isActive: Boolean? = if (
            inputData.hasKeyWithValueOfType<Boolean>(KEY_IS_ACTIVE)
        ) inputData.getBoolean(KEY_IS_ACTIVE, false) else null
        val deviceId: String? = inputData.getString(KEY_DEVICE_ID)
        val appIds: LongArray? = inputData.getLongArray(KEY_APP_IDS)

        // Do the work here, do a switch-case to determine which call to make
        // Make checks to ensure input parameters are valid, if not, do not execute
        return when (method) {
            METHOD_GET_GAMES -> {
                if (appIds != null) {
                    val serialised = SteamWhistleRemoteDatabase.getGames(appIds.toList()).map {
                        firebaseGame -> Json.encodeToString(firebaseGame)
                    }
                    val array: Array<String> = serialised.toTypedArray()

                    if (array.size != appIds.size) {
                        Log.e(TAG, "Got ${array.size} games from firebase, expected ${appIds.size}")
                        Result.failure()
                    } else {
                        Result.success(workDataOf(RESULT_KEY to array))
                    }
                } else {
                    Log.e(TAG, "Got null appIds")
                    Result.failure()
                }
            }
            METHOD_GET_WATCHLIST_ITEMS -> {
                val serialised: List<String> = SteamWhistleRemoteDatabase.getWatchlistItems().map {
                    firebaseWatchlistItem -> Json.encodeToString(firebaseWatchlistItem)
                }
                val array: Array<String> = serialised.toTypedArray()
                Result.success(workDataOf(RESULT_KEY to array))
            }
            METHOD_LOAD_USER_TOKEN -> {
                if (token != null) {
                    SteamWhistleRemoteDatabase.loadUid(token)
                    Result.success()
                } else {
                    Log.e(TAG, "Did not receive user token.")
                    Result.failure()
                }
            }
            METHOD_ADD_OR_UPDATE_WATCHLIST_GAME -> {
                if (
                    appId >= 0
                    && threshold >= 0
                    && updatedSeconds >= 0
                    && updatedNanos >= 0
                    && createdSeconds >= 0
                    && createdNanos >= 0
                    && isActive != null
                ) {
                    Log.i(TAG, "Adding or updating game $appId")
                    SteamWhistleRemoteDatabase.addOrUpdateWatchlistGame(
                        appId = appId,
                        threshold = threshold,
                        updatedSeconds = updatedSeconds,
                        updatedNanos = updatedNanos,
                        createdSeconds = createdSeconds,
                        createdNanos = createdNanos,
                        isActive = isActive,
                    )
                    Result.success()
                } else {
                    Log.e(TAG, "Could not add or update game $appId, $isActive")
                    Result.failure()
                }
            }
            METHOD_GET_THRESHOLD_FOR_GAME -> {
                if (appId >= 0) {
                    Result.success(workDataOf(
                        RESULT_KEY to SteamWhistleRemoteDatabase.getThresholdForGame(appId)
                    ))
                } else {
                    Result.failure()
                }
            }
            METHOD_ADD_DEVICE -> {
                if (deviceId != null) {
                    Log.i(TAG, "Adding device $deviceId")
                    SteamWhistleRemoteDatabase.addDevice(deviceId)
                    Result.success()
                } else {
                    Log.e(TAG, "Could not add device: got null device ID")
                    Result.failure()
                }
            }
            else -> {
                Log.e(TAG, "Unknown method")
                Result.failure()
            }
        }
    }
}