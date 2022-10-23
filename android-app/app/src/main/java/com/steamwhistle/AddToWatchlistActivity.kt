package com.steamwhistle

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import java.time.ZonedDateTime
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL

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

        var client = OkHttpClient()

        adapter = GameAdapter()
        adapter.submitList(searchResults)
        adapter.onGameClickListener = { game ->
            // TODO: Get the threshold from the user.
            var finalPrice: Long = 0
            try {
                val SDK_INT = Build.VERSION.SDK_INT
                if (SDK_INT > 8) {
                    val policy = ThreadPolicy.Builder()
                        .permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                    val url =
                        URL("https://store.steampowered.com/api/appdetails/?appids=" + game.appId + "&filters=basic,price_overview&cc=au")
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val result = response.body?.string().orEmpty()
                    Log.i("SteamAPIRequestSuccess", result)
//                Log.i("SteamAPIRequestSuccess", url.toString())
                    if (result.isNotBlank()) {
                        val priceInfo = JSONObject(result)
                            .getJSONObject(game.appId.toString())
                            .getJSONObject("data")
                            .optJSONObject("price_overview")
                        finalPrice = if (priceInfo != null) priceInfo.optLong("final") else 0
                        Log.i(
                            "SteamAPIRequestSuccess",
                            "The current price is " + finalPrice.toString()
                        )
                    }
                }
            } catch (err: Error) {
                Log.i(
                    "SteamAPIRequestError",
                    "Error when executing get request: " + err.localizedMessage
                )
            }

            val threshold = (finalPrice * 0.9).toLong()
            val watchlistGame = WatchlistGame(
                game.appId,
                game.name,
                finalPrice,
                threshold,
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                true,
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