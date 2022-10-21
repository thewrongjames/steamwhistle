package com.steamwhistle

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchlistDao {
    companion object {
        private const val TAG = "WatchlistDao"
    }

    @Insert
    suspend fun insertGame(watchlistGame: WatchlistGame)

    @Query("SELECT * FROM watchlist_games WHERE is_active = 1 ORDER BY name")
    fun getWatchlistGames(): LiveData<List<WatchlistGame>>

    /**
     * Mark the game as removed from the watchlist. We keep the record to facilitate syncing the
     * deletion with firebase.
     */
    @Query("UPDATE watchlist_games SET is_active = 0 where app_id = :appId")
    suspend fun removeGame(appId: Int)

    @Query("SELECT NOT is_active FROM watchlist_games WHERE app_id = :appId")
    suspend fun gameHasBeenRemoved(appId: Int): Boolean

    /**
     * Actually delete the game completely from the database. In general this should not be used.
     */
    @Query("DELETE FROM watchlist_games WHERE app_id = :appId")
    suspend fun deleteGame(appId: Int)

    @Update
    suspend fun updateGame(watchlistGame: WatchlistGame)

    /**
     * Add the given [watchlistGame] to the watchlist. It may need to un-remove it if it has
     * already been put in the database. Returns true if the game has been added (it was not on the
     * watchlist) and false if the game has not been added (it was already on the watchlist).
     */
    @Transaction
    suspend fun addGame(watchlistGame: WatchlistGame): Boolean {
        Log.i(TAG, "Adding game $watchlistGame")
        try {
            insertGame(watchlistGame)
        } catch (error: SQLiteConstraintException) {
            updateGame(watchlistGame)
//            // The game already exists.
//            Log.i(TAG, "The game is already in the database.")
//            if (!gameHasBeenRemoved(watchlistGame.appId)) {
//                Log.i(TAG, "The game has not been marked as removed, it cannot be added again.")
//                // The game just already exists, and it has not been deleted, so we can't add it
//                // again.
//                return false
//            }
//
//            Log.i(TAG, "The game has been removed, un-removing it.")
//            // We just need to undelete the game. For some reason I couldn't get an update to work.
//            // We want the new details, including timestamps.
//            updateGame(watchlistGame)
//            Log.e(TAG, "HERE!")
////            insertGame(watchlistGame)
//            return true
        }

        return true
    }
}