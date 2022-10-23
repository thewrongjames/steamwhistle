package com.steamwhistle

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

    @Query("UPDATE watchlist_games SET price = :price WHERE app_id = :appId")
    suspend fun updatePrice(appId: Int, price: Int)

    suspend fun attemptToUpdateLocalGameFromNotificationData(
        appIdString: String?,
        priceString: String?
    ) {
        Log.i(TAG, "Attempting to update game from notification: $appIdString, $priceString")
        if (appIdString == null || priceString == null) {
            Log.e(TAG, "Null app ID or price")
            return
        }

        val appId: Int
        val price: Int
        try {
            appId = appIdString.toInt()
            price = priceString.toInt()
        } catch (error: NumberFormatException) {
            Log.e(TAG, "Could not parse app ID or price to an integer")
            return
        }

        updatePrice(appId, price)
    }

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