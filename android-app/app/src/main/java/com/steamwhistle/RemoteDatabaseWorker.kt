package com.steamwhistle

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf

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

        const val METHOD_LOAD_USER_TOKEN = "loadUserToken"
        const val METHOD_ADD_OR_UPDATE_WATCHLIST_GAME = "addOrUpdateWatchlistGame"
        const val METHOD_REMOVE_GAME_FROM_WATCHLIST = "removeGameFromWathclist"
        const val METHOD_GET_ALL_WATCHED_GAMES = "getAllWatchedGames"
        const val METHOD_GET_THRESHOLD_FOR_GAME = "getThresholdForGame"
        const val METHOD_ADD_DEVICE = "addDevice"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "adding task to queue")

        // Retrieve params needed for database function calls
        // We cannot pass objects into inputData, so method is a String
        val method: String? = inputData.getString(KEY_METHOD)
        val token: String? = inputData.getString(KEY_UID)
        val appId: Int = inputData.getInt(KEY_APP_ID,-1)
        val threshold: Int = inputData.getInt(KEY_THRESHOLD, -1)
        val updatedSeconds: Long = inputData.getLong(KEY_UPDATED_SECONDS, -1)
        val updatedNanos: Int = inputData.getInt(KEY_UPDATED_NANOS, -1)
        val createdSeconds: Long = inputData.getLong(KEY_CREATED_SECONDS, -1)
        val createdNanos: Int = inputData.getInt(KEY_CREATED_NANOS, -1)
        val deviceId: String? = inputData.getString(KEY_DEVICE_ID)

        var result: Any? = null

        // Do the work here, do a switch-case to determine which call to make
        // Make checks to ensure input parameters are valid, if not, do not execute
        // TODO: could throw errors instead of silently failing
        when (method) {
            METHOD_LOAD_USER_TOKEN -> {
                if (token != null) SteamWhistleRemoteDatabase.loadUid(token)
            }
            METHOD_ADD_OR_UPDATE_WATCHLIST_GAME -> {
                if (
                    appId >= 0
                    && threshold >= 0
                    && updatedSeconds >= 0
                    && updatedNanos >= 0
                    && createdSeconds >= 0
                    && createdNanos >= 0
                ) {
                    Log.i(TAG, "Adding or updating game $appId")
                    SteamWhistleRemoteDatabase.addOrUpdateWatchlistGame(
                        appId = appId,
                        threshold = threshold,
                        updatedSeconds = updatedSeconds,
                        updatedNanos = updatedNanos,
                        createdSeconds = createdSeconds,
                        createdNanos = createdNanos,
                    )
                } else {
                    Log.e(TAG, "Could not add or update game $appId")
                }
            }
            METHOD_REMOVE_GAME_FROM_WATCHLIST -> {
                if (appId >= 0)
                    SteamWhistleRemoteDatabase.removeGameFromWatchList(appId)
            }
            METHOD_GET_ALL_WATCHED_GAMES -> result = SteamWhistleRemoteDatabase.getAllWatchedGames()
            METHOD_GET_THRESHOLD_FOR_GAME -> {
                if (appId >= 0)
                    result = SteamWhistleRemoteDatabase.getThresholdForGame(appId)
            }
            METHOD_ADD_DEVICE -> {
                if (deviceId != null) {
                    Log.i(TAG, "Adding device $deviceId")
                    SteamWhistleRemoteDatabase.addDevice(deviceId)
                } else {
                    Log.e(TAG, "Could not add device: got null device ID")
                }
            }
        }

        val output: Data = workDataOf("result" to result)

        // Indicate whether the work finished successfully with the Result
        return Result.success(output)
    }
}