package com.steamwhistle

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.time.ZonedDateTime

@Dao
interface WatchlistDao {
    companion object {
        private const val TAG = "WatchlistDao"
    }

    @Insert()
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
    @Query("UPDATE watchlist_games SET is_active = 1, updated = :updated WHERE app_id = :appId")
    suspend fun unRemoveGame(appId: Int, updated: ZonedDateTime)

    @Update
    suspend fun updateGame(watchlistGame: WatchlistGame)

    @Query("SELECT EXISTS (SELECT * FROM watchlist_games WHERE app_id = :appId)")
    suspend fun gameExists(appId: Int): Boolean

    /**
     * Add the given [watchlistGame] to the watchlist. It may need to un-remove it if it has
     * already been put in the database. Returns true if the game is added the the users watchlist.
     */
    @Transaction
    suspend fun addGame(newGame: WatchlistGame): Boolean {
        Log.i(TAG, "Adding game $newGame")

        if (!gameExists(newGame.appId)) {
            Log.i(TAG, "The game does not exist in the database, adding it.")
            insertGame(newGame)
            return true
        }

        if (!gameHasBeenRemoved(newGame.appId)) {
            Log.i(
                TAG,
                """
                The game is in the database and not 'removed', so it is already on the user's
                watchlist.
                """.trimIndent()
            )
            return false
        }

        unRemoveGame(newGame.appId, newGame.updated)
        return true
    }
}