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

    private var recyclerAdapter: WatchlistItemRecyclerAdapter? = null

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