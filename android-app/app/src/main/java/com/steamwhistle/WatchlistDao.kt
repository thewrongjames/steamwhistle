package com.steamwhistle

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Dao
interface WatchlistDao {
    companion object {
        private const val TAG = "WatchlistDao"
    }

    @Insert()
    suspend fun insertGame(watchlistGame: WatchlistGame)

    @Query("SELECT * FROM watchlist_games WHERE is_active = 1 ORDER BY name")
    fun getActiveWatchlistGames(): LiveData<List<WatchlistGame>>

    @Query("SELECT app_id FROM watchlist_games WHERE is_active = 1")
    suspend fun getActiveWatchlistGameIds(): List<Long>

    @Query("SELECT NOT is_active FROM watchlist_games WHERE app_id = :appId")
    suspend fun gameHasBeenRemoved(appId: Long): Boolean

    /**
     * Actually delete the game completely from the database. In general this should not be used.
     */
    @Query("UPDATE watchlist_games SET is_active = 1, updated = :updated WHERE app_id = :appId")
    suspend fun unRemoveGame(appId: Long, updated: ZonedDateTime)

    @Update
    suspend fun updateGame(watchlistGame: WatchlistGame)

    @Query("SELECT EXISTS (SELECT * FROM watchlist_games WHERE app_id = :appId)")
    suspend fun gameExists(appId: Long): Boolean

    /**
     * Update the parts of a watchlist item that depend on the game.
     */
    @Query("UPDATE watchlist_games SET name = :name, price = :price WHERE app_id = :appId")
    suspend fun updateGameData(appId: Long, name: String, price: Long)

    suspend fun attemptToUpdateLocalGameFromNotificationData(
        appIdString: String?,
        name: String?,
        priceString: String?
    ) {
        Log.i(TAG, "Attempting to update game from notification: $appIdString, $name, $priceString")
        if (appIdString == null || name == null || priceString == null) {
            Log.e(TAG, "Null game data")
            return
        }

        val appId: Long
        val price: Long
        try {
            appId = appIdString.toLong()
            price = priceString.toLong()
        } catch (error: NumberFormatException) {
            Log.e(TAG, "Could not parse app ID or price to an integer")
            return
        }

        updateGameData(appId, name, price)
    }

    @Query("SELECT * FROM watchlist_games WHERE app_id = :appId")
    suspend fun getGameById(appId: Long): WatchlistGame?

    /**
     * Update the local watch list game corresponding to the given firebase watchlist item if the
     * firebase watchlist item is newer. Returns true if it created a new game locally, and false
     * otherwise. New games will have an empty price and name, and it will need to be updated
     * elsewhere.
     * TODO: Handle new game prices better than this.
     */
    @Transaction
    suspend fun updateFromFirebaseIfNewer(firebaseWatchlistItem: FirebaseWatchlistItem): Boolean {
        val appId = firebaseWatchlistItem.appId
        val watchlistGame = getGameById(appId)

        val firebaseUpdated = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(
                firebaseWatchlistItem.updatedSeconds,
                firebaseWatchlistItem.updatedNanos.toLong(),
            ),
            ZoneId.systemDefault(),
        )
        val firebaseCreated = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(
                firebaseWatchlistItem.createdSeconds,
                firebaseWatchlistItem.updatedNanos.toLong(),
            ),
            ZoneId.systemDefault(),
        )

        if (watchlistGame == null) {
            Log.i(TAG, "Creating new watchlist item for appId $appId from firebase")

            // We don't have the item, so we should add it from firebase.
            val newWatchlistGame = WatchlistGame(
                appId = appId,
                name = "",
                price = 0,
                threshold = firebaseWatchlistItem.threshold,
                updated = firebaseUpdated,
                created = firebaseCreated,
                isActive = firebaseWatchlistItem.isActive,
            )
            insertGame(newWatchlistGame)
            return true
        }

        if (firebaseUpdated > watchlistGame.updated) {
            Log.i(TAG, "Updating watchlist for appId $appId from firebase")
            watchlistGame.threshold = firebaseWatchlistItem.threshold
            watchlistGame.updated = firebaseUpdated
            watchlistGame.isActive = firebaseWatchlistItem.isActive
            updateGame(watchlistGame)
        } else {
            Log.i(TAG, "Firebase watchlist item for appId $appId is older than local")
        }

        return false
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