package com.steamwhistle

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A view model for managing the data and database connections of the watch list. The lifecycle of
 * a view model outlives that of an activity, because the activity is destroyed and recreated all
 * the time, for events such as screen rotations. This is an [AndroidViewModel] so that it can have
 * access to the [Application], which it needs so it can have a context to attach the database to,
 * because view models should never have references to any context other than the application, as
 * they will likely outlive them, causing memory leaks.
 */
class WatchlistViewModel(application: Application): AndroidViewModel(application) {
    private val database = SteamWhistleDatabase.getDatabase(application)
    private val dao = database.watchlistDao()

    val games: LiveData<List<WatchlistGame>> = dao.getWatchlistGames()

    /**
     * Save the given [watchlistGame]. Return true if the game is successfully inserted, and false
     * if the game already exists (or violates some other SQL constraint). Any other exceptional
     * cases should cause errors.
     */
    suspend fun saveGame(watchlistGame: WatchlistGame): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dao.insert(watchlistGame)
            } catch (error: SQLiteConstraintException) {
                return@withContext false
            }
            return@withContext true
        }
    }

    suspend fun deleteGame(watchlistGame: WatchlistGame): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteGame(watchlistGame)
            } catch (error: SQLiteConstraintException) {
                return@withContext false
            }
            return@withContext true
        }
    }


    suspend fun updateGame(watchlistGame: WatchlistGame): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dao.updateGame(watchlistGame)
            } catch (error: SQLiteConstraintException) {
                return@withContext false
            }

            return@withContext true
        }
    }
}