package com.steamwhistle

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView

class AddToWatchlistActivity : AppCompatActivity() {
    private val searchResults: ArrayList<Game> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_to_watchlist)

        findViewById<EditText>(R.id.addToWatchlistSearchText).setOnEditorActionListener {
            view, _, _ ->
            onSearch(view)
            true
        }

        searchResults.add(SearchResultGame(2210, "Quake 4", 4200))
        searchResults.add(SearchResultGame(220, "Half-Life 2", 4200))
        searchResults.add(SearchResultGame(9200, "RAGE", 4200))
        searchResults.add(SearchResultGame(1092790, "Inscryption", 4200))
        searchResults.add(SearchResultGame(17460, "Mass Effect (2007)", 4200))
        searchResults.add(SearchResultGame(221910, "The Stanley Parable", 4200))
        searchResults.add(SearchResultGame(22300, "Fallout 3", 4200))
        searchResults.add(SearchResultGame(400, "Portal", 4200))
        searchResults.add(SearchResultGame(24010, "Train Simulator Classic", 4200))
        searchResults.add(SearchResultGame(620, "Portal 2", 4200))
        searchResults.add(SearchResultGame(257510, "The Talos Principle", 4200))
        searchResults.add(SearchResultGame(72850, "The Elder Scrolls V: Skyrim", 4200))
        searchResults.add(SearchResultGame(433340, "Slime Rancher", 4200))

        val adapter = GameAdapter()
        adapter.submitList(searchResults)
        adapter.onItemClickListener = { position ->
            // TODO: Get the threshold from the user.
            val threshold = 2000
            val watchlistGame = WatchlistGame(
                searchResults[position].appId,
                searchResults[position].name,
                searchResults[position].price,
                threshold
            )

            val intent = Intent()
            intent.putExtra("game", watchlistGame)
            setResult(RESULT_OK, intent)
            finish()
        }

        findViewById<RecyclerView>(R.id.addToWatchlistList).adapter = adapter
    }

    fun onBackClick(view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun onSettingsClick(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onSearch(view: View) {
        AlertDialog.Builder(this)
            .setMessage("Not implemented.")
            .setPositiveButton(R.string.okay) {_, _ -> }
            .create()
            .show()
    }
}