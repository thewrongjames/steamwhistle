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
    }

    override suspend fun doWork(): Result {

        Log.i(TAG, "adding task to queue")

        // Retrieve params needed for database function calls
        // We cannot pass objects into inputData, so method is a String
        val method: String? = inputData.getString("method")
        val token: String? = inputData.getString("token")
        val appId: Int = inputData.getInt("appId",-1)
        val threshold: Int = inputData.getInt("threshold", -1)
        val updatedSeconds: Long = inputData.getLong("updatedSeconds", -1)
        val updatedNanos: Int = inputData.getInt("updatedNanos", -1)
        val createdSeconds: Long = inputData.getLong("createdSeconds", -1)
        val createdNanos: Int = inputData.getInt("createdNanos", -1)

        var result: Any? = null

        // Do the work here, do a switch-case to determine which call to make
        // Make checks to ensure input parameters are valid, if not, do not execute
        // TODO: could throw errors instead of silently failing
        when (method) {
            "loadUserToken" -> {
                if (token != null) SteamWhistleRemoteDatabase.loadUserToken(token)
            }
            "addGameToWatchList" -> {
                if (
                    appId >= 0
                    && threshold >= 0
                    && updatedSeconds >= 0
                    && updatedNanos >= 0
                    && createdSeconds >= 0
                    && createdNanos >= 0
                ) {
                    SteamWhistleRemoteDatabase.addOrUpdateWatchlistGame(
                        appId = appId,
                        threshold = threshold,
                        updatedSeconds = updatedSeconds,
                        updatedNanos = updatedNanos,
                        createdSeconds = createdSeconds,
                        createdNanos = createdNanos,
                    )
                }
            }
            "removeGameFromWatchList" -> {
                if (appId >= 0)
                    SteamWhistleRemoteDatabase.removeGameFromWatchList(appId)
            }
            "getAllWatchedGames" -> result = SteamWhistleRemoteDatabase.getAllWatchedGames()
            "getThresholdForGame" -> {
                if (appId >= 0)
                    result = SteamWhistleRemoteDatabase.getThresholdForGame(appId)
            }
        }

        val output: Data = workDataOf("result" to result)

        // Indicate whether the work finished successfully with the Result
        return Result.success(output)
    }
}