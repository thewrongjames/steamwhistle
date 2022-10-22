package com.steamwhistle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AddToWatchlistActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AddToWatchlistActivity"
    }

    private var searchResults: List<Game> = ArrayList()
    private lateinit var adapter: GameAdapter
    private lateinit var recyclerView: RecyclerView

    private val viewModel: AddToWatchlistViewModel by viewModels()
    private lateinit var searchText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_to_watchlist)

        recyclerView = findViewById(R.id.addToWatchlistList)
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(recyclerView.context, R.drawable.divider)!!)
        recyclerView.addItemDecoration(dividerItemDecoration)

        searchText = findViewById(R.id.addToWatchlistSearchText)
        findViewById<EditText>(R.id.addToWatchlistSearchText).setOnEditorActionListener {
            view, _, _ ->
            onSearch(view)
            true
        }

        adapter = GameAdapter()
        adapter.submitList(searchResults)
        adapter.onGameClickListener = { game ->
            // TODO: Get the threshold from the user.
            val threshold = 2000
            val watchlistGame = WatchlistGame(
                game.appId,
                game.name,
                1000,
                threshold
            )

            val intent = Intent()
            intent.putExtra(WatchlistActivity.GAME_EXTRA_ID, watchlistGame)
            setResult(RESULT_OK, intent)
            finish()
        }

        recyclerView.adapter = adapter
    }

    fun onBackClick(view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun onSearch(view: View) {
        viewModel.viewModelScope.launch {
            searchResults = viewModel.searchGames(searchText.text.toString())
            adapter.submitList(searchResults)
            Log.i(TAG, searchResults.toString())
        }
    }
}