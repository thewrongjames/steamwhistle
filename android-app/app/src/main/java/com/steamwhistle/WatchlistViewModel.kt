package com.steamwhistle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A view model for managing the data and database connections of
 */
class WatchlistViewModel(application: Application): AndroidViewModel(application) {
    private val database = SteamWhistleDatabase.getDatabase(application)
    private val dao = database.watchlistDao()

    val games: LiveData<List<WatchlistGame>> = dao.getWatchlistGames()

    fun saveGame(watchlistGame: WatchlistGame) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(watchlistGame)
        }
    }
}