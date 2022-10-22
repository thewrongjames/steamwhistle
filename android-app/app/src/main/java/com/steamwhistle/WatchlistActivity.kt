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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.launch

class WatchlistActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WatchlistActivity"
        const val GAME_EXTRA_ID = "game"
    }

    private val viewModel: WatchlistViewModel by viewModels()

    private lateinit var messagingService: WhistleMessagingService
    private lateinit var adapter: GameAdapter

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        recyclerView = findViewById(R.id.watchlistList)

        adapter = GameAdapter()
        adapter.onLongPress = {
            showDeleteAlertDialog(it)
        }

        adapter.onWatchlistGameClickListener = { game ->
            val intent = Intent(this@WatchlistActivity,GameDetailsActivity::class.java)
            intent.putExtra(GAME_EXTRA_ID,game)
            startActivity(intent)
        }

        viewModel.games.observe(this) { games -> adapter.submitList(games) }
        recyclerView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(recyclerView.context, R.drawable.divider)!!)
        recyclerView.addItemDecoration(dividerItemDecoration)

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
        messagingService.addTokenListener(viewModel.viewModelScope)
        messagingService.onData = onData@{data ->
            val appIdString = data["appId"]
            val priceString = data["currentPrice"]

            if (appIdString == null || priceString == null) {
                Log.e(TAG, "Failed to extract data from FCM message (1).")
                return@onData
            }

            val appId: Int
            val price: Int

            try {
                appId = appIdString.toInt()
            } catch (error: java.lang.NumberFormatException) {
                Log.e(TAG, "Failed to extract data from FCM message (2).")
                return@onData
            }

            try {
                price = priceString.toInt()
            } catch (error: java.lang.NumberFormatException) {
                Log.e(TAG, "Failed to extract data from FCM message (3).")
                return@onData
            }

            Log.i(TAG, "Updating watchlist from notification")
            viewModel.viewModelScope.launch { viewModel.updatePrice(appId, price) }
        }
        messagingService.onNotification = {notificationString ->
            Toast.makeText(this, notificationString, Toast.LENGTH_LONG)
        }

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
                Toast.makeText(
                    this@WatchlistActivity,
                    getString(R.string.game_already_added,
                        addedGame.name
                    ), Toast.LENGTH_LONG).show()
            } else {
                // Update firebase.
                // TODO: Do this in a worker.
                val (updatedSeconds, updatedNanos) = addedGame.getUpdatedSecondsAndNanos()
                val (createdSeconds, createdNanos) = addedGame.getCreatedSecondsAndNanos()
                SteamWhistleRemoteDatabase.addOrUpdateWatchlistGame(
                    appId = addedGame.appId,
                    threshold = addedGame.threshold,
                    updatedSeconds = updatedSeconds,
                    updatedNanos = updatedNanos,
                    createdSeconds = createdSeconds,
                    createdNanos = createdNanos,
                )
                Toast.makeText(
                    this@WatchlistActivity,
                    getString(R.string.game_added, addedGame.name),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun removeGame(game: WatchlistGame) {
        viewModel.viewModelScope.launch {
            viewModel.removeGame(game)
        }
    }

    private fun showDeleteAlertDialog(game: WatchlistGame) {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to remove ${game.name} from your watchlist?")
            .setPositiveButton(R.string.yes) {_, _ ->
                removeGame(game)
            }
            .setNegativeButton(R.string.no){_, _ -> }
            .create()
            .show()
    }
}