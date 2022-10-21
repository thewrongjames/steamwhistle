package com.steamwhistle

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchlistDao {
    @Insert
    suspend fun insert(watchlistGame: WatchlistGame)

    @Query("SELECT * FROM watchlist_games ORDER BY name")
    fun getWatchlistGames(): LiveData<List<WatchlistGame>>

    @Delete
    suspend fun deleteGame(watchlistGame: WatchlistGame)

    @Update
    suspend fun updateGame(watchlistGame: WatchlistGame)
}