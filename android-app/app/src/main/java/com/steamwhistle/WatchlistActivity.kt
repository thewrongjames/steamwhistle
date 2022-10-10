package com.steamwhistle

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class WatchlistActivity : AppCompatActivity() {
    companion object {
        internal const val TAG = "WatchlistActivity"
    }

    private val watchedGames: ArrayList<Game> = ArrayList()

    private var recyclerAdapter: WatchlistItemRecyclerAdapter? = null

    private lateinit var database: FirebaseDatabase
    private lateinit var messagingService: WhistleMessagingService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        watchedGames.add(Game("Half-Life 2"))
        watchedGames.add(Game("Inscryption"))
        watchedGames.add(Game("The Stanley Parable"))
        watchedGames.add(Game("Portal"))
        watchedGames.add(Game("Portal 2"))
        watchedGames.add(Game("The Talos Principle"))
        watchedGames.add(Game("The Elder Scrolls V: Skyrim"))
        watchedGames.add(Game("FTL: Faster THan Light"))
        watchedGames.add(Game("Age of Empires II"))
        watchedGames.add(Game("Slime Rancher"))

        recyclerAdapter = WatchlistItemRecyclerAdapter(watchedGames)
        recyclerAdapter?.onItemClickListener = { position ->
            Log.i(TAG, "Clicked item at position $position")

            AlertDialog.Builder(this)
                .setMessage("Not implemented.")
                .setPositiveButton(R.string.okay) {_, _ -> }
                .create()
                .show()
        }

        findViewById<RecyclerView>(R.id.watchlistList).adapter = recyclerAdapter

        // Init messaging service
        val channelName = getString(R.string.channel_name);
        val channelDesc = getString(R.string.channel_description);
        val channelId = getString(R.string.channel_id)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(channelId, channelName, importance)
            mChannel.description = channelDesc
            notificationManager.createNotificationChannel(mChannel)
        }

        messagingService = WhistleMessagingService()
        messagingService.addTokenListener()

        // Check if Google Play Services are available
        checkGooglePlayServices()

        // Init Firebase database
        database = Firebase.database
        val ref = database.getReference("games/57750/price")

        // Read from the database
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.value
                Log.d(TAG, "Value is: $value")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
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
        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
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

        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
    }
}