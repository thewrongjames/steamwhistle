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

        var result: Any? = null

        // Do the work here, do a switch-case to determine which call to make
        // Make checks to ensure input parameters are valid, if not, do not execute
        // TODO: could throw errors instead of silently failing
        when (method) {
            "loadDeviceToken" -> {
                if (token != null) SteamWhistleRemoteDatabase.loadDeviceToken(token)
            }
            "loadUserToken" -> {
                if (token != null) SteamWhistleRemoteDatabase.loadUserToken(token)
            }
            "loadUserDeviceToDatabase" -> SteamWhistleRemoteDatabase.loadUserDeviceToDatabase()
            "getUser" -> result = SteamWhistleRemoteDatabase.getUser()
            "getDevice" -> result = SteamWhistleRemoteDatabase.getDevice()
            "addUser" -> SteamWhistleRemoteDatabase.addUser()
            "addDevice" -> SteamWhistleRemoteDatabase.addDevice()
            "addGameToWatchList" -> {
                if (appId >= 0 && threshold >= 0)
                    SteamWhistleRemoteDatabase.addGameToWatchList(appId, threshold)
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