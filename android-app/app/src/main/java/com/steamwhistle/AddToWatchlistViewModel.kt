package com.steamwhistle

import android.util.Log
import androidx.lifecycle.ViewModel
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddToWatchlistViewModel: ViewModel() {
    companion object {
        private const val TAG = "AddToWatchlistViewModel"
    }

    private var client: ClientSearch = ClientSearch(
        applicationID = ApplicationID("T5UAP75D11"),
        apiKey = APIKey("d8c970fa01556a1a34b453fe6e5870ea")
    )
    private var index: Index = client.initIndex(IndexName("steamGames"))

    suspend fun searchGames(name: String):List<Game> {
        return withContext(Dispatchers.IO) {
            val response = index.search(Query(name))
            Log.i(TAG, response.toString())
            return@withContext response.hits.deserialize(SearchResultGame.serializer())
        }

    }
}