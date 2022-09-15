package com.steamwhistle

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
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
        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
    }
}