package com.steamwhistle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * A view model for managing the data and database connections of the watch list. The lifecycle of
 * a view model outlives that of an activity, because the activity is destroyed and recreated all
 * the time, for events such as screen rotations. This is an [AndroidViewModel] so that it can have
 * access to the [Application], which it needs so it can have a context to attach the database to,
 * because view models should never have references to any context other than the application, as
 * they will likely outlive them, causing memory leaks.
 */
class WatchlistViewModel(application: Application): AndroidViewModel(application) {
    private val database = SteamWhistleDatabase.getDatabase(application)
    private val dao = database.watchlistDao()
    private val workManager: WorkManager = WorkManager.getInstance(application)

    val games: LiveData<List<WatchlistGame>> = dao.getWatchlistGames()

    /**
     * Save the given [watchlistGame]. Return true if the game is successfully inserted, and false
     * if the game already exists (or violates some other SQL constraint). Any other exceptional
     * cases should cause errors.
     */
    suspend fun saveGame(watchlistGame: WatchlistGame): Boolean {
        return withContext(Dispatchers.IO) {
            val result = dao.addGame(watchlistGame)

            // If we added the game, add it on firebase.
            if (result) {
                val (updatedSeconds, updatedNanos) = watchlistGame.getUpdatedSecondsAndNanos()
                val (createdSeconds, createdNanos) = watchlistGame.getCreatedSecondsAndNanos()

                addTaskToQueue(workDataOf(
                    "method" to "addOrUpdateWatchlistGame",
                    "appId" to watchlistGame.appId,
                    "threshold" to watchlistGame.threshold,
                    "updatedSeconds" to updatedSeconds,
                    "updatedNanos" to updatedNanos,
                    "createdSeconds" to createdSeconds,
                    "createdNanos" to createdNanos,
                ))
            }

            return@withContext result
        }
    }

    suspend fun removeGame(watchlistGame: WatchlistGame) {
        withContext(Dispatchers.IO) { dao.removeGame(watchlistGame.appId) }
    }

    suspend fun updatePrice(appId: Int, price: Int) {
        withContext(Dispatchers.IO) {dao.updatePrice(appId, price)}
    }

    suspend fun updateGame(watchlistGame: WatchlistGame) {
        watchlistGame.updated = ZonedDateTime.now()
        withContext(Dispatchers.IO) { dao.updateGame(watchlistGame) }

        // Update the game on firebase.
        val (updatedSeconds, updatedNanos) = watchlistGame.getUpdatedSecondsAndNanos()
        val (createdSeconds, createdNanos) = watchlistGame.getCreatedSecondsAndNanos()

        addTaskToQueue(workDataOf(
            "method" to "addOrUpdateWatchlistGame",
            "appId" to watchlistGame.appId,
            "threshold" to watchlistGame.threshold,
            "updatedSeconds" to updatedSeconds,
            "updatedNanos" to updatedNanos,
            "createdSeconds" to createdSeconds,
            "createdNanos" to createdNanos,
        ))
    }

    // DEMO on how to use WorkManager
    // Suppose I want to fetch threshold for a game...
//        val exampleAppId = 666
//        val task = addTaskToQueue(
//            workDataOf(
//                "method" to "getThresholdForGame",
//                "token" to null,                        // not needed for getThresholdForGame
//                "appId" to exampleAppId,
//                "threshold" to null                     // not needed for getThresholdForGame
//            )
//        )

    // Get the result and read
    // https://developer.android.com/topic/libraries/architecture/workmanager/advanced
//        workManager?.getWorkInfoByIdLiveData(task.id)?.observe(this, Observer { info ->
//            if (info != null && info.state.isFinished) {
//                val result = info.outputData.getLong("result",-1)
//                Log.i(TAG, "enqueued task is complete, result from Firestore is " +
//                        "a threshold of $result for app: $exampleAppId")
//
//                val text = "Enqueued task complete, result from Firestore: " +
//                        "threshold: $result for app: $exampleAppId"
//                val duration = Toast.LENGTH_LONG
//                val toast = Toast.makeText(applicationContext, text, duration)
//                toast.show()
//            }
//        })

    /**
     * Wrapper function to add a task to the WorkManager
     * Need to define a class overriding the Worker class and pass this to the request builder
     * In this example, it is using a Worker handling database function calls
     */
    private fun addTaskToQueue(args: Data): OneTimeWorkRequest {
        // Define the constraints as per Google docs
        // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work#work-constraints
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)                   // this halts if battery is low
            // (typically < 15%)
            .setRequiredNetworkType(NetworkType.UNMETERED)    // this forces it to use WiFi or
            // or some other unmetered network
            .build()


        // Build request using our custom class and add parameters
        // See same link above for examples
        val workRequest =
            OneTimeWorkRequestBuilder<RemoteDatabaseWorker>()
                .setInputData(
                    Data.Builder()
                        .putAll(args)
                        .build()
                )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS)
                .setInitialDelay(100, TimeUnit.MILLISECONDS) // Short delay
                .build()

        workManager.enqueue(workRequest)

        return workRequest
    }
}