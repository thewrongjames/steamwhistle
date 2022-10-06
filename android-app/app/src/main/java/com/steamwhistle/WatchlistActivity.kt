package com.steamwhistle

import android.app.AlertDialog
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView

class WatchlistActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WatchlistActivity"
    }

    private val watchedGames: ArrayList<Game> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        watchedGames.add(WatchlistGame(220, "Half-Life 2", 4200, 2000))
        watchedGames.add(WatchlistGame(1092790, "Inscryption", 4200, 2000))
        watchedGames.add(WatchlistGame(221910, "The Stanley Parable", 4200, 2000))
        watchedGames.add(WatchlistGame(400, "Portal", 4200, 2000))
        watchedGames.add(WatchlistGame(620, "Portal 2", 4200, 2000))
        watchedGames.add(WatchlistGame(257510, "The Talos Principle", 4200, 2000))
        watchedGames.add(WatchlistGame(72850, "The Elder Scrolls V: Skyrim", 4200, 2000))
        watchedGames.add(WatchlistGame(433340, "Slime Rancher", 4200, 2000))
        watchedGames.add(WatchlistGame(2210, "Quake 4", 4200, 2000))
        watchedGames.add(WatchlistGame(9200, "RAGE", 4200, 2000))
        watchedGames.add(WatchlistGame(17460, "Mass Effect (2007)", 4200, 2000))
        watchedGames.add(WatchlistGame(22300, "Fallout 3", 4200, 2000))
        watchedGames.add(WatchlistGame(24010, "Train Simulator Classic", 4200, 2000))

        val adapter = GameAdapter()
        adapter.submitList(watchedGames)
        adapter.onItemClickListener = { position ->
            Log.i(TAG, "Clicked item at position $position")

            AlertDialog.Builder(this)
                .setMessage("Not implemented.")
                .setPositiveButton(R.string.okay) {_, _ -> }
                .create()
                .show()
        }

        findViewById<RecyclerView>(R.id.watchlistList).adapter = adapter
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