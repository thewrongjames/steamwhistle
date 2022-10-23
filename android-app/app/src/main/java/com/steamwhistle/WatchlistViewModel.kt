package com.steamwhistle

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.WorkInfo.State
import com.google.android.gms.tasks.OnCompleteListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * A view model for managing the data and database connections of the watch list. The lifecycle of
 * a view model outlives that of an activity, because the activity is destroyed and recreated all
 * the time, for events such as screen rotations. This is an [AndroidViewModel] so that it can have
 * access to the [Application], which it needs so it can have a context to attach the database to,
 * because view models should never have references to any context other than the application, as
 * they will likely outlive them, causing memory leaks.
 */
class WatchlistViewModel(application: Application): AndroidViewModel(application) {
    companion object {
        private const val TAG = "WatchlistViewModel"
    }

    private val database = SteamWhistleDatabase.getDatabase(application)
    private val dao = database.watchlistDao()

    private val workManager: WorkManager = WorkManager.getInstance(application)

    private val messaging = FirebaseManager.getInstance().messaging

    val games: LiveData<List<WatchlistGame>> = dao.getActiveWatchlistGames()

    /**
     * Save the given [watchlistGame]. Return true if the game is successfully inserted, and false
     * if the game already exists (or violates some other SQL constraint). Any other exceptional
     * cases should cause errors.
     */
    suspend fun addGame(watchlistGame: WatchlistGame): Boolean {
        watchlistGame.updated = ZonedDateTime.now()

        return withContext(Dispatchers.IO) {
            val result = dao.addGame(watchlistGame)

            // If we added the game, add it on firebase.
            if (result) {
                addOrUpdateWatchlistGameOnFirebase(watchlistGame)
            }

            return@withContext result
        }
    }

    suspend fun removeGame(watchlistGame: WatchlistGame) {
        watchlistGame.isActive = false
        watchlistGame.updated = ZonedDateTime.now()
        withContext(Dispatchers.IO) { dao.updateGame(watchlistGame) }
        addOrUpdateWatchlistGameOnFirebase(watchlistGame)
    }

    suspend fun updateGame(watchlistGame: WatchlistGame) {
        watchlistGame.updated = ZonedDateTime.now()
        withContext(Dispatchers.IO) { dao.updateGame((watchlistGame)) }
        addOrUpdateWatchlistGameOnFirebase(watchlistGame)
    }

    suspend fun attemptToUpdateLocalGameFromNotificationData(
        appIdString: String?,
        name: String?,
        priceString: String?,
    ) {
        withContext(Dispatchers.IO) {
            dao.attemptToUpdateLocalGameFromNotificationData(appIdString, name, priceString)
        }
    }

    fun addDeviceToFirebase() {
        messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Register the device Id on firestore.
            val deviceId = task.result
            Log.i(TAG, "Enqueuing task to add device $deviceId")

            addTaskToQueue(workDataOf(
                RemoteDatabaseWorker.KEY_METHOD to RemoteDatabaseWorker.METHOD_ADD_DEVICE,
                RemoteDatabaseWorker.KEY_DEVICE_ID to deviceId,
            ))
        })
    }

    private fun addOrUpdateWatchlistGameOnFirebase(watchlistGame: WatchlistGame) {
        // Update the game on firebase.
        val (updatedSeconds, updatedNanos) = watchlistGame.getUpdatedSecondsAndNanos()
        val (createdSeconds, createdNanos) = watchlistGame.getCreatedSecondsAndNanos()

        addTaskToQueue(workDataOf(
            RemoteDatabaseWorker.KEY_METHOD to RemoteDatabaseWorker.METHOD_ADD_OR_UPDATE_WATCHLIST_GAME,
            RemoteDatabaseWorker.KEY_APP_ID to watchlistGame.appId,
            RemoteDatabaseWorker.KEY_THRESHOLD to watchlistGame.threshold,
            RemoteDatabaseWorker.KEY_UPDATED_SECONDS to updatedSeconds,
            RemoteDatabaseWorker.KEY_UPDATED_NANOS to updatedNanos,
            RemoteDatabaseWorker.KEY_CREATED_SECONDS to createdSeconds,
            RemoteDatabaseWorker.KEY_CREATED_NANOS to createdNanos,
            RemoteDatabaseWorker.KEY_IS_ACTIVE to watchlistGame.isActive
        ))
    }

    /**
     * Update the games with the given [appIds] with their values from firebase.
     */
    private suspend fun updateGamesFromFirebase(lifecycleOwner: LifecycleOwner, appIds: List<Long>) {
        val appIdsArray: LongArray = appIds.toLongArray()

        val gamesTask = addTaskToQueue(workDataOf(
            RemoteDatabaseWorker.KEY_METHOD to RemoteDatabaseWorker.METHOD_GET_GAMES,
            RemoteDatabaseWorker.KEY_APP_IDS to appIdsArray,
        ))

        workManager.getWorkInfoByIdLiveData(gamesTask.id).observe(lifecycleOwner) { info ->
            if (info == null) {
                Log.i(TAG, "Games update task got null info")
                return@observe
            }
            if (!info.state.isFinished) {
                Log.i(TAG, "Games update task for unfinished info")
                return@observe
            }
            if (info.state == State.FAILED) {
                Log.e(TAG, "Games update task failed")
                return@observe
            }

            val result = info.outputData.getStringArray(RemoteDatabaseWorker.RESULT_KEY)
            if (result == null) {
                Log.e(TAG, "Games update task gave null result")
                return@observe
            }

            val firebaseGames: List<FirebaseGame> = result.toList().map { firebaseGameString ->
                try {
                    Json.decodeFromString(firebaseGameString)
                } catch (error: SerializationException) {
                    Log.e(TAG, "Could not deserialise games update result item $firebaseGameString")
                    return@observe
                }
            }

            for (firebaseGame in firebaseGames) {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.updateGameData(firebaseGame.appId, firebaseGame.name, firebaseGame.price)
                }
            }
        }
    }

    /**
     * Update our local data from firebase.
     * TODO: Do this somewhere less ephemeral than a view model scope using an activity lifecycle.
     */
    suspend fun updateDataFromFirebase(lifecycleOwner: LifecycleOwner) {
        val appIds: List<Long> = withContext(Dispatchers.IO) { dao.getActiveWatchlistGameIds() }
        updateGamesFromFirebase(lifecycleOwner, appIds)

        val watchlistItemsTask = addTaskToQueue(workDataOf(
            RemoteDatabaseWorker.KEY_METHOD to RemoteDatabaseWorker.METHOD_GET_WATCHLIST_ITEMS
        ))

        workManager.getWorkInfoByIdLiveData(watchlistItemsTask.id).observe(lifecycleOwner) {info ->
            if (info == null) {
                Log.i(TAG, "Games update task got null info")
                return@observe
            }
            if (!info.state.isFinished) {
                Log.i(TAG, "Games update task for unfinished info")
                return@observe
            }
            if (info.state == State.FAILED) {
                Log.e(TAG, "Games update task failed")
                return@observe
            }

            val result = info.outputData.getStringArray(RemoteDatabaseWorker.RESULT_KEY)
            if (result == null) {
                Log.e(TAG, "Games update task gave null result")
                return@observe
            }

            val firebaseWatchlistItems: List<FirebaseWatchlistItem> = result.toList().map {
                firebaseWatchlistString -> try {
                    Json.decodeFromString(firebaseWatchlistString)
                } catch (error: SerializationException) {
                    Log.e(
                        TAG,
                        "Could not deserialise watchlist update item $firebaseWatchlistString"
                    )
                    return@observe
                }
            }

            viewModelScope.launch {
                val newAppIds = mutableListOf<Long>()
                for (firebaseWatchlistItem in firebaseWatchlistItems) {
                    val isNew = withContext(Dispatchers.IO) {
                        dao.updateFromFirebaseIfNewer(firebaseWatchlistItem)
                    }
                    if (isNew) newAppIds.add(firebaseWatchlistItem.appId)
                }

                // Update the prices of the new games, because they will just be zero.
                updateGamesFromFirebase(lifecycleOwner, newAppIds)
            }
        }
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