package com.steamwhistle

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WatchlistActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WatchlistActivity"
    }

    private val viewModel: WatchlistViewModel by viewModels()

    private lateinit var database: FirebaseDatabase
    private lateinit var messagingService: WhistleMessagingService
    private var workManager: WorkManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        val adapter = GameAdapter()
        adapter.onItemClickListener = { position ->
            Log.i(TAG, "Clicked item at position $position")

            AlertDialog.Builder(this)
                .setMessage("Not implemented.")
                .setPositiveButton(R.string.okay) {_, _ -> }
                .create()
                .show()
        }
        viewModel.games.observe(this) { games -> adapter.submitList(games) }
        findViewById<RecyclerView>(R.id.watchlistList).adapter = adapter

        // Init the WorkManager
        workManager = WorkManager.getInstance(applicationContext)

        // Init messaging service
        val channelName = getString(R.string.channel_name);
        val channelDesc = getString(R.string.channel_description);
        val channelId = getString(R.string.channel_id)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(channelId, channelName, importance)
        mChannel.description = channelDesc
        notificationManager.createNotificationChannel(mChannel)

        messagingService = WhistleMessagingService()
        messagingService.addTokenListener()

        // Check if Google Play Services are available
        checkGooglePlayServices()

        // Example: get intent extras from FCM notification click
        // TODO: Do something with this data, change payload in FCM script
        if (intent.extras != null) {
            for (key in intent.extras!!.keySet()) {
                if (key == "appName" ||
                    key == "appId" ||
                    key == "currentPrice" ||
                    key == "threshold")
                {
                    val value = intent.extras!!.getString(key)
                    Log.d(TAG, "Key: $key Value: $value")
                }
            }
        }

        // DEMO on how to use WorkManager
        // Suppose I want to fetch threshold for a game...
        val exampleAppId = 666
        val task = addTaskToQueue(
            workDataOf(
                "method" to "getThresholdForGame",
                "token" to null,                        // not needed for getThresholdForGame
                "appId" to exampleAppId,
                "threshold" to null                     // not needed for getThresholdForGame
            )
        )

        // Get the result and read
        // https://developer.android.com/topic/libraries/architecture/workmanager/advanced
        workManager!!.getWorkInfoByIdLiveData(task.id)
            .observe(this, Observer { info ->
                if (info != null && info.state.isFinished) {
                    val result = info.outputData.getLong("result",-1)
                    Log.i(TAG, "enqueued task is complete, result from Firestore is " +
                            "a threshold of $result for app: $exampleAppId")

                    val text = "Enqueued task complete, result from Firestore: " +
                            "threshold: $result for app: $exampleAppId"
                    val duration = Toast.LENGTH_LONG
                    val toast = Toast.makeText(applicationContext, text, duration)
                    toast.show()
                }
            })

    }

    /*
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

        workManager!!.enqueue(workRequest)

        return workRequest
    }

    /*
     * Google Play Services is required for Firebase Messaging, so this function checks if
     * the Play Services is installed and active on the Android device. If not, it tries to
     * make it active so FCM can function correctly
     */
    private fun checkGooglePlayServices() {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        // Try to make Google Play Services available if initially not successful
        if (status != ConnectionResult.SUCCESS) {
            val res = GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
            res.addOnSuccessListener { Log.i(WatchlistActivity.TAG, "success ${res.result})") }
            res.addOnFailureListener{ Log.i(WatchlistActivity.TAG, "fail ${res.result})") }
        }

        // DEBUG - print the result
        Log.i(WatchlistActivity.TAG, "status: $status, ${ConnectionResult.SUCCESS}")
    }

    fun onSettingsClick(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onAddClick(view: View) {
        val intent = Intent(this, AddToWatchlistActivity::class.java)
        handleAddGameResponse.launch(intent)
    }

    private val handleAddGameResponse = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) handleAddGameResult@{ result: ActivityResult? ->
        if (result == null) {
            Log.e(TAG, "handleAddGameResponse got null result")
            return@handleAddGameResult
        }
        if (result.resultCode == RESULT_CANCELED) {
            Log.i(TAG, "handleAddGameResponse got cancelled result")
            return@handleAddGameResult
        }
        if (result.resultCode != RESULT_OK) {
            Log.e(TAG, "handleAddGameResponse got non-cancelled, non-ok result")
            return@handleAddGameResult
        }

        val intent = result.data
        if (intent == null) {
            Log.e(TAG, "handleAddGameResponse got null result.data")
            return@handleAddGameResult
        }

        // TODO: Make "game" not a magic number.
        val addedGame: WatchlistGame? = if (android.os.Build.VERSION.SDK_INT < 33) {
            // This is deprecated, but the replacement (below) only works in API 33 and up, which
            // is the newest, so I don't really want to force the minSDK up to that.
            intent.getParcelableExtra("game")
        } else {
            intent.getParcelableExtra("game", WatchlistGame::class.java)
        }

        if (addedGame == null) {
            Log.e(TAG, "handleAddGameResponse got a null \"game\" extra")
            return@handleAddGameResult
        }

        viewModel.viewModelScope.launch {
            val successfullySaved = viewModel.saveGame(addedGame)
            if (!successfullySaved) {
                Toast.makeText(this@WatchlistActivity, getString(R.string.game_already_added, addedGame.name), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@WatchlistActivity, getString(R.string.game_added, addedGame.name), Toast.LENGTH_LONG).show()
            }
        }
    }
}